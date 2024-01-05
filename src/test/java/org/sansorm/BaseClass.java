package org.sansorm;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

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
