package com.zaxxer.sansorm;

import com.zaxxer.sansorm.internal.OrmReaderTest;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SqlClosureElfTest {
   @Test
   public void getInClausePlaceholdersByItems()
   {
      assertThat(SqlClosureElf.getInClausePlaceholders()).isEqualTo(" ('s0me n0n-ex1st4nt v4luu') ");
      assertThat(SqlClosureElf.getInClausePlaceholders(0)).isEqualTo(" (?) ");
      assertThat(SqlClosureElf.getInClausePlaceholders("1")).isEqualTo(" (?) ");
      assertThat(SqlClosureElf.getInClausePlaceholders("1", "2", "3", "4", "5")).isEqualTo(" (?,?,?,?,?) ");
   }

   @Test
   public void getInClausePlaceholdersByCount()
   {
      assertThat(SqlClosureElf.getInClausePlaceholdersForCount(0)).isEqualTo(" ('s0me n0n-ex1st4nt v4luu') ");
      assertThat(SqlClosureElf.getInClausePlaceholdersForCount(1)).isEqualTo(" (?) ");
      assertThat(SqlClosureElf.getInClausePlaceholdersForCount(5)).isEqualTo(" (?,?,?,?,?) ");
      assertThatIllegalArgumentException().isThrownBy(() -> SqlClosureElf.getInClausePlaceholdersForCount(-1));
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

   /**
    * TODO Composite primary key is not supported???
    * <p>
    * java.lang.RuntimeException: org.h2.jdbc.JdbcSQLException: NULL not allowed for column "ID1"; SQL statement: INSERT INTO TestClass2(field) VALUES (?)
    */
   @Test
   public void insertObjectCompositeKeyH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try {
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

         SqlClosureElfTest.TestClass2 obj = SqlClosureElf.insertObject(new SqlClosureElfTest.TestClass2());
         assertEquals(id1, obj.id1);
         obj = SqlClosureElf.getObjectById(SqlClosureElfTest.TestClass2.class, obj.id1, obj.id2);
         assertNotNull(obj);

         SqlClosureElf.executeUpdate("update TestClass2 set field = 'changed'");

         SqlClosureElfTest.TestClass2 obj2 = OrmElf.refresh(ds.getConnection(), obj);
         assertTrue(obj == obj2);
         assertEquals("changed", obj.field);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE TestClass2");
      }
   }

}
