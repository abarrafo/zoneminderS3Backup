package com.andrewbarraford.s3backup.redis.queue;

import com.andrewbarraford.s3backup.redis.models.QueueMessage;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueueFactory {

    @Getter
    private static final transient String MESSAGE_QUEUE = "MESSAGE_QUEUE";
    @Getter
    private static final transient String PROCESSING_QUEUE = "PROCESSING_QUEUE";

    /** @see RedisTemplate */
    private final transient RedisTemplate<String, QueueMessage> redisTemplate;

    @Autowired
    public QueueFactory(
            final RedisTemplate<String, QueueMessage> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** add to message queue */
    public void pushToMessageQueue(final QueueMessage QueueMessage){
        redisTemplate.opsForList().leftPush(MESSAGE_QUEUE, QueueMessage);
    }

    public QueueMessage popFromMessageQueueOntoProcessing(){
        return redisTemplate.opsForList().rightPopAndLeftPush(MESSAGE_QUEUE, PROCESSING_QUEUE);
    }

    public void pushToProcessingQueue(final QueueMessage queueMessage){
        redisTemplate.opsForList().leftPush(PROCESSING_QUEUE, queueMessage);
    }

    public QueueMessage popFromProcessing(){
        return redisTemplate.opsForList().rightPop(PROCESSING_QUEUE);
    }

}
