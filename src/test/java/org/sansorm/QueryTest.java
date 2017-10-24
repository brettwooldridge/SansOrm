package org.sansorm;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.common.XAResourceProducer;
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
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;
import com.zaxxer.sansorm.TransactionElf;

public class QueryTest
{
   static DataSource prepareTestDatasource() throws IOException {
      System.setProperty("org.slf4j.simpleLogger.log.bitronix.tm", "WARN");

      // We don't actually need the transaction manager to journal, this is just for testing
      System.setProperty("bitronix.tm.journal", "null");
      System.setProperty("bitronix.tm.serverId", "test");

      Properties props = new Properties();
      props.setProperty("resource.ds.className", "org.h2.jdbcx.JdbcDataSource");
      props.setProperty("resource.ds.uniqueName", "test-h2");
      props.setProperty("resource.ds.minPoolSize", "2");
      props.setProperty("resource.ds.maxPoolSize", "5");
      props.setProperty("resource.ds.driverProperties.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

      File file = File.createTempFile("btm", ".properties");
      file.deleteOnExit();
      try (OutputStream out = new FileOutputStream(file))
      {
         props.store(out, "");
      }

      TransactionManagerServices.getConfiguration().setResourceConfigurationFilename(file.getAbsolutePath());

      BitronixTransactionManager tm = TransactionManagerServices.getTransactionManager();
      TransactionElf.setTransactionManager(tm);
      TransactionElf.setUserTransaction(tm);

      Map<String, XAResourceProducer> resources = TransactionManagerServices.getResourceLoader().getResources();
      return (DataSource)resources.values().iterator().next();
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

   @AfterClass
   public static void tearDown()
   {
      TransactionManagerServices.getTransactionManager().shutdown();
   }

   @Test
   public void testObjectFromClause()
   {
      TargetClass1 original = new TargetClass1(new Date(0), "Hi");
      SqlClosureElf.insertObject(original);

      TargetClass1 target = SqlClosureElf.objectFromClause(TargetClass1.class, "string = ?", "Hi");
      assertEquals("Hi", target.getString());
      assertEquals(0, target.getTimestamp().getTime());
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
      target = SqlClosure.sqlExecute((connection) -> {
         PreparedStatement pstmt = connection.prepareStatement(
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

}
