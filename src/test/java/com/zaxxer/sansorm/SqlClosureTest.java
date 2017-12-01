package com.zaxxer.sansorm;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.TestUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(Parameterized.class)
public class SqlClosureTest {
   @Parameterized.Parameters(name = "autocommit={0}, ut={1}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
         { true, true }, { true, false }, { false, true }, { false, false }
      });
   }

   @Parameterized.Parameter(0)
   public boolean withAutoCommit;

   @Parameterized.Parameter(1)
   public boolean withUserTx;

   @Before // not @BeforeClass to have fresh table in each test, also sde
   public void setUp() throws IOException {
      final JdbcDataSource dataSource = TestUtils.makeH2DataSource(/*autoCommit=*/withAutoCommit);
      if (withUserTx) {
         SansOrm.initializeTxSimple(dataSource);
      } else {
         SansOrm.initializeTxNone(dataSource);
      }
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
      assertThat(insertedValues).containsOnly("1", "2");
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

      final Set<String> insertedValues = SqlClosure.sqlExecute(SqlClosureTest::getStrings);
      assertThat(insertedValues).isEmpty();
   }

   @Test
   public void shouldRollbackNestedClosuresWithUserTransaction() {
      assertThatThrownBy(() -> SqlClosure.sqlExecute(c -> {
         SqlClosureElf.executeUpdate(c, "INSERT INTO tx_test VALUES (?)", "6");

         // here goes nested SqlClosure
         SqlClosure.sqlExecute(cNested -> SqlClosureElf.executeUpdate(cNested, "INSERT INTO tx_test VALUES (?)", "7"));

         SqlClosureElf.executeUpdate(c, "INSERT INTO tx_test VALUES (?)", "8");
         throw new Error("boom!"); // ie something not or type SQLException or RuntimeException
      })).isInstanceOf(Error.class).hasMessage("boom!");

      final Set<String> insertedValues = SqlClosure.sqlExecute(SqlClosureTest::getStrings);
      if (withUserTx) {
         assertThat(insertedValues).containsOnly().as("With UserTransaction nested closures share same tx scope");
      } else {
         assertThat(insertedValues).containsOnly("7").as("Without UserTransaction every closure defines it's own tx scope");
      }
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
