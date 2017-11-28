package org.sansorm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Table(name = "target_class1")
public class TargetClass1 extends BaseClass
{
   @Column(name = "timestamp")
   @Temporal(TemporalType.TIMESTAMP)
   private Date timestamp;

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

   public Date getTimestamp()
   {
      return timestamp;
   }

   public String getStringFromNumber() {
      return stringFromNumber;
   }

}
