package my;

import com.zaxxer.sansorm.SqlClosureElf;
import com.zaxxer.sansorm.internal.CaseSensitiveDatabasesTest;
import com.zaxxer.sansorm.internal.OrmReader;
import org.junit.Test;
import org.sansorm.testutils.*;

import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 29.03.18
 */
public class Join {


   @Table(name = "MY_TABLE")
   public static class ObjectFromClause {
      @Id
      String id;
      @JoinColumn(name = "\"JOIN_COLUMN_NAME\"")
      ObjectFromClause joinColumnName;
   }

   @Test
   public void objectFromClause() throws SQLException {
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
                        return Join.this.getParameterCount(fetchedSql[0]);
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
                              return 2;
                           }

                           @Override
                           public String getColumnName(int column) {
                              return   column == 1 ? "id" :
                                 column == 2 ? "JOIN_COLUMN_NAME"
                                    : null;
                           }
                        };
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return   columnIndex == 1  ? "xyz" :
                           // TODO Wer erzeugt dieses Objekt?
                           columnIndex == 2  ? new ObjectFromClause()
                              : null;
                     }
                  };
               }
            };
         }
      };
      ObjectFromClause obj = OrmReader.objectFromClause(con, ObjectFromClause.class, "WHERE id = ?", "xyz");
      assertEquals("SELECT MY_TABLE.id,MY_TABLE.\"JOIN_COLUMN_NAME\" FROM MY_TABLE MY_TABLE WHERE id = ?", fetchedSql[0]);
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
