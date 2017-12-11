package com.andrewbarraford.s3backup.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "Events")
public class Event {

    /** serialization uid */
    private static final long serialVersionUID = 42L;

    /** An Identity id, allowing the db to auto-generate the id on insert. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", length = 10)
    @Setter(AccessLevel.NONE)
    private int id;

    @Column(name = "MonitorId", length=10)
    private int monitorId;

    @Column(name = "Name", length = 64)
    private String name;

    @Column(name = "Cause", length = 32)
    private String cause;

    @Column(name = "StartTime", columnDefinition="DATETIME", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startTime;

    @Column(name = "EndTime", columnDefinition="DATETIME", insertable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @Column(name = "BackedUp", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean backedUp;

    @Column(name = "BackedUpComplete", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean backUpComplete;
}
