package chalkim.visionflow.controller;

import chalkim.visionflow.service.WebRTCWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebSocketController {

    private final WebRTCWebSocketHandler webRTCWebSocketHandler;

    @Autowired
    public WebSocketController(WebRTCWebSocketHandler webRTCWebSocketHandler) {
        this.webRTCWebSocketHandler = webRTCWebSocketHandler;
    }
}
