package com.zaxxer.sansorm.internal;

import org.junit.Test;
import org.sansorm.TargetClass1;

import javax.persistence.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class IntrospectedTest
{
   @Test
   public void shouldHandleCommonJPAAnnotations()
   {
      Introspected inspected = new Introspected(TargetClass1.class);
      assertNotNull(inspected);
      assertTrue(inspected.hasGeneratedId());
      assertArrayEquals(new String[]{"id"}, inspected.getIdColumnNames());
      assertArrayEquals(new String[]{"id", "string", "string_from_number", "timestamp"}, inspected.getColumnNames());
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

      Introspected inspected = new Introspected(SomeEntity.class);
      assertNotNull(inspected);
      assertEquals("According to Table::name javadoc, empty name should default to entity name",
         "SomeEntity", inspected.getTableName());
      assertEquals("According to Column::name javadoc, empty name should default to field name",
         "id", inspected.getColumnNameForProperty("id"));
      assertEquals("someString", inspected.getColumnNameForProperty("someString"));
      assertEquals("Explicit Column names are converted to lower case",
         "some_other_string", inspected.getColumnNameForProperty("someOtherString"));
      assertTrue(inspected.hasGeneratedId());
      assertArrayEquals(new String[]{"id"}, inspected.getIdColumnNames());
   }

   @Test
   public void shouldHandleMappedSuperclass()
   {
      @MappedSuperclass
      class BaseEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;
      }

      @Table
      class SomeEntity extends BaseEntity
      {
         @Column
         private String string;
      }

      Introspected inspected = new Introspected(SomeEntity.class);
      assertNotNull(inspected);
      assertEquals("SomeEntity", inspected.getTableName());
      assertEquals("Field declarations from MappedSuperclass should be available",
         "id", inspected.getColumnNameForProperty("id"));
      assertEquals("string", inspected.getColumnNameForProperty("string"));
      assertTrue(inspected.hasGeneratedId());
      assertArrayEquals(new String[]{"id"}, inspected.getIdColumnNames());
      assertArrayEquals(new String[]{"id", "string"}, inspected.getColumnNames());
   }
}
