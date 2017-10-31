package org.sansorm;

import javax.persistence.*;

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
