package org.sansorm;

import java.util.Date;
import java.util.Map;

import javax.persistence.*;

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

   @Column(name = "string_from_number")
   @Convert(converter = TestConverter.class)
   private String stringFromNumber;

   public TargetClass1()
   {

   }

   public TargetClass1(Date timestamp, String string)
   {
      this(timestamp, string, null);
   }

   public TargetClass1(Date timestamp, String string, String stringFromNumber)
   {
      this.timestamp = timestamp;
      this.string = string;
      this.stringFromNumber = stringFromNumber;
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

   public String getStringFromNumber() {
      return stringFromNumber;
   }

}
