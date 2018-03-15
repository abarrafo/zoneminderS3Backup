package com.andrewbarraford.s3backup.redis.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.andrewbarraford.s3backup.domain.Event;
import com.andrewbarraford.s3backup.domain.EventRepository;
import com.andrewbarraford.s3backup.redis.models.QueueMessage;
import com.andrewbarraford.s3backup.redis.queue.QueueFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@SuppressWarnings("unused")
@Slf4j
@Service
public class QueueProcessing {

    private final transient TransferManager transferManager;
    private final transient QueueFactory queueFactory;
    private final transient EventRepository eventRepository;

    @Autowired
    public QueueProcessing(final TransferManager transferManager, final QueueFactory queueFactory,
                           final EventRepository eventRepository) {
        this.transferManager = transferManager;
        this.queueFactory = queueFactory;
        this.eventRepository = eventRepository;
    }

    /** short poll the queue */
    @Scheduled(fixedDelay = 1000)
    public void processMessageQueue(){
        final QueueMessage message =  queueFactory.popFromMessageQueueOntoProcessing();
        if(message!=null){
            log.info("Message pulled : [{}]", message);
            pushToCloud(message);
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void checkProcessingQueue(){
        final QueueMessage queueMessage = queueFactory.popFromProcessing();
        if(queueMessage != null){
            final Optional<Event> event = eventRepository.findOneById(queueMessage.getId());
            if(event.isPresent()){
                if(event.get().isBackUpComplete()){
                    log.info("Upload for [{}] has completed.", event.get().getId());
                }
                else if(LocalDateTime.now().isAfter(LocalDateTime.parse(queueMessage.getCreateDate()).plusHours(1))) {
                    log.warn("Upload never completed in provided time. Put back on to the message queue to restart " +
                            "upload process for: [{}].", queueMessage.getId());
                    queueMessage.setAttempts(queueMessage.getAttempts() == null ? 1 : queueMessage.getAttempts() + 1);
                    if(queueMessage.getAttempts() <= 15) {
                        queueFactory.pushToMessageQueue(queueMessage);
                    }else{
                        log.warn("Upload attempted 15 times .. Dropping the vent from upload: [{}]", queueMessage);
                        event.get().setBackUpComplete(true);
                        eventRepository.save(event.get());
                    }
                }else{
                    log.info("Upload may still be processing for: [{}]", queueMessage.getId());
                    queueFactory.pushToProcessingQueue(queueMessage);
                }
            }
        }
    }

    /** process text analytics, persist message in db and push into webSocket */
    private void pushToCloud(final QueueMessage queueMessage){
        //Push to S3
        final File zip = new File(queueMessage.getPath());
        final Upload upload = transferManager.upload(queueMessage.getBucketName(), queueMessage.getKey(), zip);
        try {
            final Optional<Event> event = eventRepository.findOneById(queueMessage.getId());
            if(event.isPresent()){
                upload.waitForCompletion();

                final Event theEvent = event.get();
                theEvent.setBackUpComplete(true);
                eventRepository.save(theEvent);

                if(zip.delete()){
                    log.info("Temp zip: [{}] has been removed", zip.getName());
                }else {
                    log.warn("Temp zip: [{}] with path: [{}] has failed to be removed", zip.getName(), zip.getPath());
                }
            }
        } catch (AmazonClientException e) {
            log.error("Unable to upload file, upload was aborted.");
            log.error("Error occurred, e.message: [{}], e.cause: [{}], e.stack: [{}], e.class: [{}]",
                    e.getMessage(), e.getCause(), e.getStackTrace(), e.getClass());
        } catch (InterruptedException e) {
            log.error("Error occurred, e.message: [{}], e.cause: [{}], e.stack: [{}], e.class: [{}]",
                    e.getMessage(), e.getCause(), e.getStackTrace(), e.getClass());
        }
    }

}
