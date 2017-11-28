package org.sansorm;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class BaseClass
{
   @Id
   @GeneratedValue
   @Column(name = "id")
   private int id;

   @Column(name = "string")
   protected String string;

   public int getId()
   {
      return id;
   }

   public String getString()
   {
      return string;
   }

   public void setString(String string) {
      this.string = string;
   }
}
