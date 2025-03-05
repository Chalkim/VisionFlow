package chalkim.visionflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        ByteBuffer buffer = message.getPayload();
        byte[] imageData = new byte[buffer.remaining()];
        buffer.get(imageData);

        String base64Image = Base64.getEncoder().encodeToString(imageData);

        int width = 0;
        int height = 0;
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
            if (bufferedImage != null) {
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            }
        } catch (IOException e) {
            LOG.warning("Failed to read image data: " + e.getMessage());
        }

        Map<String, String> data = new HashMap<>();
        data.put("session", session.getId());
        data.put("image", base64Image);
        data.put("width", String.valueOf(width));
        data.put("height", String.valueOf(height));

        redisTemplate.opsForStream().add("video_stream", data);

        LOG.info("Received image from session " + session.getId() + " with dimensions " + width + "x" + height);
    }
}
