package org.sansorm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

public class TargetClass1
{
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "timestamp")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "string")
    private String string;
}
