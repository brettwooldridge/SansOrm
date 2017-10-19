package org.sansorm;

import org.junit.Assert;
import org.junit.Test;

import javax.persistence.*;

import com.zaxxer.sansorm.internal.Introspected;
import com.zaxxer.sansorm.internal.Introspector;

public class IntrospectorTest
{
   @Test
   public void test()
   {
      Introspected is1 = Introspector.getIntrospected(TargetClass1.class);
      Assert.assertNotNull(is1);
   }

   @Test
   public void shouldHandleEmptyAnnotationNames()
   {
      @Table
      class SomeEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;

         @Column
         private String someString;

         @Column(name = "SOME_OTHER_STRING") // just to demonstrate mixed case
         private String someOtherString;
      }

      Introspected inspected = Introspector.getIntrospected(SomeEntity.class);
      Assert.assertNotNull(inspected);
      Assert.assertEquals("According to Table::name javadoc, empty name should default to entity name",
         "SomeEntity", inspected.getTableName());
      Assert.assertEquals("According to Column::name javadoc, empty name should default to field name",
         "id", inspected.getColumnNameForProperty("id"));
      Assert.assertEquals("someString", inspected.getColumnNameForProperty("someString"));
      Assert.assertEquals("Explicit Column names are converted to lower case",
         "some_other_string", inspected.getColumnNameForProperty("someOtherString"));
   }
}
