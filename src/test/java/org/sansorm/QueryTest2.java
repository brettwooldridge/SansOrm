package org.sansorm;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.sansorm.QueryTest.prepareTestDatasource;

import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;


public class QueryTest2
{
   @BeforeClass
   public static void setup() throws Throwable
   {
      DataSource ds = prepareTestDatasource();
      SqlClosure.setDefaultDataSource(ds);
      SqlClosureElf.executeUpdate(
         "CREATE TABLE TargetClass2 ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY,"
            + " string VARCHAR(128),"
            + " someDate TIMESTAMP," // H2 is case-insensitive to column case, ResultSet::getMetaData will return it as SOMEDATE
            + " )");
   }

   @Test
   public void testObjectFromClause()
   {
      // given
      int timestamp = 42;
      String string = "Hi";
      TargetClass2 original = new TargetClass2(new Date(timestamp), string);
      SqlClosureElf.insertObject(original);

      // when
      TargetClass2 target = SqlClosureElf.objectFromClause(TargetClass2.class, "someDate = ?", timestamp);
      TargetClass2 targetAgain = SqlClosureElf.getObjectById(TargetClass2.class, target.getId());

      // then
      assertEquals(target.getId(), targetAgain.getId());
      assertEquals(string, target.getString());
      assertEquals(timestamp, target.getSomeDate().getTime());
      assertEquals(string, targetAgain.getString());
      assertEquals(timestamp, targetAgain.getSomeDate().getTime());
   }

   @Test
   public void testListFromClause()
   {
      // given
      int timestamp = 43;
      String string = "Ho";
      TargetClass2 original = new TargetClass2(new Date(timestamp), string);
      SqlClosureElf.insertObject(original);

      // when
      List<TargetClass2> target = SqlClosureElf.listFromClause(TargetClass2.class, "string = ?", string);

      // then
      assertEquals(string, target.get(0).getString());
      assertEquals(timestamp, target.get(0).getSomeDate().getTime());
   }

   @Test
   public void testNumberFromSql()
   {
      Number initialCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM TargetClass2");
      SqlClosureElf.insertObject(new TargetClass2(null, ""));

      Number newCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM TargetClass2");
      assertEquals(initialCount.intValue() + 1, newCount.intValue());

      int countCount = SqlClosureElf.countObjectsFromClause(TargetClass2.class, null);
      assertEquals(newCount.intValue(), countCount);
   }

   @Test
   public void testDate()
   {
      Date date = new Date();

      TargetClass2 target = SqlClosureElf.insertObject(new TargetClass2(date, "Date"));
      target = SqlClosureElf.getObjectById(TargetClass2.class, target.getId());

      assertEquals("Date", target.getString());
      assertEquals(date, target.getSomeDate());
   }
}
