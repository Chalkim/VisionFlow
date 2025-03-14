package chalkim.visionflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Component
public class ResultStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final static Logger LOG = LoggerFactory.getLogger(ResultStreamListener.class);

    private final WebSocketSessionRegistry sessionRegistry;

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public ResultStreamListener(WebSocketSessionRegistry sessionRegistry, StringRedisTemplate stringRedisTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.redisTemplate = stringRedisTemplate;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Map<String, String> data = message.getValue();
        String sessionId = data.get("sessionId");
        String results = data.get("results");

        // LOG.info("Received sessionId: {}", sessionId);
        // LOG.info("Received results: {}", results);
        // LOG.info("-------------");

        redisTemplate.opsForStream().delete(message.getStream(), message.getId());

        try {
            WebSocketSession session = sessionRegistry.getSession(sessionId);

            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(results));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}
