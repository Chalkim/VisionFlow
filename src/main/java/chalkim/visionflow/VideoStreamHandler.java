package chalkim.visionflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/camera")
public class VideoStreamHandler extends BinaryWebSocketHandler {

    private static final Logger LOG = Logger.getLogger(VideoStreamHandler.class.getName());

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WebSocketSessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        sessionRegistry.registerSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        sessionRegistry.removeSession(session.getId());
    }

    public static byte[] convertJpegToBmp(byte[] imageData) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);
        BufferedImage image = ImageIO.read(byteArrayInputStream);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer buffer = message.getPayload();
        byte[] imageData = new byte[buffer.remaining()];
        buffer.get(imageData);

        try {
            // byte[] bmpData = convertJpegToBmp(imageData);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData);

            int width = 0;
            int height = 0;
            BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
            if (bufferedImage != null) {
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            }
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            Map<String, String> data = new HashMap<>();
            data.put("sessionId", session.getId());
            data.put("imageData", base64Image);
            data.put("inputW", String.valueOf(width));
            data.put("inputH", String.valueOf(height));

            // delete old data in pending_data if amount of data is more than 10
            Long size = redisTemplate.opsForStream().size("pending_data");
            if (size != null && size > 5) {
                LOG.warning("Inferencing is too slow, deleting old data");
                redisTemplate.opsForStream().trim("pending_data", 5, true);
            }

            redisTemplate.opsForStream().add("pending_data", data);
        } catch (IOException e) {
            LOG.warning("Failed to read image data: " + e.getMessage());
        }

        // Map<String, String> result = new HashMap<>();
        // result.put("sessionId", session.getId());
        // result.put("results", "Image received with dimensions " + width + "x" + height);

        // redisTemplate.opsForStream().add("pending_results", result);

        // LOG.info("Received image from session " + session.getId() + " with dimensions " + width + "x" + height);
    }
}
