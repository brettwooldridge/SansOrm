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
         @Column(name = "Column_Name")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("columnName");
      assertEquals("Column_Name", colName);
   }

   @Test
   public void columnsNameElementInQuotes() {
      class TestClass {
         @Column(name = "\"Column Name\"")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("columnName");
      assertEquals("\"Column Name\"", colName);
   }

   @Test
   public void joinColumnsNameElementNotInQuotes() {
      class TestClass {
         @JoinColumn(name = "Join_Column_Name")
         TestClass joinColumnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("joinColumnName");
      assertEquals("Join_Column_Name", colName);
   }

   @Test
   public void joinColumnsNameElementInQuotes() {
      class TestClass {
         @JoinColumn(name = "\"Join Column Name\"")
         TestClass joinColumnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String colName = introspected.getColumnNameForProperty("joinColumnName");
      assertEquals("\"Join Column Name\"", colName);
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
         @Column(table = "Table_Name")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String[] columnTableNames = introspected.getColumnTableNames();
      assertEquals("Table_Name", columnTableNames[0]);
   }

   @Test
   public void columnsTableNameElementInQuotes() {
      class TestClass {
         @Column(table = "\"Table Name\"")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      String[] columnTableNames = introspected.getColumnTableNames();
      assertEquals("\"Table Name\"", columnTableNames[0]);
   }

   @Test
   public void getColumnsCsv() {
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      String cols = OrmReader.getColumnsCsv(TestClass.class);
      assertEquals("Default_Case,\"Delimited Field Name\"", cols);
   }

   @Test
   public void getColumnsCsvExclude() {
      String cols = OrmBase.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "Delimited Field Name");
      assertEquals("Default_Case,Id", cols);
      cols = OrmBase.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "Default_Case");
      assertEquals("\"Delimited Field Name\",Id", cols);
   }

   @Test
   public void getColumnsCsvExcludeWithTableName() {
      class TestClass {
         @Column(name = "\"Delimited Field Name\"", table = "Default_Table_Name")
         String delimitedFieldName;
         @Column(name = "Default_Case", table = "\"DELIMITED_TABLE_NAME\"")
         String defaultCase;
         @Column
         String excluded;
      }
      String cols = OrmBase.getColumnsCsvExclude(TestClass.class, "excluded");
      assertEquals("\"DELIMITED_TABLE_NAME\".Default_Case,Default_Table_Name.\"Delimited Field Name\"", cols);
   }

   @Test
   public void getColumnNameForProperty() {
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      assertEquals("\"Delimited Field Name\"", introspected.getColumnNameForProperty("delimitedFieldName"));
      assertEquals("Default_Case", introspected.getColumnNameForProperty("defaultCase"));
   }

   @Test
   public void isInsertableColumn() {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      Introspected introspected = new Introspected(TestClass.class);
      assertTrue(introspected.isInsertableColumn("Default_Case"));
      assertTrue(introspected.isInsertableColumn("Delimited Field Name"));
   }

   @Test
   public void getInsertableColumns() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"Default_Case", "\"Delimited Field Name\""}, cols);
   }

   @Test
   public void getInsertableColumns2() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"Default_Case", "\"Delimited Field Name\"", "Id"}, cols);
   }

   @Test
   public void getInsertableColumnsGeneratedId() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"Default_Case", "\"Delimited Field Name\""}, cols);
   }

   /**
    * CLARIFY Behaves different from {@link OrmBase#getColumnsCsvExclude(Class, String...)} in that it does not qualify field names with table names. See {@link #getColumnsCsvExcludeWithTableName()}.
     */
   @Test
   public void getInsertableColumnsWithTableName() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id;
         @Column(name = "\"Delimited Field Name\"", table = "Default_Table_Name")
         String delimitedFieldName;
         @Column(name = "Default_Case", table="\"Delimited Table Name\"")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"Default_Case", "\"Delimited Field Name\""}, cols);
   }

   @Test
   public void getUpdatableColumns() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"Default_Case", "\"Delimited Field Name\"", "Id"}, cols);
   }

   @Test
   public void getUpdatableColumnsGeneratedId() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"Default_Case", "\"Delimited Field Name\""}, cols);
   }

   @Test
   public void isUpdatableColumn() {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      Introspected introspected = new Introspected(TestClass.class);
      assertTrue(introspected.isUpdatableColumn("Default_Case"));
      assertTrue(introspected.isUpdatableColumn("Delimited Field Name"));
   }

   @Test
   public void getIdColumnNames() {
      class TestClass {
         @Id @Column(name = "\"ID\"")
         String Id;
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
      assertEquals("Id4", idColumnNames[3]);
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
      assertEquals("Col3", columnsSansIds[2]);
      assertEquals("Col4", columnsSansIds[3]);
   }

   @Test
   public void insertObject() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
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
      assertEquals("INSERT INTO Test_Class(Default_Case,\"Delimited Field Name\") VALUES (?,?)", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
   }

   @Test
   public void objectById() throws SQLException {
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
      CaseSensitiveDatabasesClass obj = OrmReader.objectById(con, CaseSensitiveDatabasesClass.class, "xyz");
      assertEquals("SELECT Test_Class.Default_Case,Test_Class.\"Delimited Field Name\",Test_Class.Id FROM Test_Class Test_Class WHERE  Id=?", fetchedSql[0]);
      assertEquals(idValue, obj.getId());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
   }

   @Test
   public void updateObject() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
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
      assertEquals("UPDATE Test_Class SET Default_Case=?,\"Delimited Field Name\"=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
   }

   @Test
   public void deleteObject() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id = "xyz";
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
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
      assertEquals("DELETE FROM Test_Class WHERE Id=?", fetchedSql[0]);
      assertEquals("xyz", fetchedId[0]);
   }

   @Test
   public void deleteObjectById() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id = "xyz";
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
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
      assertEquals("DELETE FROM Test_Class WHERE Id=?", fetchedSql[0]);
      assertEquals("xyz", fetchedId[0]);
   }

   @Test
   public void statementToObject() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
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
      PreparedStatement pstmnt = con.prepareStatement("select * from Test_Class where Id = ?");
      CaseSensitiveDatabasesClass obj = OrmElf.statementToObject(pstmnt, CaseSensitiveDatabasesClass.class, "xyz");
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      assertEquals(idValue, obj.getId());
   }

   @Test
   public void deleteObjectNoIdProvided() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default Case")
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

   @Table(name = "\"Test Class\"")
   public static class InsertObjectH2 {
      @Id @GeneratedValue
      int Id;
      @Column(name = "\"Delimited field name\"")
      String delimitedFieldName = "delimited field value";
      @Column(name = "Default_Case")
      String defaultCase = "default case value";
   }

   @Test
   public void insertObjectH2() {

      SansOrm.initializeTxNone(TestUtils.makeH2DataSource());
      SqlClosureElf.executeUpdate("CREATE TABLE \"Test Class\" ("
         + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
         + "\"Delimited field name\" VARCHAR(128), "
         + "Default_Case VARCHAR(128) "
         + ")");

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      InsertObjectH2 obj = SqlClosureElf.insertObject(new InsertObjectH2());
      assertEquals(1, obj.Id);
      obj = SqlClosureElf.getObjectById(InsertObjectH2.class, obj.Id);
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
