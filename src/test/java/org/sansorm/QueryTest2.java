package org.sansorm;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosureElf;


public class QueryTest2
{
   @BeforeClass
   public static void setup() throws Throwable
   {
      SansOrm.initializeTxNone(TestUtils.makeH2DataSource());
      SqlClosureElf.executeUpdate(
         "CREATE TABLE TargetClass2 ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY,"
            + " string VARCHAR(128),"
            + " someDate TIMESTAMP," // H2 is case-insensitive to column case, ResultSet::getMetaData will return it as SOMEDATE
            + " )");
   }

   @AfterClass
   public static void tearDown()
   {
      SansOrm.deinitialize();
   }

   @Test
   public void testObjectFromClause()
   {
      // given
      long timestamp = 42L;
      String string = "Hi";
      TargetClass2 original = new TargetClass2(new Date(timestamp), string);
      SqlClosureElf.insertObject(original);

      // when
      TargetClass2 target = SqlClosureElf.objectFromClause(TargetClass2.class, "someDate = ?", timestamp);
      TargetClass2 targetAgain = SqlClosureElf.getObjectById(TargetClass2.class, target.getId());

      // then
      assertThat(targetAgain.getId()).isEqualTo(target.getId());
      assertThat(target.getString()).isEqualTo(string);
      assertThat(target.getSomeDate().getTime()).isEqualTo(timestamp);
      assertThat(targetAgain.getString()).isEqualTo(string);
      assertThat(targetAgain.getSomeDate().getTime()).isEqualTo(timestamp);
   }

   @Test
   public void testListFromClause()
   {
      // given
      long timestamp = 43L;
      String string = "Ho";
      TargetClass2 original = new TargetClass2(new Date(timestamp), string);
      SqlClosureElf.insertObject(original);

      // when
      List<TargetClass2> target = SqlClosureElf.listFromClause(TargetClass2.class, "string = ?", string);

      // then
      assertThat(target.get(0).getString()).isEqualTo(string);
      assertThat(target.get(0).getSomeDate().getTime()).isEqualTo(timestamp);
   }

   @Test
   public void testNumberFromSql()
   {
      Number initialCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM TargetClass2");
      SqlClosureElf.insertObject(new TargetClass2(null, ""));

      Number newCount = SqlClosureElf.numberFromSql("SELECT count(id) FROM TargetClass2");
      assertThat(newCount.intValue()).isEqualTo(initialCount.intValue() + 1);

      int countCount = SqlClosureElf.countObjectsFromClause(TargetClass2.class, null);
      assertThat(countCount).isEqualTo(newCount.intValue());
   }

   @Test
   public void testDate()
   {
      Date date = new Date();

      TargetClass2 target = SqlClosureElf.insertObject(new TargetClass2(date, "Date"));
      target = SqlClosureElf.getObjectById(TargetClass2.class, target.getId());

      assertThat(target.getString()).isEqualTo("Date");
      assertThat(target.getSomeDate().getTime()).isEqualTo(date.getTime());
   }
}
