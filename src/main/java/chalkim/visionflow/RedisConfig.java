package chalkim.visionflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamListener;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private final WebSocketSessionRegistry sessionRegistry;

    @Autowired
    public RedisConfig(WebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> subscription(RedisConnectionFactory redisConnectionFactory, StringRedisTemplate redisTemplate) {
        // Listener container configuration
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainer
                .StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(Duration.ofMillis(100))
                .build();

        ResultStreamListener resultStreamListener = new ResultStreamListener(sessionRegistry, redisTemplate);

        // Create listener container
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        listenerContainer.receive(StreamOffset.fromStart("pending_results"), resultStreamListener);

        listenerContainer.start();
        System.out.println("------------------------------------------stream listener started-----------------------------------------------------------");

        return listenerContainer;
    }
}
