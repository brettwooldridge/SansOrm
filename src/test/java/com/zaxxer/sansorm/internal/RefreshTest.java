package com.zaxxer.sansorm.internal;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosureElf;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;
import org.sansorm.testutils.*;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 21.04.18
 */
public class RefreshTest {

   @Test
   public void refresh() throws SQLException {

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default_case value";
      String idValue = "Id value";

      final String[] fetchedSql = new String[1];
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
                        return RefreshTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public ResultSet executeQuery() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return true;
                     }

                     @Override
                     public ResultSetMetaData getMetaData() {
                        return new DummyResultSetMetaData() {
                           @Override
                           public int getColumnCount() {
                              return 3;
                           }

                           @Override
                           public String getColumnName(int column) {
                              return   column == 1 ? "Delimited Field Name" :
                                       column == 2 ? "default_case" :
                                       column == 3 ? "id"
                                                   : null;
                           }
                        };
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return   columnIndex == 1 ? delimitedFieldValue :
                                 columnIndex == 2 ? defaultCaseValue :
                                 columnIndex == 3 ? idValue
                                                  : null;
                     }
                  };
               }
            };
         }
      };
      CaseSensitiveDatabasesClass obj = new CaseSensitiveDatabasesClass();
      obj.setId("xyz");
      CaseSensitiveDatabasesClass obj2 = OrmReader.refresh(con, obj);
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      // Just to remind that all fields on the object are set again.
      assertEquals(idValue, obj.getId());
      assertTrue(obj == obj2);
      assertEquals("SELECT Test_Class.Id,Test_Class.\"Delimited Field Name\",Test_Class.Default_Case FROM Test_Class Test_Class WHERE  Id=?", fetchedSql[0]);
   }

   @Test
   public void refreshNotFound() throws SQLException {

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default_case value";
      String idValue = "Id value";

      final String[] fetchedSql = new String[1];
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
                        return RefreshTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public ResultSet executeQuery() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return false;
                     }
                  };
               }
            };
         }
      };
      CaseSensitiveDatabasesClass obj = new CaseSensitiveDatabasesClass();
      obj.setId("xyz");
      CaseSensitiveDatabasesClass obj2 = OrmReader.refresh(con, obj);
      assertEquals(null, obj.getDelimitedFieldName());
      assertEquals(null, obj.getDefaultCase());
      assertEquals(null, obj2);
   }

   @Table
   public static class TestClass {
      @Id @GeneratedValue
      int Id;
      @Column
      String field1 = "value1";
      @Column
      String field2 = "value2";
   }

   @Test
   public void refreshObjectH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         SqlClosureElf.executeUpdate(
            " CREATE TABLE TestClass ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "field1 VARCHAR(128), "
               + "field2 VARCHAR(128) "
               + ")");

         TestClass obj = SqlClosureElf.insertObject(new TestClass());
         assertEquals(1, obj.Id);
         obj = SqlClosureElf.getObjectById(TestClass.class, obj.Id);
         assertNotNull(obj);
         assertEquals("value1", obj.field1);

         SqlClosureElf.executeUpdate("update TestClass set field1 = 'changed'");

         TestClass obj2 = OrmElf.refresh(con, obj);
         assertTrue(obj == obj2);
         assertEquals("changed", obj.field1);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE TestClass");
      }
   }

   @Table
   public static class TestClass2 {
      @Id
      String id1 = "id1";
      @Id
      String id2 = "id2";
      @Column
      String field;
   }

   @Test
   public void refreshObjectCompositePrimaryKeyH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE TestClass2 ("
               + "id1 VARCHAR(128) NOT NULL, "
               + "id2 VARCHAR(128) NOT NULL, "
               + "field VARCHAR(128), "
               + "PRIMARY KEY (id1, id2)"
               + ")");

         String id1 = "id1";
         String id2 = "id2";
         String field = "field";

//         Connection connection = ds.getConnection();
//         Statement stmnt = connection.createStatement();
//         int rowCount = stmnt.executeUpdate("insert into TestClass2 (id1, id2, field) values('a', 'b', 'c')");
//         connection.close();
//         assertEquals(1, rowCount);

         TestClass2 obj = OrmElf.insertObject(con, new TestClass2());
         assertEquals(id1, obj.id1);
         obj = SqlClosureElf.getObjectById(TestClass2.class, obj.id1, obj.id2);
         assertNotNull(obj);
         assertEquals(null, obj.field);

         SqlClosureElf.executeUpdate("update TestClass2 set field = 'changed' where id1 = " + id1 + " and id2 = " + id2);

         TestClass2 obj2 = OrmElf.refresh(con, obj);
         assertTrue(obj == obj2);
         assertEquals("changed", obj.field);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE TestClass2");
      }
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
