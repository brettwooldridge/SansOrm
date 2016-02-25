package org.sansorm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Table(name = "target_class1")
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

   public TargetClass1()
   {

   }

   public TargetClass1(Date timestamp, String string)
   {
      this.timestamp = timestamp;
      this.string = string;
   }

   public int getId()
   {
      return id;
   }

   public Date getTimestamp()
   {
      return timestamp;
   }

   public String getString()
   {
      return string;
   }
}
