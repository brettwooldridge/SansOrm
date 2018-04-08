package com.zaxxer.sansorm.internal;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosureElf;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.sansorm.TestUtils;
import org.sansorm.testutils.*;

import javax.persistence.*;
import java.sql.*;

import static org.junit.Assert.*;

/**
 * See Issue #22: <a href="https://github.com/brettwooldridge/SansOrm/issues/22">Problem with upper case column names</a>
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.03.18
 */
public class CaseSensitiveDatabasesTest {

   @After
   public void tearDown()
   {
      SansOrm.deinitialize();
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void columnsNameElementNotInQuotes() {
      class TestClass {
         @Column(name = "COLUMN_NAME")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("columnName");
      assertEquals("column_name", colName);
   }

   @Test
   public void columnsNameElementInQuotes() {
      class TestClass {
         @Column(name = "\"COLUMN_NAME\"")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("columnName");
      assertEquals("\"COLUMN_NAME\"", colName);
   }

   @Test
   public void joinColumnsNameElementNotInQuotes() {
      class TestClass {
         @JoinColumn(name = "JOIN_COLUMN_NAME")
         TestClass joinColumnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("joinColumnName");
      assertEquals("join_column_name", colName);
   }

   @Test
   public void joinColumnsNameElementInQuotes() {
      class TestClass {
         @JoinColumn(name = "\"JOIN_COLUMN_NAME\"")
         TestClass joinColumnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("joinColumnName");
      assertEquals("\"JOIN_COLUMN_NAME\"", colName);
   }

   @Test
   public void tablesNameElementNotInQuotes() {
      @Table(name = "TableName")
      class TestClass { }
      Introspected introspected = new Introspected(TestClass.class);
      String tableName = introspected.getTableName();
      assertEquals("TableName", tableName);
   }

   @Test
   public void columnsTableNameElementNotInQuotes() {
      class TestClass {
         @Column(table = "TABLE_NAME")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String[] columnTableNames = introspected.getColumnTableNames();
      assertEquals("table_name", columnTableNames[0]);
   }

   @Test
   public void columnsTableNameElementInQuotes() {
      class TestClass {
         @Column(table = "\"TABLE_NAME\"")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String[] columnTableNames = introspected.getColumnTableNames();
      assertEquals("\"TABLE_NAME\"", columnTableNames[0]);
   }

   @Test
   public void getColumnsCsv() {
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      String cols = OrmReader.getColumnsCsv(TestClass.class);
      assertEquals("default_case,\"DELIMITED_FIELD_NAME\"", cols);
   }

   @Test
   public void getColumnsCsvExclude() {
      String cols = OrmBase.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "DELIMITED_FIELD_NAME");
      assertEquals("default_case,id", cols);
      cols = OrmBase.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "default_case");
      assertEquals("\"DELIMITED_FIELD_NAME\",id", cols);
   }

   @Test
   public void getColumnsCsvExcludeWithTableName() {
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"", table = "DEFAULT_TABLE_NAME")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE", table = "\"DELIMITED_TABLE_NAME\"")
         String defaultCase;
         @Column
         String excluded;
      }
      String cols = OrmBase.getColumnsCsvExclude(TestClass.class, "excluded");
      assertEquals("\"DELIMITED_TABLE_NAME\".default_case,default_table_name.\"DELIMITED_FIELD_NAME\"", cols);
   }

   @Test
   public void getColumnNameForProperty() {
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      assertEquals("\"DELIMITED_FIELD_NAME\"", introspected.getColumnNameForProperty("delimitedFieldName"));
      assertEquals("default_case", introspected.getColumnNameForProperty("defaultCase"));
   }

   @Test
   public void isInsertableColumn() {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
      Introspected introspected = new Introspected(TestClass.class);
      assertTrue(introspected.isInsertableColumn("default_case"));
      assertTrue(introspected.isInsertableColumn("DELIMITED_FIELD_NAME"));
   }

   @Test
   public void getInsertableColumns() {
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"default_case", "\"DELIMITED_FIELD_NAME\""}, cols);
   }

