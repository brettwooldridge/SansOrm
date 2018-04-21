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
import java.util.*;

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
         protected boolean removeEldestEntry(final Map.Entry<Introspected, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });

      updateStatementCache = Collections.synchronizedMap(new LinkedHashMap<Introspected, String>(CACHE_SIZE) {
         private static final long serialVersionUID = -5324251353646078607L;

         @Override
         protected boolean removeEldestEntry(final Map.Entry<Introspected, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });
   }

   public static <T> void insertListBatched(final Connection connection, final Iterable<T> iterable) throws SQLException
   {
      final Iterator<T> iterableIterator = iterable.iterator();
      if (!iterableIterator.hasNext()) {
         return;
      }

      final Class<?> clazz = iterableIterator.next().getClass();
      final Introspected introspected = Introspector.getIntrospected(clazz);
      final boolean hasSelfJoinColumn = introspected.hasSelfJoinColumn();
      if (hasSelfJoinColumn) {
         throw new RuntimeException("insertListBatched() is not supported for objects with self-referencing columns due to Derby limitations");
      }

      final FieldColumnInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      try (final PreparedStatement stmt = createStatementForInsert(connection, introspected, insertableFcInfos)) {
         final int[] parameterTypes = getParameterTypes(stmt);
         for (final T item : iterable) {
            setStatementParameters(item, introspected, insertableFcInfos, stmt, parameterTypes, null);
            stmt.addBatch();
         }
         stmt.executeBatch();
      }
   }

   public static <T> void insertListNotBatched(final Connection connection, final Iterable<T> iterable) throws SQLException
   {
      final Iterator<T> iterableIterator = iterable.iterator();
      if (!iterableIterator.hasNext()) {
         return;
      }

      final Class<?> clazz = iterableIterator.next().getClass();
      final Introspected introspected = Introspector.getIntrospected(clazz);
      final boolean hasSelfJoinColumn = introspected.hasSelfJoinColumn();
      final String[] idColumnNames = introspected.getIdColumnNames();
      final FieldColumnInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      // Insert
      try (final PreparedStatement stmt = createStatementForInsert(connection, introspected, insertableFcInfos)) {
         final int[] parameterTypes = getParameterTypes(stmt);
         for (final T item : iterable) {
            setStatementParameters(item, introspected, insertableFcInfos, stmt, parameterTypes, null);
            stmt.executeUpdate();
            fillGeneratedId(item, introspected, stmt, /*checkExistingId=*/false);
            stmt.clearParameters();
         }
      }

      // If there is a self-referencing column, update it with the generated IDs
      if (hasSelfJoinColumn) {
         final FieldColumnInfo selfJoinfcInfo = introspected.getSelfJoinColumnInfo();
         final String idColumn = idColumnNames[0];
         final StringBuilder sql = new StringBuilder("UPDATE ").append(introspected.getTableName())
            .append(" SET ").append(selfJoinfcInfo.getDelimitedColumnName())
            .append("=? WHERE ").append(idColumn).append("=?");
         try (final PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (final T item : iterable) {
               final Object referencedItem = introspected.get(item, selfJoinfcInfo);
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

   public static <T> T insertObject(final Connection connection, final T target) throws SQLException
   {
      final Class<?> clazz = target.getClass();
      final Introspected introspected = Introspector.getIntrospected(clazz);
      final FieldColumnInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      try (final PreparedStatement stmt = createStatementForInsert(connection, introspected, insertableFcInfos)) {
         setParamsExecute(target, introspected, insertableFcInfos, stmt, /*checkExistingId=*/false, null);
      }
      return target;
   }

   public static <T> T updateObject(final Connection connection, final T target) throws SQLException
   {
      return updateObject(connection, target, null);
   }

   public static <T> T updateObject(final Connection connection, final T target, final Set<String> excludedColumns) throws SQLException
   {
      final Class<?> clazz = target.getClass();
      final Introspected introspected = Introspector.getIntrospected(clazz);
      final FieldColumnInfo[] updatableFcInfos = introspected.getUpdatableFcInfos();
      if (excludedColumns == null) {
         try (final PreparedStatement stmt = createStatementForUpdate(connection, introspected, updatableFcInfos)) {
            setParamsExecute(target, introspected, updatableFcInfos, stmt, /*checkExistingId=*/true, null);
         }
      }
      else {
         try (final PreparedStatement stmt = createStatementForUpdate(connection, introspected, updatableFcInfos, excludedColumns)){
            setParamsExecute(target, introspected, updatableFcInfos, stmt, /*checkExistingId=*/true, excludedColumns);
         }
      }
      return target;
   }

   public static <T> int deleteObject(final Connection connection, final T target) throws SQLException
   {
      final Class<?> clazz = target.getClass();
      final Introspected introspected = Introspector.getIntrospected(clazz);

      return deleteObjectById(connection, clazz, introspected.getActualIds(target));
   }

   public static <T> int deleteObjectById(final Connection connection, final Class<T> clazz, final Object... args) throws SQLException
   {
      final Introspected introspected = Introspector.getIntrospected(clazz);

      final StringBuilder sql = new StringBuilder()
        .append("DELETE FROM ").append(introspected.getTableName())
        .append(" WHERE ");

      final String[] idColumnNames = introspected.getIdColumnNames();
      if (idColumnNames.length == 0) {
         throw new RuntimeException("No id columns provided in: " + clazz.getName());
      }

      for (final String idColumn : idColumnNames) {
         sql.append(idColumn).append("=? AND ");
      }
      sql.setLength(sql.length() - 5);

      return executeUpdate(connection, sql.toString(), args);
   }

   public static int executeUpdate(final Connection connection, final String sql, final Object... args) throws SQLException
   {
      try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
         populateStatementParameters(stmt, args);
         return stmt.executeUpdate();
      }
   }

   // -----------------------------------------------------------------------
   //                      P R I V A T E   M E T H O D S
   // -----------------------------------------------------------------------

   private static PreparedStatement createStatementForInsert(final Connection connection,
                                                             final Introspected introspected,
                                                             final FieldColumnInfo[] fcInfos) throws SQLException
   {
      final String sql = createStatementCache.computeIfAbsent(introspected, key -> {
         final String tableName = introspected.getTableName();
         final StringBuilder sqlSB = new StringBuilder("INSERT INTO ").append(tableName).append('(');
         final StringBuilder sqlValues = new StringBuilder(") VALUES (");

         for (final FieldColumnInfo fcInfo : fcInfos) {
            sqlSB.append(fcInfo.getDelimitedColumnName()).append(',');
            sqlValues.append("?,");
         }

         sqlValues.deleteCharAt(sqlValues.length() - 1);
         sqlSB.deleteCharAt(sqlSB.length() - 1).append(sqlValues).append(')');

         return sqlSB.toString();
      });

      if (introspected.hasGeneratedId()) {
         return connection.prepareStatement(sql, introspected.getIdColumnNames());
      }
      else {
         return connection.prepareStatement(sql);
      }
   }

   /**
    *
    * @return newly created or already cached statement.
    */
   private static PreparedStatement createStatementForUpdate(final Connection connection,
                                                             final Introspected introspected,
                                                             final FieldColumnInfo[] fieldColumnInfos) throws SQLException
   {
      final String sql = updateStatementCache.computeIfAbsent(introspected, key -> createSqlForUpdate(introspected, fieldColumnInfos, null));

      return connection.prepareStatement(sql);
   }

   /**
    * To exclude columns situative. Does not cache the statement.
    */
   private static PreparedStatement createStatementForUpdate(final Connection connection,
                                                             final Introspected introspected,
                                                             final FieldColumnInfo[] fieldColumnInfos,
                                                             final Set<String> excludedColumns) throws SQLException
   {
      final String sql = createSqlForUpdate(introspected, fieldColumnInfos, excludedColumns);
      return connection.prepareStatement(sql);
   }

   /**
    *
    * @return newly created statement
    */
   private static String createSqlForUpdate(final Introspected introspected, final FieldColumnInfo[] fieldColumnInfos, final Set<String> excludedColumns) {
      final StringBuilder sqlSB = new StringBuilder("UPDATE ").append(introspected.getTableName()).append(" SET ");
      for (final FieldColumnInfo fcInfo : fieldColumnInfos) {
//         if (excludedColumns == null || !excludedColumns.contains(column)) {
         if (excludedColumns == null || !isIgnoredColumn(excludedColumns, fcInfo.getColumnName())) {
            sqlSB.append(fcInfo.getDelimitedColumnName()).append("=?,");
         }
      }
      sqlSB.deleteCharAt(sqlSB.length() - 1);

      final String[] idColumnNames = introspected.getIdColumnNames();
      if (idColumnNames.length > 0) {
         sqlSB.append(" WHERE ");
         for (final String column : idColumnNames) {
            sqlSB.append(column).append("=? AND ");
         }
         sqlSB.setLength(sqlSB.length() - 5);
      }
      return sqlSB.toString();
   }

   /** You should close stmt by yourself */
   private static <T> void setParamsExecute(final T target,
                                            final Introspected introspected,
                                            final FieldColumnInfo[] fcInfos,
                                            final PreparedStatement stmt,
                                            final boolean checkExistingId,
                                            final Set<String> excludedColumns) throws SQLException
   {
      final int[] parameterTypes = getParameterTypes(stmt);
      int parameterIndex = setStatementParameters(target, introspected, fcInfos, /*hasSelfJoinColumn*/ stmt, parameterTypes, excludedColumns);

      // If there is still a parameter left to be set, it's the ID used for an update
      if (parameterIndex <= parameterTypes.length) {
         for (final Object id : introspected.getActualIds(target)) {
            stmt.setObject(parameterIndex, id, parameterTypes[parameterIndex - 1]);
            ++parameterIndex;
         }
      }

      stmt.executeUpdate();
      fillGeneratedId(target, introspected, stmt, checkExistingId);
   }

   /** Small helper to set statement parameters from given object */
   private static <T> int setStatementParameters(final T item,
                                                 final Introspected introspected,
                                                 final FieldColumnInfo[] fcInfos,
                                                 final PreparedStatement stmt,
                                                 final int[] parameterTypes,
                                                 final Set<String> excludedColumns) throws SQLException
   {
      int parameterIndex = 1;
      for (final FieldColumnInfo fcInfo : fcInfos) {
         if (excludedColumns == null || !isIgnoredColumn(excludedColumns, fcInfo.getColumnName())) {
            final int parameterType = parameterTypes[parameterIndex - 1];
            final Object object = mapSqlType(introspected.get(item, fcInfo), parameterType);
            if (object != null && !fcInfo.isSelfJoinField()) {
               stmt.setObject(parameterIndex, object, parameterType);
            }
            else {
               stmt.setNull(parameterIndex, parameterType);
            }
            ++parameterIndex;
         }
      }
      return parameterIndex;
   }

   /** Sets auto-generated ID if not set yet */
   private static <T> void fillGeneratedId(final T target,
                                           final Introspected introspected,
                                           final PreparedStatement stmt,
                                           final boolean checkExistingId) throws SQLException {
      if (!introspected.hasGeneratedId()) {
         return;
      }

      final FieldColumnInfo fcInfo = introspected.getGeneratedIdFcInfo();
      if (checkExistingId) {
         final Object idExisting = introspected.get(target, fcInfo);
         if (idExisting != null && (!(idExisting instanceof Integer) || (Integer) idExisting > 0)) {
            // a bit tied to implementation but let's assume that integer id <= 0 means that it was not generated yet
            return;
         }
      }
      try (final ResultSet generatedKeys = stmt.getGeneratedKeys()) {
         if (generatedKeys.next()) {
            introspected.set(target, fcInfo, generatedKeys.getObject(1));
         }
      }
   }

   private static int[] getParameterTypes(final PreparedStatement stmt) throws SQLException
   {
      final ParameterMetaData metaData = stmt.getParameterMetaData();
      final int[] parameterTypes = new int[metaData.getParameterCount()];
      for (int parameterIndex = 1; parameterIndex <= metaData.getParameterCount(); parameterIndex++) {
         parameterTypes[parameterIndex - 1] = metaData.getParameterType(parameterIndex);
      }
      return parameterTypes;
   }
}
