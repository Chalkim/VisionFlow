package chalkim.visionflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.Element.PAD_ADDED;
import org.freedesktop.gstreamer.elements.DecodeBin;
import org.freedesktop.gstreamer.webrtc.WebRTCBin;
import org.freedesktop.gstreamer.webrtc.WebRTCBin.CREATE_OFFER;
import org.freedesktop.gstreamer.webrtc.WebRTCBin.ON_ICE_CANDIDATE;
import org.freedesktop.gstreamer.webrtc.WebRTCBin.ON_NEGOTIATION_NEEDED;
import org.freedesktop.gstreamer.webrtc.WebRTCSDPType;
import org.freedesktop.gstreamer.webrtc.WebRTCSessionDescription;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class GStreamerPipeline {

    private static final Logger LOG = Logger.getLogger(GStreamerPipeline.class.getName());


    private static final String PIPELINE_DESCRIPTION
            = "webrtcbin name=webrtcbin bundle-policy=max-bundle stun-server=stun://stun.l.google.com:19302";

    private final String serverUrl;
    private final String sessionId;
    private final ObjectMapper mapper = new ObjectMapper();

    private WebRTCBin webRTCBin;
    private Pipeline pipe;

    private WebSocketSession webSocketSession;

    public GStreamerPipeline(WebSocketSession webSocketSession, String sessionId, String serverUrl) {
        this.webSocketSession = webSocketSession;
        this.sessionId = sessionId;
        this.serverUrl = serverUrl;

        Gst.init(Version.of(1, 16));

        pipe = (Pipeline) Gst.parseLaunch(PIPELINE_DESCRIPTION);
        webRTCBin = (WebRTCBin) pipe.getElementByName("webrtcbin");

        setupPipeLogging(pipe);

        // When the pipeline goes to PLAYING, the on_negotiation_needed() callback
        // will be called, and we will ask webrtcbin to create an offer which will
        // match the pipeline above.
        webRTCBin.connect(onNegotiationNeeded);
        webRTCBin.connect(onIceCandidate);
        webRTCBin.connect(onIncomingStream);

        Gst.main();
    }

    protected void handleSdp(String payload) {
        try {
            JsonNode answer = mapper.readTree(payload);
            if (answer.has("sdp")) {
                String sdpStr = answer.get("sdp").get("sdp").textValue();
                LOG.info(() -> "Answer SDP:\n" + sdpStr);
                SDPMessage sdpMessage = new SDPMessage();
                sdpMessage.parseBuffer(sdpStr);
                WebRTCSessionDescription description = new WebRTCSessionDescription(WebRTCSDPType.ANSWER, sdpMessage);
                webRTCBin.setRemoteDescription(description);
            } else if (answer.has("ice")) {
                String candidate = answer.get("ice").get("candidate").textValue();
                int sdpMLineIndex = answer.get("ice").get("sdpMLineIndex").intValue();
                LOG.info(() -> "Adding ICE candidate : " + candidate);
                webRTCBin.addIceCandidate(sdpMLineIndex, candidate);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Problem reading payload", e);
        }
    }

    private void setupPipeLogging(Pipeline pipe) {
        Bus bus = pipe.getBus();
        bus.connect((Bus.EOS) source -> {
            LOG.info(() -> "Reached end of stream : " + source.toString());
            endCall();
        });

        bus.connect((Bus.ERROR) (source, code, message) -> {
            LOG.severe(() -> "Error from source : " + source
                    + ", with code : " + code + ", and message : " + message);
            endCall();
        });

        bus.connect((source, old, current, pending) -> {
            if (source instanceof Pipeline) {
                LOG.info(() -> "Pipe state changed from " + old + " to " + current);
            }
        });
    }

    protected void endCall() {
        pipe.setState(State.NULL);
        Gst.quit();
    }

    private CREATE_OFFER onOfferCreated = offer -> {
        webRTCBin.setLocalDescription(offer);
        try {
            ObjectNode rootNode = mapper.createObjectNode();
            ObjectNode sdpNode = mapper.createObjectNode();
            sdpNode.put("type", "offer");
            sdpNode.put("sdp", offer.getSDPMessage().toString());
            rootNode.set("sdp", sdpNode);
            String json = mapper.writeValueAsString(rootNode);
            LOG.info(() -> "Sending offer:\n" + json);
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(json));
            }
        } catch (JsonProcessingException e) {
            LOG.log(Level.SEVERE, "Couldn't write JSON", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Couldn't send message", e);
        }
    };

    private final ON_NEGOTIATION_NEEDED onNegotiationNeeded = elem -> {
        LOG.info(() -> "onNegotiationNeeded: " + elem.getName());

        // When webrtcbin has created the offer, it will hit our callback and we
        // send SDP offer over the websocket to signalling server
        webRTCBin.createOffer(onOfferCreated);
    };

    private final ON_ICE_CANDIDATE onIceCandidate = (sdpMLineIndex, candidate) -> {
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode iceNode = mapper.createObjectNode();
        iceNode.put("candidate", candidate);
        iceNode.put("sdpMLineIndex", sdpMLineIndex);
        rootNode.set("ice", iceNode);

        try {
            String json = mapper.writeValueAsString(rootNode);
            LOG.info(() -> "ON_ICE_CANDIDATE: " + json);
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(json));
            }
        } catch (JsonProcessingException e) {
            LOG.log(Level.SEVERE, "Couldn't write JSON", e);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Couldn't send message", e);
        }
    };

    private final PAD_ADDED onDecodedStream = (element, pad) -> {
        if (!pad.hasCurrentCaps()) {
            LOG.info("Pad has no current Caps - ignoring");
            return;
        }
        Caps caps = pad.getCurrentCaps();
        LOG.info(() -> "Received decoded stream with caps : " + caps.toString());
        if (caps.isAlwaysCompatible(Caps.fromString("video/x-raw"))) {
            Element q = ElementFactory.make("queue", "videoqueue");
            Element conv = ElementFactory.make("videoconvert", "videoconvert");
            Element sink = ElementFactory.make("autovideosink", "videosink");
            pipe.addMany(q, conv, sink);
            q.syncStateWithParent();
            conv.syncStateWithParent();
            sink.syncStateWithParent();
            pad.link(q.getStaticPad("sink"));
            q.link(conv);
            conv.link(sink);
        } else if (caps.isAlwaysCompatible(Caps.fromString("audio/x-raw"))) {
            Element q = ElementFactory.make("queue", "audioqueue");
            Element conv = ElementFactory.make("audioconvert", "audioconvert");
            Element resample = ElementFactory.make("audioresample", "audioresample");
            Element sink = ElementFactory.make("autoaudiosink", "audiosink");
            pipe.addMany(q, conv, resample, sink);
            q.syncStateWithParent();
            conv.syncStateWithParent();
            resample.syncStateWithParent();
            sink.syncStateWithParent();
            pad.link(q.getStaticPad("sink"));
            q.link(conv);
            conv.link(resample);
            resample.link(sink);
        }
    };

    private final PAD_ADDED onIncomingStream = (element, pad) -> {
        LOG.info(()
                -> "Receiving stream! Element : " + element.getName()
                + " Pad : " + pad.getName());
        if (pad.getDirection() != PadDirection.SRC) {
            return;
        }
        DecodeBin decodeBin = new DecodeBin("decodebin_" + pad.getName());
        decodeBin.connect(onDecodedStream);
        pipe.add(decodeBin);
        decodeBin.syncStateWithParent();
        pad.link(decodeBin.getStaticPad("sink"));
    };

}