   @Test
   public void getInsertableColumns2() {
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id @Column
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"default_case", "\"DELIMITED_FIELD_NAME\"", "id"}, cols);
   }

   @Test
   public void getInsertableColumnsGeneratedId() {
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id @GeneratedValue @Column
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"default_case", "\"DELIMITED_FIELD_NAME\""}, cols);
   }

   /**
    * CLARIFY Behaves different from {@link OrmBase#getColumnsCsvExclude(Class, String...)} in that it does not qualify field names with table names. See {@link #getColumnsCsvExcludeWithTableName()}.
     */
   @Test
   public void getInsertableColumnsWithTableName() {
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"", table = "DEFAULT_TABLE_NAME")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE", table="\"DELIMITED_TABLE_NAME\"")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"default_case", "\"DELIMITED_FIELD_NAME\""}, cols);
   }

   @Test
   public void getUpdatableColumns() {
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id @Column
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"default_case", "\"DELIMITED_FIELD_NAME\"", "id"}, cols);
   }

   @Test
   public void getUpdatableColumnsGenratedId() {
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id @GeneratedValue @Column
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName;
         @Column(name = "DEFAULT_CASE")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"default_case", "\"DELIMITED_FIELD_NAME\""}, cols);
   }

   @Test
   public void isUpdatableColumn() {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
      Introspected introspected = new Introspected(TestClass.class);
      assertTrue(introspected.isUpdatableColumn("default_case"));
      assertTrue(introspected.isUpdatableColumn("DELIMITED_FIELD_NAME"));
   }

   @Test
   public void getIdColumnNames() {
      class TestClass {
         @Id @Column(name = "\"ID\"")
         String id;
         @Id
         String Id2;
         @Id @Column
         String Id3;
         @Id @Column(name = "Id4")
         String Id4;
         @Id @Column(name = "")
         String Id5;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String[] idColumnNames = introspected.getIdColumnNames();
      assertTrue(idColumnNames.length == 5);
      assertEquals("\"ID\"", idColumnNames[0]);
      assertEquals("Id2", idColumnNames[1]);
      assertEquals("Id3", idColumnNames[2]);
      assertEquals("id4", idColumnNames[3]);
      assertEquals("Id5", idColumnNames[4]);
   }

   @Test
   public void constistentIdSupport() {
      class TestClass {
         @Id
         String Id;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String[] idColumnNames = introspected.getIdColumnNames();
      assertEquals("Id", idColumnNames[0]);
   }

   @Test
   public void getColumnsSansIds() {
      class TestClass {
         @Column(name = "\"COL\"")
         String col;
         @Column
         String Col2;
         @Column(name = "Col3")
         String Col3;
         @Column(name = "")
         String Col4;
      }
      Introspected introspected = new Introspected(TestClass.class);

      String[] columnsSansIds = introspected.getColumnsSansIds();
      assertTrue(columnsSansIds.length == 4);
      assertEquals("\"COL\"", columnsSansIds[0]);
      assertEquals("Col2", columnsSansIds[1]); // differs from getIdColumnNames()
      assertEquals("col3", columnsSansIds[2]);
      assertEquals("Col4", columnsSansIds[3]);
   }

   @Test
   public void insertObject() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id
         String id;
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.insertObject(con, new TestClass());
      assertEquals("INSERT INTO TEST_CLASS(default_case,\"DELIMITED_FIELD_NAME\") VALUES (?,?)", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
   }

   @Test
   public void objectById() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default_case value";
      String idValue = "id value";
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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
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
                              return   column == 1 ? "DELIMITED_FIELD_NAME" :
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
      CaseSensitiveDatabasesClass obj = OrmReader.objectById(con, CaseSensitiveDatabasesClass.class, "xyz");
      assertEquals("SELECT TEST_CLASS.default_case,TEST_CLASS.\"DELIMITED_FIELD_NAME\",TEST_CLASS.id FROM TEST_CLASS TEST_CLASS WHERE  id=?", fetchedSql[0]);
      assertEquals(idValue, obj.getId());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
   }

   @Test
   public void updateObject() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.updateObject(con, new TestClass());
      assertEquals("UPDATE TEST_CLASS SET default_case=?,\"DELIMITED_FIELD_NAME\"=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
   }

   @Test
   public void deleteObject() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id
         String id = "xyz";
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      final String[] fetchedId = new String[1];
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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public int executeUpdate() {
                  return 1;
               }
               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
                  fetchedId[0] = (String) x;
               }

            };
         }
      };
      assertEquals(1, OrmWriter.deleteObject(con, new TestClass()));
      assertEquals("DELETE FROM TEST_CLASS WHERE id=?", fetchedSql[0]);
      assertEquals("xyz", fetchedId[0]);
   }

   @Test
   public void deleteObjectById() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Id
         String id = "xyz";
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      final String[] fetchedId = new String[1];
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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public int executeUpdate() {
                  return 1;
               }

               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
                  fetchedId[0] = (String) x;
               }
            };
         }
      };
      assertEquals(1, OrmWriter.deleteObjectById(con, TestClass.class, "xyz"));
      assertEquals("DELETE FROM TEST_CLASS WHERE id=?", fetchedSql[0]);
      assertEquals("xyz", fetchedId[0]);
   }

   @Test
   public void statementToObject() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "id value";

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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
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
                              return   column == 1 ? "DELIMITED_FIELD_NAME" :
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
      PreparedStatement pstmnt = con.prepareStatement("select * from TEST_CLASS where id = ?");
      CaseSensitiveDatabasesClass obj = OrmElf.statementToObject(pstmnt, CaseSensitiveDatabasesClass.class, "xyz");
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      assertEquals(idValue, obj.getId());
   }

   @Test
   public void deleteObjectNoIdProvided() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "TEST_CLASS")
      class TestClass {
         @Column(name = "\"DELIMITED_FIELD_NAME\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "DEFAULT_CASE")
         String defaultCase = defaultCaseValue;
      }
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
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
            };
         }
      };
      thrown.expectMessage("No id columns provided");
      OrmWriter.deleteObject(con, new TestClass());
   }

   @Table(name = "\"test_class\"")
   public static class InsertObjectH2 {
      @Id @GeneratedValue
      int id;
      @Column(name = "\"delimited_field_name\"")
      String delimitedFieldName = "delimited field value";
      @Column(name = "default_case")
      String defaultCase = "default case value";
   }

   @Test
   public void insertObjectH2() {

      SansOrm.initializeTxNone(TestUtils.makeH2DataSource());
      SqlClosureElf.executeUpdate("CREATE TABLE \"test_class\" ("
         + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
         + "\"delimited_field_name\" VARCHAR(128), "
         + "DEFAULT_CASE VARCHAR(128) "
         + ")");

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      InsertObjectH2 obj = SqlClosureElf.insertObject(new InsertObjectH2());
      assertEquals(1, obj.id);
      obj = SqlClosureElf.getObjectById(InsertObjectH2.class, obj.id);
      assertNotNull(obj);
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
