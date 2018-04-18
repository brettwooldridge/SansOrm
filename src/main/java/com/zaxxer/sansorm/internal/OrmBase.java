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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrmBase
 */
class OrmBase
{
   private static final Map<String, String> csvCache;

   static {
      csvCache = new ConcurrentHashMap<>();
   }

   protected OrmBase() {
      // protected constructor
   }

   protected static void populateStatementParameters(final PreparedStatement stmt, final Object... args) throws SQLException
   {
      final ParameterMetaData parameterMetaData = stmt.getParameterMetaData();
      final int paramCount = parameterMetaData.getParameterCount();
      if (paramCount > 0 && args.length < paramCount) {
         throw new RuntimeException("Too few parameters supplied for query");
      }

      for (int column = paramCount; column > 0; column--) {
         final int parameterType = parameterMetaData.getParameterType(column);
         final Object object = mapSqlType(args[column - 1], parameterType);
         stmt.setObject(column, object, parameterType);
      }
   }

   /**
    *
    * @see #getColumnsCsvExclude(Class, String...)
    */
   public static <T> String getColumnsCsv(final Class<T> clazz, final String... tablePrefix)
   {
      final String cacheKey = (tablePrefix == null || tablePrefix.length == 0 ? clazz.getName() : tablePrefix[0] + clazz.getName());
      return csvCache.computeIfAbsent(cacheKey, key -> {
        final StringBuilder sb = new StringBuilder();

        final Introspected introspected = Introspector.getIntrospected(clazz);
        final FieldColumnInfo[] selectableFields = introspected.getSelectableFcInfos();
        for (FieldColumnInfo selectableField : selectableFields) {
           sb.append(selectableField.getFullyQualifiedDelimitedFieldName(tablePrefix)).append(',');
        }

        return sb.deleteCharAt(sb.length() - 1).toString();
      });
   }

   /**
    * @param excludeColumns Case as in name element or property name. In case of delimited column names provide name without delimiters.
    * @return Selectable columns. Comma separated. In case of delimited column names the column names are surrounded by delimiters.
    */
   public static <T> String getColumnsCsvExclude(final Class<T> clazz, final String... excludeColumns)
   {
      final Set<String> excludes = new HashSet<>(Arrays.asList(excludeColumns));
      final StringBuilder sb = new StringBuilder();

      final Introspected introspected = Introspector.getIntrospected(clazz);
      final FieldColumnInfo[] selectableFields = introspected.getSelectableFcInfos();
      for (FieldColumnInfo selectableField : selectableFields) {
         if (!excludes.contains(selectableField.getCaseSensitiveColumnName())) {
            sb.append(selectableField.getFullyQualifiedDelimitedFieldName()).append(',');
         }
      }

      return sb.deleteCharAt(sb.length() - 1).toString();
   }

   protected static Object mapSqlType(final Object object, final int sqlType)
   {
      switch (sqlType) {
      case Types.TIMESTAMP:
         if (object instanceof Timestamp) {
            return object;
         }
         if (object instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) object).getTime());
         }
         break;
      case Types.DECIMAL:
         if (object instanceof BigInteger) {
            return new BigDecimal(((BigInteger) object));
         }
         break;
      case Types.SMALLINT:
         if (object instanceof Boolean) {
            return (((Boolean) object) ? (short) 1 : (short) 0);
         }
         break;
      default:
         break;
      }

      return object;
   }

   /**
    * Case insensitive comparison.
    */
   protected static boolean isIgnoredColumn(final Set<String> ignoredColumns, final String columnName) {
      return ignoredColumns.stream().anyMatch(s -> s.equalsIgnoreCase(columnName));
   }
}
