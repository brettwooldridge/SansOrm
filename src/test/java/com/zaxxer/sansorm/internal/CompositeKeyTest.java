package com.zaxxer.sansorm.internal;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosureElf;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sansorm.TestUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 22.04.18
 */
public class CompositeKeyTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void invalidCompositePrimaryKey() {
      class TestClass {
         @Id @GeneratedValue
         String Id1;
         @Id
         String Id2;
         @Id
         String Id3;
         String name;
      }
      thrown.expectMessage("Cannot have multiple @Id annotations and @GeneratedValue at the same time.");
      Introspected introspected = new Introspected(TestClass.class);
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


   @Test
   public void insertObjectCompositeKeyH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
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

         TestClass2 obj = SqlClosureElf.insertObject(new TestClass2());
         assertEquals(id1, obj.id1);
         obj = SqlClosureElf.getObjectById(TestClass2.class, obj.id1, obj.id2);
         assertNotNull(obj);

         SqlClosureElf.executeUpdate("update TestClass2 set field = 'changed'");

         TestClass2 obj2 = OrmElf.refresh(con, obj);
         assertTrue(obj == obj2);
         assertEquals("changed", obj.field);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE TestClass2");
      }
   }

   @Test
   public void updateObjectCompositeKeyH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
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

         TestClass2 obj = SqlClosureElf.insertObject(new TestClass2());

         obj = SqlClosureElf.getObjectById(obj.getClass(), obj.id1, obj.id2);
         assertNotNull(obj);
         assertEquals(null, obj.field);

         obj.field = "changed";
         OrmElf.updateObject(con, obj);
         obj = OrmElf.objectById(con, obj.getClass(), obj.id1, obj.id2);
         assertEquals("changed", obj.field);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE TestClass2");
      }
   }

   @Test
   public void deleteObjectCompositeKeyH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
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

         TestClass2 obj = SqlClosureElf.insertObject(new TestClass2());
         int rowCount = OrmElf.countObjectsFromClause(con, obj.getClass(), "field is null");
         assertEquals(1, rowCount);

         OrmElf.deleteObject(con, obj);
         rowCount = OrmElf.countObjectsFromClause(con, obj.getClass(), "field is null");
         assertEquals(0, 0);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE TestClass2");
      }
   }

}
