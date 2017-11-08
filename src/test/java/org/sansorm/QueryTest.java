package org.sansorm;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;
import com.zaxxer.sansorm.transaction.TransactionElf;
import com.zaxxer.sansorm.transaction.TxTransactionManager;
import com.zaxxer.sansorm.internal.Introspected;
import com.zaxxer.sansorm.internal.Introspector;

public class QueryTest
{
   static DataSource prepareTestDatasource() throws IOException {

      final JdbcDataSource dataSource = new JdbcDataSource();
      dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      TxTransactionManager tm = new TxTransactionManager(dataSource);
      TransactionElf.setTransactionManager(tm);
      TransactionElf.setUserTransaction(tm);

      return tm.getTxDataSource();
   }

   @BeforeClass
   public static void setup() throws Throwable
   {
      DataSource ds = prepareTestDatasource();
      SqlClosure.setDefaultDataSource(ds);
      SqlClosureElf.executeUpdate("CREATE TABLE target_class1 ("
         + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
         + "timestamp TIMESTAMP, "
         + "string VARCHAR(128), "
         + "string_from_number NUMERIC "
         + ")");
   }

   @Test
   public void shouldPerformCRUD()
   {
      Introspected is = Introspector.getIntrospected(TargetClass1.class);
      assertTrue("test is meaningful only if class has generated id", is.hasGeneratedId());
      assertArrayEquals(new String[]{"id"}, is.getIdColumnNames());

      TargetClass1 original = new TargetClass1(new Date(0), "Hi");
      assertEquals(0, original.getId());

      TargetClass1 inserted = SqlClosureElf.insertObject(original);
      assertSame("insertOject() only set generated id", original, inserted);
      int idAfterInsert = inserted.getId();
      assertNotEquals(0, idAfterInsert);

      TargetClass1 selected = SqlClosureElf.objectFromClause(TargetClass1.class, "string = ?", "Hi");
      assertEquals(idAfterInsert, selected.getId());
      assertEquals("Hi", selected.getString());
      assertEquals(0, selected.getTimestamp().getTime());

      selected.setString("Hi edited");
      TargetClass1 updated = SqlClosureElf.updateObject(selected);
      assertSame("updateObject() only set generated id if it was missing", original, inserted);
      assertEquals(idAfterInsert, updated.getId());
   }

   @Test
   public void testNumberFromSql()
   {
      Number initialCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM target_class1");
      SqlClosureElf.insertObject(new TargetClass1(null, ""));

      Number newCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM target_class1");
      assertEquals(initialCount.intValue() + 1, newCount.intValue());

      int countCount = SqlClosureElf.countObjectsFromClause(TargetClass1.class, null);
      assertEquals(newCount.intValue(), countCount);
   }

   @Test
   public void testDate()
   {
      Date date = new Date();

      TargetClass1 target = SqlClosureElf.insertObject(new TargetClass1(date, "Date"));
      target = SqlClosureElf.getObjectById(TargetClass1.class, target.getId());

      assertEquals("Date", target.getString());
      assertEquals(date, target.getTimestamp());
   }

   @Test
   public void testTimestamp()
   {
      Timestamp tstamp = new Timestamp(System.currentTimeMillis());
      tstamp.setNanos(200);

      TargetTimestampClass1 target = SqlClosureElf.insertObject(new TargetTimestampClass1(tstamp, "Timestamp"));
      target = SqlClosureElf.getObjectById(TargetTimestampClass1.class, target.getId());

      assertEquals("Timestamp", target.getString());
      assertEquals(Timestamp.class, target.getTimestamp().getClass());
      assertEquals(tstamp, target.getTimestamp());
      assertEquals(200, target.getTimestamp().getNanos());
   }

   @Test
   public void testConverterSave()
   {
      TargetClass1 target = SqlClosureElf.insertObject(new TargetClass1(null, null, "1234"));
      Number number = SqlClosureElf.numberFromSql("SELECT string_from_number + 1 FROM target_class1 where id = ?", target.getId());
      assertEquals(1235, number.intValue());
   }

   @Test
   public void testConverterLoad() throws Exception
   {
      TargetClass1 target = SqlClosureElf.insertObject(new TargetClass1(null, null, "1234"));
      final int targetId = target.getId();
      target = SqlClosure.sqlExecute(c -> {
         PreparedStatement pstmt = c.prepareStatement(
                 "SELECT t.id, t.timestamp, t.string, (t.string_from_number + 1) as string_from_number FROM target_class1 t where id = ?");
         return OrmElf.statementToObject(pstmt, TargetClass1.class, targetId);
      });
      assertEquals("1235", target.getStringFromNumber());
   }

   @Test
   public void testConversionFail()
   {
      TargetClass1 target = SqlClosureElf.insertObject(new TargetClass1(null, null, "foobar"));
      target = SqlClosureElf.getObjectById(TargetClass1.class, target.getId());
      assertNull(target.getStringFromNumber());
   }


   @Test
   public void testInsertListNotBatched2() {
      // given
      int count = 5;
      Set<TargetClass1> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClass1(new Date(i), String.valueOf(i)))
         .collect(Collectors.toSet());

      // when
      SqlClosure.sqlExecute(c -> {
         OrmElf.insertListNotBatched(c, toInsert);
         return null;
      });

      // then
      Set<Integer> generatedIds = toInsert.stream().map(BaseClass::getId).collect(Collectors.toSet());
      assertFalse("Generated ids should be filled for passed objects", generatedIds.contains(0));
      assertEquals("Generated ids should be unique", count, generatedIds.size());
   }

   @Test
   public void testInsertListBatched() {
      // given
      int count = 5;
      String u = UUID.randomUUID().toString();
      Set<TargetClass1> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClass1(new Date(i), u + String.valueOf(i)))
         .collect(Collectors.toSet());

      // when
      SqlClosure.sqlExecute(c -> {
         OrmElf.insertListBatched(c, toInsert);
         return null;
      });

      // then
      List<TargetClass1> inserted = SqlClosureElf.listFromClause(
         TargetClass1.class,
         "string in " + OrmElf.getInClausePlaceholdersForCount(count),
         IntStream.range(0, count).boxed().map(i -> u + String.valueOf(i)).collect(Collectors.toList()).toArray(new Object[]{}));
      Set<Integer> generatedIds = inserted.stream().map(BaseClass::getId).collect(Collectors.toSet());
      assertFalse("Generated ids should be filled for passed objects", generatedIds.contains(0));
      assertEquals("Generated ids should be unique", count, generatedIds.size());
   }
}
