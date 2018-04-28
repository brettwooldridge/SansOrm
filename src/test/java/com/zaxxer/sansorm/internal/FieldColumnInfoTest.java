package com.zaxxer.sansorm.internal;

import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Table;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 17.04.18
 */
public class FieldColumnInfoTest {

   @Test
   public void getFullyQualifiedTableNameFromColumnAnnotation() {
      @Table
      class TestClass {
         @Column(table = "TEST_CLASS")
         String field;
      }
      Introspected introspected = new Introspected(TestClass.class);
      FieldColumnInfo[] fcInfos = introspected.getSelectableFcInfos();
      String fqn = fcInfos[0].getFullyQualifiedDelimitedFieldName();
      assertEquals("TEST_CLASS.field", fqn);
   }

//   @Test
//   public void getFullyQualifiedTableNameFromClassName() {
//      @Table
//      class TestClass {
//         @Column
//         String field;
//      }
//      Introspected introspected = new Introspected(TestClass.class);
//      FieldColumnInfo[] fcInfos = introspected.getSelectableFcInfos();
//      String fqn = fcInfos[0].getFullyQualifiedDelimitedFieldName();
//      assertEquals("TestClass.field", fqn);
//   }

//   @Test
//   public void getFullyQualifiedTableNameFromTableAnnotation() {
//      @Table(name = "TEST_CLASS")
//      class TestClass {
//         @Column
//         String field;
//      }
//      Introspected introspected = new Introspected(TestClass.class);
//      FieldColumnInfo[] fcInfos = introspected.getSelectableFcInfos();
//      String fqn = fcInfos[0].getFullyQualifiedDelimitedFieldName();
//      assertEquals("TEST_CLASS.field", fqn);
//   }

}
