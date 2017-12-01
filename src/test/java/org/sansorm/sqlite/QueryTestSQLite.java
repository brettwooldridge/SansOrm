package org.sansorm.sqlite;

import org.junit.AfterClass;
import org.junit.Test;
import org.sansorm.TestUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.sansorm.OrmElf;
import com.zaxxer.sansorm.SansOrm;
import com.zaxxer.sansorm.SqlClosure;
import com.zaxxer.sansorm.SqlClosureElf;
import com.zaxxer.sansorm.internal.Introspected;
import com.zaxxer.sansorm.internal.Introspector;

public class QueryTestSQLite {
   public static Closeable prepareSQLiteDatasource(File db) {
      HikariDataSource hds = TestUtils.makeSQLiteDataSource(db);
      SansOrm.initializeTxNone(hds);
      SqlClosureElf.executeUpdate("CREATE TABLE IF NOT EXISTS TargetClassSQL ("
         + "id integer PRIMARY KEY AUTOINCREMENT,"
         + "string text NOT NULL,"
         + "timestamp INTEGER"
         + ')');
      return hds; // to close it properly
   }

   @AfterClass
   public static void tearDown()
   {
      SansOrm.deinitialize();
   }

   @Test
   public void shouldPerformCRUD() throws IOException {
      Introspected is = Introspector.getIntrospected(TargetClassSQL.class);
      assertThat(is.hasGeneratedId()).isTrue().as("test is meaningful only if class has generated id");
      assertThat(is.getIdColumnNames()).isEqualTo(new String[]{"id"});

      try (Closeable ignored = prepareSQLiteDatasource(null)) {
         TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
         assertThat(original.getId()).isNull();
         TargetClassSQL inserted = SqlClosureElf.insertObject(original);
         assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
         Integer idAfterInsert = inserted.getId();
         assertThat(idAfterInsert).isNotNull();

         List<TargetClassSQL> selectedAll = SqlClosureElf.listFromClause(TargetClassSQL.class, null);
         assertThat(selectedAll).isNotEmpty();

         TargetClassSQL selected = SqlClosureElf.objectFromClause(TargetClassSQL.class, "string = ?", "Hi");
         assertThat(selected.getId()).isEqualTo(idAfterInsert);
         assertThat(selected.getString()).isEqualTo("Hi");
         assertThat(selected.getTimestamp().getTime()).isEqualTo(0);

         selected.setString("Hi edited");
         TargetClassSQL updated = SqlClosureElf.updateObject(selected);
         assertThat(updated).isSameAs(selected).as("updateObject() only set generated id if it was missing");
         assertThat(updated.getId()).isEqualTo(idAfterInsert);
      }
   }

   @Test
   public void shouldPerformCRUDAfterReconnect() throws IOException {
      Introspected is = Introspector.getIntrospected(TargetClassSQL.class);
      assertThat(is.hasGeneratedId()).isTrue().as("test is meaningful only if class has generated id");
      assertThat(is.getIdColumnNames()).isEqualTo(new String[]{"id"});

      File path = File.createTempFile("sansorm", ".db");
      path.deleteOnExit();

      Integer idAfterInsert;
      try (Closeable ignored = prepareSQLiteDatasource(path)) {
         TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
         assertThat(original.getId()).isNull();
         TargetClassSQL inserted = SqlClosureElf.insertObject(original);
         assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
         idAfterInsert = inserted.getId();
         assertThat(idAfterInsert).isNotNull();
      }

      // reopen database, it is important for this test
      // then select previously inserted object and try to edit it
      try (Closeable ignored = prepareSQLiteDatasource(path)) {
         TargetClassSQL selected = SqlClosureElf.objectFromClause(TargetClassSQL.class, "string = ?", "Hi");
         assertThat(selected.getId()).isEqualTo(idAfterInsert);
         assertThat(selected.getString()).isEqualTo("Hi");
         assertThat(selected.getTimestamp().getTime()).isEqualTo(0L);

         selected.setString("Hi edited");
         TargetClassSQL updated = SqlClosureElf.updateObject(selected);
         assertThat(updated).isSameAs(selected).as("updateObject() only set generated id if it was missing");
         assertThat(updated.getId()).isEqualTo(idAfterInsert);
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
      try (Closeable ignored = prepareSQLiteDatasource(null)) {
         SqlClosure.sqlExecute(c -> {
            OrmElf.insertListNotBatched(c, toInsert);
            return null;
         });
      }

      // then
      Set<Integer> generatedIds = toInsert.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
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
      try (Closeable ignored = prepareSQLiteDatasource(null)) {
         SqlClosure.sqlExecute(c -> {
            OrmElf.insertListBatched(c, toInsert);
            return null;
         });
         List<TargetClassSQL> inserted = SqlClosureElf.listFromClause(
            TargetClassSQL.class,
            "string in " + SqlClosureElf.getInClausePlaceholdersForCount(count),
            IntStream.range(0, count).boxed().map(i -> u + String.valueOf(i)).collect(Collectors.toList()).toArray(new Object[]{}));

         // then
         Set<Integer> generatedIds = inserted.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
         assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
         assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
      }
   }
}
