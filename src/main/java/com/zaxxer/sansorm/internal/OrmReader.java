/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.sansorm.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * OrmReader
 */
// MULTIPLESTRINGS:OFF
public class OrmReader extends OrmBase
{
   private static final int CACHE_SIZE = Integer.getInteger("com.zaxxer.sansorm.statementCacheSize", 500);

   private static final Map<String, String> fromClauseStmtCache;

   static {
      fromClauseStmtCache = Collections.synchronizedMap(new LinkedHashMap<String, String>(CACHE_SIZE) {
         private static final long serialVersionUID = 6259942586093454872L;

         @Override
         protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });
   }

   public static <T> List<T> statementToList(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      try {
         return resultSetToList(statementToResultSet(stmt, args), clazz);
      }
      finally {
         stmt.close();
      }
   }

   public static ResultSet statementToResultSet(PreparedStatement stmt, Object... args) throws SQLException
   {
      populateStatementParameters(stmt, args);
      return stmt.executeQuery();
   }

   // COMPLEXITY:OFF
   public static <T> List<T> resultSetToList(ResultSet resultSet, Class<T> targetClass) throws SQLException
   {
      List<T> list = new ArrayList<>();
      if (!resultSet.next()) {
         resultSet.close();
         return list;
      }

      Introspected introspected = Introspector.getIntrospected(targetClass);
      final boolean hasJoinColumns = introspected.hasSelfJoinColumn();
      Map<T, Object> deferredSelfJoinFkMap = (hasJoinColumns ? new HashMap<>() : null);
      Map<Object, T> idToTargetMap = (hasJoinColumns ? new HashMap<>() : null);

      ResultSetMetaData metaData = resultSet.getMetaData();
      final int columnCount = metaData.getColumnCount();
      final String[] columnNames = new String[columnCount];
      for (int column = columnCount; column > 0; column--) {
         columnNames[column - 1] = metaData.getColumnName(column).toLowerCase();
      }

      try {
         do {
            T target = targetClass.newInstance();
            list.add(target);
            for (int column = columnCount; column > 0; column--) {
               Object columnValue = resultSet.getObject(column);
               if (columnValue == null) {
                  continue;
               }

               String columnName = columnNames[column - 1];

               if (hasJoinColumns && introspected.isSelfJoinColumn(columnName)) {
                  deferredSelfJoinFkMap.put(target, columnValue);
               }
               else {
                  introspected.set(target, columnName, columnValue);
               }
            }

            if (hasJoinColumns) {
               idToTargetMap.put(introspected.getActualIds(target)[0], target);
            }
         }
         while (resultSet.next());

         resultSet.close();

         if (hasJoinColumns) {
            // set the self join object instances based on the foreign key ids...
            String idColumn = introspected.getSelfJoinColumn();
            for (Entry<T, Object> entry : deferredSelfJoinFkMap.entrySet()) {
               T value = idToTargetMap.get(entry.getValue());
               if (value != null) {
                  introspected.set(entry.getKey(), idColumn, value);
               }
            }
         }
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

      return list;
   }
   // COMPLEXITY:ON

   public static <T> T statementToObject(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      populateStatementParameters(stmt, args);

      ResultSet resultSet = null;
      try {
         resultSet = stmt.executeQuery();
         if (resultSet.next()) {
            T target = clazz.newInstance();
            return resultSetToObject(resultSet, target);
         }

         return null;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
      finally {
         if (resultSet != null) {
            resultSet.close();
         }
         stmt.close();
      }
   }

   public static <T> T resultSetToObject(ResultSet resultSet, T target) throws SQLException
   {
      Set<String> ignoreNone = Collections.emptySet();
      return resultSetToObject(resultSet, target, ignoreNone);
   }

   public static <T> T resultSetToObject(ResultSet resultSet, T target, Set<String> ignoredColumns) throws SQLException
   {
      ResultSetMetaData metaData = resultSet.getMetaData();

      Introspected introspected = Introspector.getIntrospected(target.getClass());
      for (int column = metaData.getColumnCount(); column > 0; column--) {
         String columnName = metaData.getColumnName(column).toLowerCase();
         // To make names in ignoredColumns independend from database case sensitivity. Otherwise you have to write database dependent code.
         if (isIgnoredColumn(ignoredColumns, columnName)) {
            continue;
         }

         Object columnValue = resultSet.getObject(column);
         if (columnValue == null) {
            continue;
         }
         introspected.set(target, columnName, columnValue);
      }
      return target;
   }

   /**
    * Case insensitive comparison.
    */
   private static boolean isIgnoredColumn(Set<String> ignoredColumns, String columnName) {
      for (String ignoredColumn : ignoredColumns) {
         if (columnName.compareToIgnoreCase(ignoredColumn) == 0) {
            return true;
         }
      }
      return false;
   }


   public static <T> T objectById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      Introspected introspected = Introspector.getIntrospected(clazz);

      StringBuilder where = new StringBuilder();
      for (String column : introspected.getIdColumnNames()) {
         where.append(column).append("=? AND ");
      }

      // the where clause can be length of zero if we are loading an object that is presumed to
      // be the only row in the table and therefore has no id.
      if (where.length() > 0) {
         where.setLength(where.length() - 5);
      }

      return objectFromClause(connection, clazz, where.toString(), args);
   }

   public static <T> List<T> listFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      String sql = generateSelectFromClause(clazz, clause);

      PreparedStatement stmt = connection.prepareStatement(sql);

      return statementToList(stmt, clazz, args);
   }

