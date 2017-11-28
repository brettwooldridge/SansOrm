package org.sansorm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
