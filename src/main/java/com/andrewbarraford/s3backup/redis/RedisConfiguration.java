package com.andrewbarraford.s3backup.redis;

import com.andrewbarraford.s3backup.redis.models.QueueMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {

    /** redis configuration */
    @Bean
    public RedisTemplate<String, QueueMessage> redisQueueTemplate(RedisConnectionFactory connectionFactory) {
        final RedisTemplate<String, QueueMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(QueueMessage.class));
        return template;
    }

}
