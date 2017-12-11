package com.andrewbarraford.s3backup.redis.models;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class QueueMessage implements Serializable{

    /** versioning */
    private static transient final long serialVersionUID = 1L;

    private int id;
    private String path;
    private String bucketName;
    private String key;

    @Setter(AccessLevel.NONE)
    private String createDate = LocalDateTime.now().toString();

}
