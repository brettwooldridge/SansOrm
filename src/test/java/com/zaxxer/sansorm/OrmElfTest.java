package com.zaxxer.sansorm;

import org.junit.Test;
import org.sansorm.testutils.DummyConnection;
import org.sansorm.testutils.DummyParameterMetaData;
import org.sansorm.testutils.DummyStatement;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 10.04.18
 */
public class OrmElfTest {

   @Test
   public void updateObjectExludeColumns() throws SQLException {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id = "xyz";
         @Column(name = "FIELD_1")
         String field1 = "field1";
         @Column(name = "FIELD_2")
         String field2 = "field2";
         @Column(name = "\"Field_3\"")
         String field3 = "field3";
         @Column
         String field4 = "field4";
      }
      final String[] fetchedSql = new String[1];
      Map<Integer, String> idxToValue = new HashMap<>();
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return OrmElfTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
                  idxToValue.put(parameterIndex, (String) x);
               }
            };
         }
      };
      TestClass obj = OrmElf.updateObject(con, new TestClass(), "field_1", "Field_3");
      assertEquals("UPDATE Test_Class SET FIELD_2=?,field4=? WHERE id=?", fetchedSql[0]);
      assertEquals("field2", idxToValue.get(1));
      assertEquals("field4", idxToValue.get(2));
      assertEquals("xyz", idxToValue.get(3));
   }

   // ######### Utility methods ######################################################

   private int getParameterCount(String s) {
      int count = 0;
      for (Byte b : s.getBytes()) {
         if ((int)b == '?') {
            count++;
         }
      }
      return count;
   }
}
