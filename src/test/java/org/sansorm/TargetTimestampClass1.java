package org.sansorm;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Created by lbayer on 2/25/16.
 */
@Table(name = "target_class1")
public class TargetTimestampClass1
{
    @Id
    @GeneratedValue
    @Column(name = "ID")
    private int id;

    @Column(name = "timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Timestamp timestamp;

    @Column(name = "string")
    private String string;

    public TargetTimestampClass1()
    {
    }

    public TargetTimestampClass1(Timestamp timestamp, String string)
    {
        this.timestamp = timestamp;
        this.string = string;
    }

    public int getId()
    {
        return id;
    }

    public Timestamp getTimestamp()
    {
        return timestamp;
    }

    public String getString()
    {
        return string;
    }
}
