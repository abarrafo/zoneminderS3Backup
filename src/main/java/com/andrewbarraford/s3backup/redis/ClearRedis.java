package com.andrewbarraford.s3backup.redis;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Profile("localhost")
@Component
public class ClearRedis {

    @Resource(name = "redisQueueTemplate")
    private transient RedisTemplate template;

    @PostConstruct
    public void init(){
        template.execute((RedisCallback<Void>) connection -> {
            connection.flushDb();
            return null;
        });
    }

}
