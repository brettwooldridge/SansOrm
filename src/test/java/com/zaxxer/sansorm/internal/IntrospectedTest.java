package com.zaxxer.sansorm.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sansorm.TargetClass1;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

public class IntrospectedTest
{
   @Test
   public void shouldHandleCommonJPAAnnotations()
   {
      Introspected inspected = new Introspected(TargetClass1.class);
      assertThat(inspected).isNotNull();
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string", "string_from_number", "timestamp"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"timestamp", "string_from_number", "id", "string"});
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
      assertThat(inspected).isNotNull();
      assertThat(inspected.getTableName()).isEqualTo("SomeEntity").as("According to Table::name javadoc, empty name should default to entity name");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("According to Column::name javadoc, empty name should default to field name");
      assertThat(inspected.getColumnNameForProperty("someString")).isEqualTo("someString");
      assertThat(inspected.getColumnNameForProperty("someOtherString")).isEqualTo("SOME_OTHER_STRING").as("Explicit Column names are converted to lower case");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
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
      assertThat(inspected).isNotNull();
      assertThat(inspected.getTableName()).isEqualTo("SomeEntity");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("Field declarations from MappedSuperclass should be available");
      assertThat(inspected.getColumnNameForProperty("string")).isEqualTo("string");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"string", "id"});
   }

   @Test
   public void inheritanceChain()
   {
      @MappedSuperclass
      class BaseEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;
      }

      class SomeEntity extends BaseEntity
      {
         @Column
         private String string; // Not reachable from SomeEntitySub
      }

      @Table
      class SomeEntitySub extends SomeEntity
      {
         @Column
         private String string2;
      }

      Introspected inspected = new Introspected(SomeEntitySub.class);
      assertThat(inspected).isNotNull();
      assertThat(inspected.getTableName()).isEqualTo("SomeEntitySub");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("Field declarations from MappedSuperclass should be available");
      assertThat(inspected.getColumnNameForProperty("string")).isNull();
      assertThat(inspected.getColumnNameForProperty("string2")).isEqualTo("string2");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"string2", "id"});
   }

   @Test
   public void twoMappedSuperclassesInherited()
   {
      @MappedSuperclass
      class BaseEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;
      }

      @MappedSuperclass
      class SomeEntity extends BaseEntity
      {
         @Column
         private String string;
      }

      @Table
      class SomeEntitySub extends SomeEntity
      {
         @Column
         private String string2;
      }

      Introspected inspected = new Introspected(SomeEntitySub.class);
      assertThat(inspected).isNotNull();
      assertThat(inspected.getTableName()).isEqualTo("SomeEntitySub");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("Field declarations from MappedSuperclass should be available");
      assertThat(inspected.getColumnNameForProperty("string")).isEqualTo("string");
      assertThat(inspected.getColumnNameForProperty("string2")).isEqualTo("string2");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"string2", "string", "id"});
   }

}
