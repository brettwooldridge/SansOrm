package com.zaxxer.sansorm.internal;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosureElf;
import org.assertj.core.api.Assertions;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SelfJoinManyToOneFieldAccessTest {

   @Test
   public void selfJoinFieldAccess() {
      class Test {
         @JoinColumn(name = "id", referencedColumnName = "parentId")
         private Test parent;
      }
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getSelfJoinColumnInfo();
      assertTrue(info.isToBeConsidered());
   }

   @Table(name = "JOINTEST")
   public static class FieldAccessedSelfJoin {
      @Id @GeneratedValue
      private int id;
      @ManyToOne
      @JoinColumn(name = "parentId", referencedColumnName = "id")
      private FieldAccessedSelfJoin parentId;
      private String type;

      @Override
      public String toString() {
         return "Test{" +
            "id=" + id +
            ", parentId=" + parentId +
            ", type='" + type + '\'' +
            '}';
      }
   }

   @Test
   public void selfJoinColumnFieldAccessH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         SqlClosureElf.insertObject(parent);
         assertTrue(parent.id > 0);

         // SansOrm does not persist child when parent is persisted
         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         SqlClosureElf.updateObject(parent);
         assertEquals(0, child.id);

         // persist child explicitely. parentId from parent is also stored.
         OrmWriter.insertObject(con, child);
         assertTrue(child.id > 0);
         int count = SqlClosureElf.countObjectsFromClause(FieldAccessedSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         FieldAccessedSelfJoin childFromDb = SqlClosureElf.objectFromClause
            (FieldAccessedSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = OrmElf.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.parentId);
         assertEquals(1, childFromDb.parentId.id);

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.parentId.type);
         OrmElf.refresh(con, childFromDb.parentId);
         assertEquals("parent", childFromDb.parentId.type);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void introspectJoinColumn() {

      Introspected introspected = new Introspected(FieldAccessedSelfJoin.class);
      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
//      Arrays.stream(insertableFcInfos).forEach(System.out::println);
      assertEquals(2, insertableFcInfos.length);
   }

   @Test
   public void selfJoinColumnPropertyAccessH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         SqlClosureElf.insertObject(parent);
         assertTrue(parent.id > 0);

         // SansOrm does not persist child when parent is persisted
         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         SqlClosureElf.updateObject(parent);
         assertEquals(0, child.id);

         // persist child explicitely. parentId from parent is also stored.
         OrmWriter.insertObject(con, child);
         assertTrue(child.id > 0);
         int count = SqlClosureElf.countObjectsFromClause(FieldAccessedSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         FieldAccessedSelfJoin childFromDb = SqlClosureElf.objectFromClause
            (FieldAccessedSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = OrmElf.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.parentId);
         assertEquals(1, childFromDb.parentId.id);

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.parentId.type);
         OrmElf.refresh(con, childFromDb.parentId);
         assertEquals("parent", childFromDb.parentId.type);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClause() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         SqlClosureElf.insertObject(parent);

         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         OrmWriter.insertObject(con, child);

         List<FieldAccessedSelfJoin> objs = SqlClosureElf.listFromClause(FieldAccessedSelfJoin.class, "id=2");
         objs.forEach(System.out::println);
         Assertions.assertThat(objs).filteredOn(obj -> obj.parentId != null && obj.parentId.id == 1).size().isEqualTo(1);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void insertListNotBatched() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      SansOrm.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         SqlClosureElf.insertObject(parent);

         FieldAccessedSelfJoin parent2 = new FieldAccessedSelfJoin();
         parent2.type = "parent";
         SqlClosureElf.insertObject(parent2);

         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;

         FieldAccessedSelfJoin child2 = new FieldAccessedSelfJoin();
         child2.type = "child";
         child2.parentId = parent2;

         ArrayList<FieldAccessedSelfJoin> children = new ArrayList<>();
         children.add(child);
         children.add(child2);

         OrmWriter.insertListNotBatched(con, children);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

}
