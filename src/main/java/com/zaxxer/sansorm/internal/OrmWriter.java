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
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OrmWriter
 */
public class OrmWriter extends OrmBase
{
   private static final int CACHE_SIZE = Integer.getInteger("com.zaxxer.sansorm.statementCacheSize", 500);
   private static final Map<Introspected, String> createStatementCache;
   private static final Map<Introspected, String> updateStatementCache;

   static {
      createStatementCache = Collections.synchronizedMap(new LinkedHashMap<Introspected, String>(CACHE_SIZE) {
         private static final long serialVersionUID = 4559270460685275064L;

         @Override
         protected boolean removeEldestEntry(Map.Entry<Introspected, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });

      updateStatementCache = Collections.synchronizedMap(new LinkedHashMap<Introspected, String>(CACHE_SIZE) {
         private static final long serialVersionUID = -5324251353646078607L;

         @Override
         protected boolean removeEldestEntry(Map.Entry<Introspected, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });
   }

   public static <T> void insertListBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      Iterator<T> iterableIterator = iterable.iterator();
      if (!iterableIterator.hasNext()) {
         return;
      }

      Class<?> clazz = iterableIterator.next().getClass();
      Introspected introspected = Introspector.getIntrospected(clazz);
      final boolean hasSelfJoinColumn = introspected.hasSelfJoinColumn();
      if (hasSelfJoinColumn) {
         throw new RuntimeException("insertListBatched() is not supported for objects with self-referencing columns due to Derby limitations");
      }

      String[] columnNames = introspected.getInsertableColumns();

      try (PreparedStatement stmt = createStatementForInsert(connection, introspected, columnNames)) {
         int[] parameterTypes = getParameterTypes(stmt);
         for (T item : iterable) {
            setStatementParameters(item, introspected, columnNames, hasSelfJoinColumn, stmt, parameterTypes);
            stmt.addBatch();
         }
         stmt.executeBatch();
      }
   }

   public static <T> void insertListNotBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      Iterator<T> iterableIterator = iterable.iterator();
      if (!iterableIterator.hasNext()) {
         return;
      }

      Class<?> clazz = iterableIterator.next().getClass();
      Introspected introspected = Introspector.getIntrospected(clazz);
      final boolean hasSelfJoinColumn = introspected.hasSelfJoinColumn();
      String[] idColumnNames = introspected.getIdColumnNames();
      String[] columnNames = introspected.getInsertableColumns();

      // Insert
      try (PreparedStatement stmt = createStatementForInsert(connection, introspected, columnNames)) {
         int[] parameterTypes = getParameterTypes(stmt);
         for (T item : iterable) {
            setStatementParameters(item, introspected, columnNames, hasSelfJoinColumn, stmt, parameterTypes);
            stmt.executeUpdate();
            fillGeneratedId(item, introspected, stmt, /*checkExistingId=*/false);
            stmt.clearParameters();
         }
      }

      // If there is a self-referencing column, update it with the generated IDs
      if (hasSelfJoinColumn) {
         final String selfJoinColumn = introspected.getSelfJoinColumn();
         final String idColumn = idColumnNames[0];
         StringBuilder sql = new StringBuilder("UPDATE ").append(introspected.getTableName()).append(" SET ");
         sql.append(selfJoinColumn).append("=? WHERE ").append(idColumn).append("=?");
         try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (T item : iterable) {
               Object referencedItem = introspected.get(item, selfJoinColumn);
               if (referencedItem != null) {
                  stmt.setObject(1, introspected.getActualIds(referencedItem)[0]);
                  stmt.setObject(2, introspected.getActualIds(item)[0]);
                  stmt.addBatch();
                  stmt.clearParameters();
               }
            }
            stmt.executeBatch();
         }
      }
   }

   public static <T> T insertObject(Connection connection, T target) throws SQLException
   {
      Class<?> clazz = target.getClass();
      Introspected introspected = Introspector.getIntrospected(clazz);
      String[] columnNames = introspected.getInsertableColumns();
      try (PreparedStatement stmt = createStatementForInsert(connection, introspected, columnNames)) {
         setParamsExecute(target, introspected, columnNames, stmt, /*checkExistingId=*/false);
      }
      return target;
   }

   public static <T> T updateObject(Connection connection, T target) throws SQLException
   {
      Class<?> clazz = target.getClass();
      Introspected introspected = Introspector.getIntrospected(clazz);
      String[] columnNames = introspected.getUpdatableColumns();
      try (PreparedStatement stmt = createStatementForUpdate(connection, introspected, columnNames)) {
         setParamsExecute(target, introspected, columnNames, stmt, /*checkExistingId=*/true);
      }
      return target;
   }

   public static <T> int deleteObject(Connection connection, T target) throws SQLException
   {
      Class<?> clazz = target.getClass();
      Introspected introspected = Introspector.getIntrospected(clazz);

      return deleteObjectById(connection, clazz, introspected.getActualIds(target));
   }

   public static <T> int deleteObjectById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      Introspected introspected = Introspector.getIntrospected(clazz);

      StringBuilder sql = new StringBuilder();
      sql.append("DELETE FROM ").append(introspected.getTableName()).append(" WHERE ");

      for (String idColumn : introspected.getIdColumnNames()) {
         sql.append(idColumn).append("=? AND ");
      }
      sql.setLength(sql.length() - 5);

      return executeUpdate(connection, sql.toString(), args);
   }

   public static int executeUpdate(Connection connection, String sql, Object... args) throws SQLException
   {
      try (PreparedStatement stmt = connection.prepareStatement(sql)) {
         populateStatementParameters(stmt, args);
         return stmt.executeUpdate();
      }
   }

   // -----------------------------------------------------------------------
   //                      P R I V A T E   M E T H O D S
   // -----------------------------------------------------------------------

   private static PreparedStatement createStatementForInsert(Connection connection, Introspected introspected, String[] columns) throws SQLException
   {
      String sql = createStatementCache.get(introspected);
      if (sql == null) {
         String tableName = introspected.getTableName();
         StringBuilder sqlSB = new StringBuilder("INSERT INTO ").append(tableName).append('(');
         StringBuilder sqlValues = new StringBuilder(") VALUES (");
         for (String column : columns) {
            sqlSB.append(column).append(',');
            sqlValues.append("?,");
         }
         sqlValues.deleteCharAt(sqlValues.length() - 1);
         sqlSB.deleteCharAt(sqlSB.length() - 1).append(sqlValues).append(')');

         sql = sqlSB.toString();
         createStatementCache.put(introspected, sql);
      }

      if (introspected.hasGeneratedId()) {
         return connection.prepareStatement(sql, introspected.getIdColumnNames());
      }
      else {
         return connection.prepareStatement(sql);
      }
   }

   private static PreparedStatement createStatementForUpdate(Connection connection, Introspected introspected, String[] columnNames) throws SQLException
   {
      String sql = updateStatementCache.get(introspected);
      if (sql == null) {
         StringBuilder sqlSB = new StringBuilder("UPDATE ").append(introspected.getTableName()).append(" SET ");
         for (String column : columnNames) {
            sqlSB.append(column).append("=?,");
         }
         sqlSB.deleteCharAt(sqlSB.length() - 1);

         String[] idColumnNames = introspected.getIdColumnNames();
         if (idColumnNames.length > 0) {
            sqlSB.append(" WHERE ");
            for (String column : idColumnNames) {
               sqlSB.append(column).append("=? AND ");
            }
            sqlSB.setLength(sqlSB.length() - 5);
         }

         sql = sqlSB.toString();
         updateStatementCache.put(introspected, sql);
      }

      return connection.prepareStatement(sql);
   }

   /** You should close stmt by yourself */
   private static <T> void setParamsExecute(T target, Introspected introspected, String[] columnNames, PreparedStatement stmt, boolean checkExistingId) throws SQLException
   {
      int[] parameterTypes = getParameterTypes(stmt);
      int parameterIndex = setStatementParameters(target, introspected, columnNames, /*hasSelfJoinColumn*/false, stmt, parameterTypes);

      // If there is still a parameter left to be set, it's the ID used for an update
      if (parameterIndex <= parameterTypes.length) {
         for (Object id : introspected.getActualIds(target)) {
            stmt.setObject(parameterIndex, id, parameterTypes[parameterIndex - 1]);
            ++parameterIndex;
         }
      }

      stmt.executeUpdate();
      fillGeneratedId(target, introspected, stmt, checkExistingId);
   }

   /** Small helper to set statement parameters from given object */
   private static <T> int setStatementParameters(T item, Introspected introspected, String[] columnNames, boolean hasSelfJoinColumn, PreparedStatement stmt, int[] parameterTypes) throws SQLException
   {
      int parameterIndex = 1;
      for (String column : columnNames) {
         int parameterType = parameterTypes[parameterIndex - 1];
         Object object = mapSqlType(introspected.get(item, column), parameterType);
         if (object != null && !(hasSelfJoinColumn && introspected.isSelfJoinColumn(column))) {
            stmt.setObject(parameterIndex, object, parameterType);
         }
         else {
            stmt.setNull(parameterIndex, parameterType);
         }
         ++parameterIndex;
      }
      return parameterIndex;
   }

   /** Sets auto-generated ID if not set yet */
   private static <T> void fillGeneratedId(T target, Introspected introspected, PreparedStatement stmt, boolean checkExistingId) throws SQLException {
      if (!introspected.hasGeneratedId()) {
         return;
      }
      final String idColumn = introspected.getIdColumnNames()[0];
      if (checkExistingId) {
         final Object idExisting = introspected.get(target, idColumn);
         if (idExisting != null && (!(idExisting instanceof Integer) || (Integer) idExisting > 0)) {
            // a bit tied to implementation but let's assume that integer id <= 0 means that it was not generated yet
            return;
         }
      }
      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
         if (generatedKeys.next()) {
            introspected.set(target, idColumn, generatedKeys.getObject(1));
         }
      }
   }

   private static int[] getParameterTypes(PreparedStatement stmt) throws SQLException
   {
      ParameterMetaData metaData = stmt.getParameterMetaData();
      int[] parameterTypes = new int[metaData.getParameterCount()];
      for (int parameterIndex = 1; parameterIndex <= metaData.getParameterCount(); parameterIndex++) {
         parameterTypes[parameterIndex - 1] = metaData.getParameterType(parameterIndex);
      }
      return parameterTypes;
   }
}
