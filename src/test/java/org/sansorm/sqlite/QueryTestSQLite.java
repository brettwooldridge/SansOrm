package org.sansorm.sqlite;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;
import com.zaxxer.sansorm.transaction.TransactionElf;
import com.zaxxer.sansorm.internal.Introspected;
import com.zaxxer.sansorm.internal.Introspector;

public class QueryTestSQLite {
   static HikariDataSource prepareSQLiteDatasource(File db) throws IOException {
      final SQLiteConfig sconfig = new SQLiteConfig();
      sconfig.setJournalMode(SQLiteConfig.JournalMode.MEMORY);
      SQLiteDataSource sds = new SQLiteDataSource(sconfig);
      sds.setUrl(db == null
         ? "jdbc:sqlite::memory:"
         : "jdbc:sqlite:" + db.getAbsolutePath()
      );

      HikariConfig hconfig = new HikariConfig();
      hconfig.setAutoCommit(false);
      hconfig.setDataSource(sds);
      hconfig.setMaximumPoolSize(1);
      HikariDataSource hds = new HikariDataSource(hconfig);

      SqlClosure.setDefaultDataSource(hds);
      SqlClosureElf.executeUpdate("CREATE TABLE IF NOT EXISTS TargetClassSQL ("
         + "id integer PRIMARY KEY AUTOINCREMENT,"
         + "string text NOT NULL,"
         + "timestamp INTEGER"
         + ')');
      return hds;
   }

   @BeforeClass
   public static void setup() throws Throwable
   {
      TransactionElf.setUserTransaction(new UserTransaction() {
         @Override public int getStatus() throws SystemException { return Status.STATUS_NO_TRANSACTION;} // autocommit is disabled
         @Override public void begin() throws NotSupportedException, SystemException {}
         @Override public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {}
         @Override public void rollback() throws IllegalStateException, SecurityException, SystemException {}
         @Override public void setRollbackOnly() throws IllegalStateException, SystemException {}
         @Override public void setTransactionTimeout(int i) throws SystemException {}
      });
   }

   @AfterClass
   public static void tearDown()
   {
      SqlClosure.setDefaultDataSource(null);
      TransactionElf.setUserTransaction(null);
   }

   @Test
   public void shouldPerformCRUD() throws IOException {
      Introspected is = Introspector.getIntrospected(TargetClassSQL.class);
      assertTrue("test is meaningful only if class has generated id", is.hasGeneratedId());
      assertArrayEquals(new String[]{"id"}, is.getIdColumnNames());

      try (HikariDataSource ignored = prepareSQLiteDatasource(null)) {
         TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
         assertNull(original.getId());
         TargetClassSQL inserted = SqlClosureElf.insertObject(original);
         assertSame("insertObject() sets generated id", original, inserted);
         Integer idAfterInsert = inserted.getId();
         assertNotNull(idAfterInsert);

         TargetClassSQL selected = SqlClosureElf.objectFromClause(TargetClassSQL.class, "string = ?", "Hi");
         assertEquals(idAfterInsert, selected.getId());
         assertEquals("Hi", selected.getString());
         assertEquals(0, selected.getTimestamp().getTime());

         selected.setString("Hi edited");
         TargetClassSQL updated = SqlClosureElf.updateObject(selected);
         assertSame("updateObject() only set generated id if it was missing", selected, updated);
         assertEquals(idAfterInsert, updated.getId());
      }
   }

   @Test
   public void shouldPerformCRUDAfterReconnect() throws IOException {
      Introspected is = Introspector.getIntrospected(TargetClassSQL.class);
      assertTrue("test is meaningful only if class has generated id", is.hasGeneratedId());
      assertArrayEquals(new String[]{"id"}, is.getIdColumnNames());

      File path = File.createTempFile("sansorm", ".db");
      path.deleteOnExit();

      Integer idAfterInsert;
      try (HikariDataSource ignored = prepareSQLiteDatasource(path)) {
         TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
         assertNull(original.getId());
         TargetClassSQL inserted = SqlClosureElf.insertObject(original);
         assertSame("insertObject() sets generated id", original, inserted);
         idAfterInsert = inserted.getId();
         assertNotNull(idAfterInsert);
      }

      // reopen database, it is important for this test
      // then select previously inserted object and try to edit it
      try (HikariDataSource ignored = prepareSQLiteDatasource(path)) {
         TargetClassSQL selected = SqlClosureElf.objectFromClause(TargetClassSQL.class, "string = ?", "Hi");
         assertEquals(idAfterInsert, selected.getId());
         assertEquals("Hi", selected.getString());
         assertEquals(0, selected.getTimestamp().getTime());

         selected.setString("Hi edited");
         TargetClassSQL updated = SqlClosureElf.updateObject(selected);
         assertSame("updateObject() only set generated id if it was missing", selected, updated);
         assertEquals(idAfterInsert, updated.getId());
      }
   }

   @Test
   public void testInsertListNotBatched2() throws IOException {
      // given
      int count = 5;
      Set<TargetClassSQL> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClassSQL(String.valueOf(i), new Date(i)))
         .collect(Collectors.toSet());

      // when
      try (HikariDataSource ignored = prepareSQLiteDatasource(null)) {
         SqlClosure.sqlExecute(c -> {
            OrmElf.insertListNotBatched(c, toInsert);
            return null;
         });
      }

      // then
      Set<Integer> generatedIds = toInsert.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
      assertFalse("Generated ids should be filled for passed objects", generatedIds.contains(0));
      assertEquals("Generated ids should be unique", count, generatedIds.size());
   }

   @Test
   public void testInsertListBatched() throws IOException {
      // given
      int count = 5;
      String u = UUID.randomUUID().toString();
      Set<TargetClassSQL> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClassSQL(u + String.valueOf(i), new Date(i)))
         .collect(Collectors.toSet());

      // when
      try (HikariDataSource ignored = prepareSQLiteDatasource(null)) {
         SqlClosure.sqlExecute(c -> {
            OrmElf.insertListBatched(c, toInsert);
            return null;
         });
         List<TargetClassSQL> inserted = SqlClosureElf.listFromClause(
            TargetClassSQL.class,
            "string in " + OrmElf.getInClausePlaceholdersForCount(count),
            IntStream.range(0, count).boxed().map(i -> u + String.valueOf(i)).collect(Collectors.toList()).toArray(new Object[]{}));

         // then
         Set<Integer> generatedIds = inserted.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
         assertFalse("Generated ids should be filled for passed objects", generatedIds.contains(0));
         assertEquals("Generated ids should be unique", count, generatedIds.size());
      }
   }
}
