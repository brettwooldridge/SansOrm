package com.zaxxer.sansorm.internal;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosureElf;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.sansorm.TestUtils;
import org.sansorm.testutils.*;

import javax.persistence.*;
import java.sql.*;
import java.util.HashSet;

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
      // Preserve field order!!!
      assertEquals("\"Delimited Field Name\",Default_Case", cols);
   }

   @Test
   public void getColumnsCsvTableName() {
      class TestClass {
         @Column(name = "\"Delimited Field Name\"", table = "\"Delimited table name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case", table = "Default_Case_Table")
         String defaultCase;
      }
      String cols = OrmReader.getColumnsCsv(TestClass.class);
      // Preserve field order!!!
      assertEquals(cols, "\"Delimited table name\".\"Delimited Field Name\",Default_Case_Table.Default_Case");
   }

   @Test
   public void getColumnsCsvExclude() {
      String cols = OrmBase.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "Delimited Field Name");
      // Preserve field order!!!
      assertEquals("Id,Default_Case", cols);
      cols = OrmBase.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "Default_Case");
      // Preserve field order!!!
      assertEquals("Id,\"Delimited Field Name\"", cols);
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
      // Preserve field order!!!
      assertEquals("Default_Table_Name.\"Delimited Field Name\",\"DELIMITED_TABLE_NAME\".Default_Case", cols);
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
         @Id @GeneratedValue
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
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
      assertArrayEquals(new String[]{"Id", "\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getInsertableColumnsInsertableFalse() {
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"", insertable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", insertable = false)
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * Bug caused by PR #22: In Introspected#getInsertableColumns() insertableColumns is set to columns.addAll(Arrays.asList(columnsSansIds)) and insertable = false is ignored.
    */
   @Test
   public void getInsertableColumnsInsertableFalseGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"", insertable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", insertable = false)
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * Work around for {@link #getInsertableColumnsInsertableFalseGeneratedValue()}
    */
   @Test
   public void getInsertableColumnsInsertableFalseWithId() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column(insertable = false)
         String id;
         @Column(name = "\"Delimited Field Name\"", insertable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", insertable = false)
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * Work around for {@link #getInsertableColumnsInsertableFalseGeneratedValue()}
    */
   @Test
   public void getUpdatetableColumnsUpdatableFalseWithId() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column(updatable = false)
         String id;
         @Column(name = "\"Delimited Field Name\"", updatable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", updatable = false)
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * See {@link #getInsertableColumnsInsertableFalseGeneratedValue()}.
    */
   @Test
   public void getUpdatableColumnsUpdatableFalseGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"", updatable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", updatable = false)
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   @Test
   public void getInsertableColumnsGeneratedValue() {
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
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   /**
    * CLARIFY Behaves different from {@link OrmBase#getColumnsCsvExclude(Class, String...)} in that it does not qualify field names with table names. See {@link #getColumnsCsvExcludeWithTableName()}.
     */
   @Test
   public void getInsertableColumnsWithTableName() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String Id;
         @Column(name = "\"Delimited Field Name\"", table = "Default_Table_Name")
         String delimitedFieldName;
         @Column(name = "Default_Case", table="\"Delimited Table Name\"")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
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
      assertArrayEquals(new String[]{"Id", "\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getUpdatableColumns2() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getUpdatableColumnsGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspector.getIntrospected(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
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
         @Id
         String id;
         @Id @Column(name = "Id2")
         String id2;
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
         @Id @GeneratedValue
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql, String[] columnNames) {
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
               public ResultSet getGeneratedKeys() throws SQLException {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() throws SQLException {
                        return true;
                     }

                     @Override
                     public Object getObject(int columnIndex) throws SQLException {
                        return "auto-generated id";
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.insertObject(con, new TestClass());
      assertEquals("INSERT INTO Test_Class(\"Delimited Field Name\",Default_Case) VALUES (?,?)", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
   }

   @Test
   public void insertObjectGeneratedValue() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column(name = "\"Id\"")
         String id;
         @Column(name = "\"Delimited field name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql, String[] columnNames) {
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
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return true;
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return "123";
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.insertObject(con, new TestClass());
      assertEquals("INSERT INTO Test_Class(\"Delimited field name\",Default_Case) VALUES (?,?)", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
      assertEquals("123", obj.id);
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
      // Preserve field order!!!
      assertEquals("SELECT Test_Class.Id,Test_Class.\"Delimited Field Name\",Test_Class.Default_Case FROM Test_Class Test_Class WHERE  Id=?", fetchedSql[0]);
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
      assertEquals("UPDATE Test_Class SET \"Delimited Field Name\"=?,Default_Case=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
   }

   @Test
   public void updateObjectGeneratedId() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
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

               @Override
               public ResultSet getGeneratedKeys() throws SQLException {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() throws SQLException {
                        return true;
                     }

                     @Override
                     public Object getObject(int columnIndex) throws SQLException {
                        return "123";
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.updateObject(con, new TestClass());
      assertEquals("UPDATE Test_Class SET \"Delimited Field Name\"=?,Default_Case=? WHERE id=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
   }

   @Test
   public void updateObjectGeneratedDelimitedId() throws SQLException {
      String upperCaseValue = "delimited field value";
      int defaultCaseValue = 1;
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column(name = "\"Id\"")
         int id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
         int myInt = defaultCaseValue;
         @Column
         int myInt2 = defaultCaseValue;
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
                        return   param == 1 ? Types.INTEGER :
                                 param == 2 ? Types.VARCHAR :
                                 param == 3 ? Types.INTEGER
                                            : Types.INTEGER;
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return true;
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return 123;
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.updateObject(con, new TestClass());
      assertEquals("UPDATE Test_Class SET \"Delimited Field Name\"=?,Default_Case=?,myInt2=? WHERE \"Id\"=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.myInt);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
      assertEquals(123, obj.id);
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
               public void setObject(int parameterIndex, Object x, int targetSqlType) {
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
   public void countObjectsFromClause() throws SQLException {
      @Table
      class TestClass {
         @Id @Column(name = "Id")
         String id;
      }

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
                     public Object getObject(int columnIndex) {
                        return 123;
                     }
                  };
               }
            };
         }
      };
      int count = OrmElf.countObjectsFromClause(con, TestClass.class, "where \"Delimited Field Name\" = null");
      assertEquals("SELECT COUNT(TestClass.Id) FROM TestClass TestClass where \"Delimited Field Name\" = null", fetchedSql[0]);
      assertEquals(123, count);
   }

   @Test
   public void insertObjectH2() {

      SansOrm.initializeTxNone(TestUtils.makeH2DataSource());
      try {
         SqlClosureElf.executeUpdate(
               " CREATE TABLE \"Test Class\" ("
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
         int count = SqlClosureElf.countObjectsFromClause(InsertObjectH2.class, "\"Delimited field name\" = 'delimited field value'");
         assertEquals(1, count);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectH2GeneratedId() {

      SansOrm.initializeTxNone(TestUtils.makeH2DataSource());
      try {
         SqlClosureElf.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
            + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
            + "\"Delimited field name\" VARCHAR(128), "
            + "Default_Case VARCHAR(128) "
            + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         InsertObjectH2 obj = new InsertObjectH2();
         obj = SqlClosureElf.insertObject(obj);
         obj.defaultCase = "changed";
         obj = SqlClosureElf.updateObject(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectH2GeneratedDelimitedId() throws SQLException {

      @Table(name = "\"Test Class\"")
      class TestClass {
         @Id @GeneratedValue @Column(name = "\"Id\"")
         int id;
         @Column(name = "\"Delimited field name\"")
         String delimitedFieldName = "delimited field value";
         @Column(name = "Default_Case")
         String defaultCase = "default case value";
      }

      try {
         JdbcDataSource dataSource = TestUtils.makeH2DataSource();
         SansOrm.initializeTxNone(dataSource);
         SqlClosureElf.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
            + "\"Id\" INTEGER NOT NULL IDENTITY PRIMARY KEY, "
            + "\"Delimited field name\" VARCHAR(128), "
            + "Default_Case VARCHAR(128) "
            + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         TestClass obj = new TestClass();
         obj = SqlClosureElf.insertObject(obj);
         obj.defaultCase = "changed";
         obj = SqlClosureElf.updateObject(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void getColumnNames() {
      Introspected introspected = new Introspected(InsertObjectH2.class);
      String[] columnNames = introspected.getColumnNames();
      // Preserve field order!!!
      assertArrayEquals(new String[]{"Id", "\"Delimited field name\"", "Default_Case"}, columnNames);
   }

   @Test
   public void resultSetToObjectIgnoredColumns() throws SQLException {

      @Table
      class ResultSetToObjectClass {
         @Id @Column(name = "Id")
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
         @Column
         String ignoredCol;
      }

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "Id value";
      String ignoredColValue = "ignored col";

      DummyResultSet rs = new DummyResultSet() {
         @Override
         public boolean next() {
            return true;
         }

         @Override
         public ResultSetMetaData getMetaData() {
            return new DummyResultSetMetaData() {
               @Override
               public int getColumnCount() {
                  return 4;
               }

               @Override
               public String getColumnName(int column) {
                  return   column == 1 ? "Delimited Field Name" :
                           column == 2 ? "default_case" :
                           column == 3 ? "ignoredcol" :
                           column == 4 ? "ID"
                                       : null;
               }
            };
         }

         @Override
         public Object getObject(int columnIndex) {
            return   columnIndex == 1 ? delimitedFieldValue :
                     columnIndex == 2 ? defaultCaseValue :
                     columnIndex == 3 ? ignoredColValue :
                     columnIndex == 4 ? idValue
                                      : null;
         }
      };

//      Introspected introspected = new Introspected(ResultSetToObjectClass.class);
      Introspector.getIntrospected(ResultSetToObjectClass.class);

      HashSet<String> ignoredCols = new HashSet<>();
      ignoredCols.add("ignoredCol");
      ResultSetToObjectClass obj = OrmElf.resultSetToObject(rs, new ResultSetToObjectClass(), ignoredCols);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(idValue, obj.id);
      assertEquals(null, obj.ignoredCol);
   }

   @Test
   public void resultSetToObjectDelimitedId() throws SQLException {

      @Table
      class ResultSetToObjectClass {
         @Id @Column(name = "\"Id\"")
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
         @Column
         String ignoredCol;
      }

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "Id value";
      String ignoredColValue = "ignored col";

      DummyResultSet rs = new DummyResultSet() {
         @Override
         public boolean next() {
            return true;
         }

         @Override
         public ResultSetMetaData getMetaData() {
            return new DummyResultSetMetaData() {
               @Override
               public int getColumnCount() {
                  return 4;
               }

               @Override
               public String getColumnName(int column) {
                  return   column == 1 ? "Delimited Field Name" :
                           column == 2 ? "default_case" :
                           column == 3 ? "ignoredcol" :
                           column == 4 ? "Id"
                                       : null;
               }
            };
         }

         @Override
         public Object getObject(int columnIndex) {
            return   columnIndex == 1 ? delimitedFieldValue :
                     columnIndex == 2 ? defaultCaseValue :
                     columnIndex == 3 ? ignoredColValue :
                     columnIndex == 4 ? idValue
                                      : null;
         }
      };

//      Introspected introspected = new Introspected(ResultSetToObjectClass.class);
      Introspector.getIntrospected(ResultSetToObjectClass.class);

      HashSet<String> ignoredCols = new HashSet<>();
//      ignoredCols.add("ignoredCol");
      ResultSetToObjectClass obj = OrmElf.resultSetToObject(rs, new ResultSetToObjectClass(), ignoredCols);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(idValue, obj.id);
      assertEquals(ignoredColValue, obj.ignoredCol);
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
