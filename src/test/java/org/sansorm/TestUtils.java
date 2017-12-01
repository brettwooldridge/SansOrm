package org.sansorm;

import org.h2.jdbcx.JdbcDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.io.File;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public final class TestUtils {
   private TestUtils() {
   }

   public static JdbcDataSource makeH2DataSource() {
      return makeH2DataSource(true);
   }

   public static JdbcDataSource makeH2DataSource(boolean autoCommit) {
      final JdbcDataSource dataSource = new JdbcDataSource();
      dataSource.setUrl(String.format("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;autocommit=%s", autoCommit ? "on" : "off"));
      return dataSource;
   }

   public static HikariDataSource makeSQLiteDataSource() {
      return makeSQLiteDataSource(null, true);
   }

   public static HikariDataSource makeSQLiteDataSource(File db) {
      return makeSQLiteDataSource(null, true);
   }

   public static HikariDataSource makeSQLiteDataSource(boolean autoCommit) {
      return makeSQLiteDataSource(null, autoCommit);
   }

   public static HikariDataSource makeSQLiteDataSource(File db, boolean autoCommit) {
      final SQLiteConfig sconfig = new SQLiteConfig();
      sconfig.setJournalMode(SQLiteConfig.JournalMode.MEMORY);
      SQLiteDataSource sds = new SQLiteDataSource(sconfig);
      sds.setUrl(db == null
         ? "jdbc:sqlite::memory:"
         : "jdbc:sqlite:" + db.getAbsolutePath()
      );

      HikariConfig hconfig = new HikariConfig();
      hconfig.setAutoCommit(autoCommit);
      hconfig.setDataSource(sds);
      hconfig.setMaximumPoolSize(1);
      return new HikariDataSource(hconfig);
   }
}
