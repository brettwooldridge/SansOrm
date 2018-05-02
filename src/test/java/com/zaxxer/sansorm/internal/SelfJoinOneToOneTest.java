package com.zaxxer.sansorm.internal;

import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosure;
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
import java.util.List;

import static org.junit.Assert.*;

/**
 * In general associations are not supported with one exception in {@link OrmReader#resultSetToList(ResultSet, Class)} where a OneToOne self reference is resolved. This method is called from
 * <pre>
 * {@link com.zaxxer.sansorm.OrmElf#resultSetToList(ResultSet, Class)}
 * {@link com.zaxxer.sansorm.OrmElf#statementToList(PreparedStatement, Class, Object...)}
 * {@link com.zaxxer.sansorm.OrmElf#listFromClause(Connection, Class, String, Object...)}
 * {@link com.zaxxer.sansorm.SqlClosureElf#listFromClause(Class, String, Object...)}
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 01.05.18
 */
public class SelfJoinOneToOneTest {

   @org.junit.Test
   public void selfJoinFieldAccess() {
      class Test {
         @JoinColumn(name = "id", referencedColumnName = "parentId")
         private Test parent;
      }
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getSelfJoinColumnInfo();
      assertTrue(info.isToBeConsidered());
   }

   @Table
   public static class FieldAccessedOneToOneSelfJoin {
      @Id @GeneratedValue
      private int id;
      @JoinColumn(name = "parentId", referencedColumnName = "id")
      private FieldAccessedOneToOneSelfJoin parentId;
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

//   /**
//    * Support for referenced columns that are not primary key columns of the referenced table is optional. Applications that use such mappings will not be portable. (JSR 317: JavaTM Persistence API, Version 2.0, 11.1.21 JoinColumn Annotation)
//    */
//   @Test
//   public void selfJoinColumnH2() throws SQLException {
//
//      JdbcDataSource ds = TestUtils.makeH2DataSource();
//      SansOrm.initializeTxNone(ds);
//      try (Connection con = ds.getConnection()){
//         SqlClosureElf.executeUpdate(
//            " CREATE TABLE JOINTEST (" +
//               " "
//               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
//               + ", parentId INTEGER"
//               + ", type VARCHAR(128)" +
//               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
//               + ")");
//         FieldAccessedOneToOneSelfJoin parent = new FieldAccessedOneToOneSelfJoin();
//         parent.type = "parent";
//         SqlClosureElf.insertObject(parent);
//
//         // SansOrm does not persist children
//         FieldAccessedOneToOneSelfJoin child = new FieldAccessedOneToOneSelfJoin();
//         child.type = "child";
//         child.parentId = parent;
//         SqlClosureElf.updateObject(parent);
//         assertEquals(0, child.id);
//
//         // persist child explicitely
//         OrmWriter.insertObject(con, child);
//         assertTrue(child.id > 0);
//         int count = SqlClosureElf.countObjectsFromClause(FieldAccessedOneToOneSelfJoin.class, null);
//         assertEquals(2, count);
//
//         // child retrieved, but without parent
//         FieldAccessedOneToOneSelfJoin obj3 = SqlClosureElf.objectFromClause(FieldAccessedOneToOneSelfJoin.class, "id=2");
//         assertEquals(null, obj3.parentId);
//
//         // child retrieved, but without parent
//         List<FieldAccessedOneToOneSelfJoin> objs = SqlClosureElf.listFromClause(FieldAccessedOneToOneSelfJoin.class, "id=2");
//         objs.forEach(System.out::println);
//      }
//      finally {
//         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
//      }
//   }

   @Table(name = "JOINTEST")
   public static class PropertyAccessedOneToOneSelfJoin {
      private int id;
      private PropertyAccessedOneToOneSelfJoin parentId;
      private String type;

      @Override
      public String toString() {
         return "Test{" +
            "id=" + id +
            ", parentId=" + parentId +
            ", type='" + type + '\'' +
            '}';
      }

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToOne
      @JoinColumn(name = "parentId", referencedColumnName = "id")
      public PropertyAccessedOneToOneSelfJoin getParentId() {
         return parentId;
      }

      public void setParentId(PropertyAccessedOneToOneSelfJoin parentId) {
         this.parentId = parentId;
      }

      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }
   }

   @Test
   public void introspectJoinColumn() {

      Introspected introspected = new Introspected(PropertyAccessedOneToOneSelfJoin.class);
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
         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.type = "parent";
         SqlClosureElf.insertObject(parent);
         assertTrue(parent.id > 0);

         // SansOrm does not persist child when parent is persisted
         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.type = "child";
         child.parentId = parent;
         SqlClosureElf.updateObject(parent);
         assertEquals(0, child.id);

         // persist child explicitely. parentId from parent is also stored.
         OrmWriter.insertObject(con, child);
         assertTrue(child.id > 0);
         int count = SqlClosureElf.countObjectsFromClause(PropertyAccessedOneToOneSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         PropertyAccessedOneToOneSelfJoin childFromDb = SqlClosureElf.objectFromClause
            (PropertyAccessedOneToOneSelfJoin.class, "id=2");
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

         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.type = "parent";
         SqlClosureElf.insertObject(parent);

         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.type = "child";
         child.parentId = parent;
         OrmWriter.insertObject(con, child);

         List<PropertyAccessedOneToOneSelfJoin> objs = SqlClosureElf.listFromClause(PropertyAccessedOneToOneSelfJoin.class, "id=2");
         objs.forEach(System.out::println);
         Assertions.assertThat(objs).filteredOn(obj -> obj.parentId != null && obj.parentId.id == 1).size().isEqualTo(1);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }
}
