package com.zaxxer.sansorm.transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.QueryTest;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;

public class TxTransactionManagerTest {
   @Before // not @BeforeClass to have fresh table in each test
   public void setUp() throws IOException {
      QueryTest.setUpDataSourceWithSimpleTx();
      SqlClosureElf.executeUpdate("CREATE TABLE tx_test (string VARCHAR(128))");
   }

   @After // not @AfterClass to have fresh table in each test
   public void tearDown() {
      SqlClosureElf.executeUpdate("DROP TABLE tx_test");
      SansOrm.deinitialize();
   }

   @Test
   public void shouldSupportNestedCalls() {
      final Set<String> insertedValues = SqlClosure.sqlExecute(c -> {
         SqlClosureElf.executeUpdate(c, "INSERT INTO tx_test VALUES (?)", "1");

         // here goes nested SqlClosure
         SqlClosure.sqlExecute(cNested -> SqlClosureElf.executeUpdate(cNested, "INSERT INTO tx_test VALUES (?)", "2"));

         return getStrings(c);
      });
      assertThat(insertedValues).containsExactlyInAnyOrder("1", "2");
   }

   @Test
   public void shouldRollbackHighestTx() {
      assertThatThrownBy(() -> SqlClosure.sqlExecute(c -> {
         SqlClosureElf.executeUpdate(c, "INSERT INTO tx_test VALUES (?)", "3");

         // here goes nested SqlClosure
         SqlClosure.sqlExecute(cNested -> {
            SqlClosureElf.executeUpdate(cNested, "INSERT INTO tx_test VALUES (?)", "4");
            throw new RuntimeException("boom!");
         });

         return SqlClosureElf.executeUpdate(c, "INSERT INTO tx_test VALUES (?)", "5");
      })).isInstanceOf(RuntimeException.class).hasMessage("boom!");

      final Set<String> insertedValues = SqlClosure.sqlExecute(TxTransactionManagerTest::getStrings);
      assertThat(insertedValues).isEmpty();
   }

   static Set<String> getStrings(Connection c) throws SQLException {
      ResultSet rs = SqlClosureElf.executeQuery(c, "SELECT string FROM tx_test;");
      Set<String> result = new HashSet<>();
      while (rs.next()) {
         result.add(rs.getString(1));
      }
      return result;
   }
}