   public static <T> T objectFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      String sql = generateSelectFromClause(clazz, clause);

      PreparedStatement stmt = connection.prepareStatement(sql);

      return statementToObject(stmt, clazz, args);
   }

   public static <T> int countObjectsFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      Introspected introspected = Introspector.getIntrospected(clazz);

      String tableName = introspected.getTableName();

      StringBuilder sql = new StringBuilder();
      sql.append("SELECT COUNT(").append(tableName).append('.');
      String[] idColumnNames = introspected.getIdColumnNames();
      if (idColumnNames.length > 0) {
         sql.append(idColumnNames[0]);
      }
      else {
         sql.append(introspected.getColumnNames()[0]);
      }
      sql.append(") FROM ").append(tableName).append(' ').append(tableName);
      if (clause != null && !clause.isEmpty()) {
         String upper = clause.toUpperCase();
         if (!upper.contains("WHERE") && !upper.contains("JOIN") && !upper.startsWith("ORDER")) {
            sql.append(" WHERE ");
         }
         sql.append(' ').append(clause);
      }

      return numberFromSql(connection, sql.toString(), args).intValue();
   }

   public static Number numberFromSql(Connection connection, String sql, Object... args) throws SQLException
   {
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
         populateStatementParameters(stmt, args);
         try (ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
               return (Number) resultSet.getObject(1);
            }
            return null;
         }
      }
   }

   private static <T> String generateSelectFromClause(Class<T> clazz, String clause)
   {
      String cacheKey = clazz.getName() + clause;

      String sql = fromClauseStmtCache.get(cacheKey);
      if (sql == null) {
         Introspected introspected = Introspector.getIntrospected(clazz);

         String tableName = introspected.getTableName();

         StringBuilder sqlSB = new StringBuilder();
         sqlSB.append("SELECT ").append(getColumnsCsv(clazz, tableName)).append(" FROM ").append(tableName).append(' ').append(tableName);
         if (clause != null && !clause.isEmpty()) {
            if (!clause.toUpperCase().contains("WHERE") && !clause.toUpperCase().contains("JOIN")) {
               sqlSB.append(" WHERE ");
            }
            sqlSB.append(' ').append(clause);
         }

         sql = sqlSB.toString();
         fromClauseStmtCache.put(cacheKey, sql);
      }

      return sql;
   }
}
