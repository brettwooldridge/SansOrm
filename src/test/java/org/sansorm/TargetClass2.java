package org.sansorm;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity // Entity annotation
@Table // no explicit table name
public class TargetClass2 extends BaseClass
{
   @Column // no explicit column name
   @Temporal(TemporalType.TIMESTAMP)
   private Date someDate; // camelCase

   public TargetClass2()
   {
   }

   public TargetClass2(Date someDate, String string)
   {
      this.someDate = someDate;
      this.string = string;
   }

   public Date getSomeDate()
   {
      return someDate;
   }
}
