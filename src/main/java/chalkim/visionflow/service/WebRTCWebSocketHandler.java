package chalkim.visionflow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.logging.Logger;

@Component
public class WebRTCWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOG = Logger.getLogger(WebRTCWebSocketHandler.class.getName());

    @Value("${visionflow.webrtc.url}")
    private static String WEBRTC_URL;

    private GStreamerPipeline gstPipeline;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        LOG.info("WebSocket connection established with session ID: " + session.getId());
        session.sendMessage(new TextMessage("HELLO 852978"));
        gstPipeline = new GStreamerPipeline(session, "852978", WEBRTC_URL);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        LOG.info("Received message: " + payload);

        if (payload.equals("HELLO")) {
            session.sendMessage(new TextMessage("SESSION 852978"));
        } else if (payload.equals("SESSION_OK")) {
            LOG.info("Received SESSION_OK, starting pipeline...");
        } else if (payload.startsWith("ERROR")) {
            LOG.severe("Error message received: " + payload);
            endCall(session);
        } else {
            LOG.info("Handling SDP: " + payload);
            gstPipeline.handleSdp(payload);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        LOG.severe("WebSocket error: " + exception.getMessage());
        endCall(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        LOG.info("WebSocket connection closed with session ID: " + session.getId());
        endCall(session);
    }

    private void endCall(WebSocketSession session) {
        try {
            session.close(CloseStatus.NORMAL);
            gstPipeline.endCall();
            System.out.println("WebSocket connection closed successfully");
            LOG.info("WebSocket connection closed successfully");
        } catch (Exception e) {
            LOG.severe("Error closing WebSocket session: " + e.getMessage());
        }
    }
}
