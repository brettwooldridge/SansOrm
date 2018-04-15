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

import org.postgresql.util.PGobject;

import javax.persistence.*;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;

/**
 * An introspected class.
 */
public final class Introspected
{
   private final Class<?> clazz;
   private String tableName;
   /** Fields in case insensitive lexicographic order */
   private final TreeMap<String, FieldColumnInfo> columnToField;
   private final List<FieldColumnInfo> allFcInfos;
   private List<FieldColumnInfo> insertableFcInfos;
   private List<FieldColumnInfo> updatableFcInfos;
   private FieldColumnInfo selfJoinFCInfo;

   private boolean isGeneratedId;

   // We use arrays because iteration is much faster
   private FieldColumnInfo[] idFieldColumnInfos;
   private String[] idColumnNames;
   private String[] columnTableNames;
   private String[] insertableColumns;
   private String[] updatableColumns;
   private String[] delimitedColumnNames;
   private String[] caseSensitiveColumnNames;
   private String[] delimitedColumnsSansIds;
   private FieldColumnInfo[] insertableFcInfosArray;
   private FieldColumnInfo[] updatableFcInfosArray;
   private FieldColumnInfo[] selectableFcInfosArray;

   /**
    * Constructor. Introspect the specified class and cache various annotation data about it.
    *
    * @param clazz the class to introspect
    */
   Introspected(Class<?> clazz) {

      this.clazz = clazz;
      this.columnToField = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // support both in- and case-sensitive DBs
      insertableFcInfos = new ArrayList<>();
      updatableFcInfos = new ArrayList<>();
      allFcInfos = new ArrayList<>();

      extractClassTableName();

      try {
         List<FieldColumnInfo> idFcInfos = new ArrayList<>();
         for (Field field : getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers)) {
               continue;
            }

            field.setAccessible(true);
            FieldColumnInfo fcInfo = new FieldColumnInfo(field, clazz);

            if (!fcInfo.isTransient) {
               columnToField.put(fcInfo.getCaseSensitiveColumnName(), fcInfo);
               allFcInfos.add(fcInfo);
               if (fcInfo.isIdField) {
                  // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
                  idFcInfos.add(fcInfo);
                  isGeneratedId = fcInfo.isGeneratedId;
                  if (isGeneratedId && idFcInfos.size() > 1) {
                     throw new IllegalStateException("Cannot have multiple @Id annotations and @GeneratedValue at the same time.");
                  }
               }
               if (fcInfo.isSelfJoinField()) {
                  selfJoinFCInfo = fcInfo;
               }
               if (!fcInfo.isGeneratedId) {
                  if (fcInfo.insertable) {
                     insertableFcInfos.add(fcInfo);
                  }
                  if (fcInfo.updatable) {
                     updatableFcInfos.add(fcInfo);
                  }
               }
            }
         }

         precalculateColumnInfos(idFcInfos);

      } catch (Exception e) {
         // To ease debugging
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   /**
    * @param columnName case insensitive without delimiters.
    */
   FieldColumnInfo getFieldColumnInfo(String columnName) {
      return columnToField.get(columnName);
   }

   private Collection<Field> getDeclaredFields() {
      LinkedList<Field> declaredFields = new LinkedList<>(Arrays.asList(clazz.getDeclaredFields()));
      for (Class<?> c = clazz.getSuperclass(); c != null; c = c.getSuperclass()) {
         // support fields from MappedSuperclass(es)
         if (c.getAnnotation(MappedSuperclass.class) != null) {
            declaredFields.addAll(Arrays.asList(c.getDeclaredFields()));
         }
      }
      return declaredFields;
   }

   private void extractClassTableName() {
      Table tableAnnotation = clazz.getAnnotation(Table.class);
      if (tableAnnotation != null) {
         String tableName = tableAnnotation.name();
         this.tableName = tableName.isEmpty()
            ? clazz.getSimpleName() // as per documentation, empty name in Table "defaults to the entity name"
            : tableName;
      }
   }

   Object get(Object target, FieldColumnInfo fcInfo)
   {
      if (fcInfo == null) {
         throw new RuntimeException("FieldColumnInfo must not be null. Type is " + target.getClass().getCanonicalName());
      }

      try {
         Object value = fcInfo.field.get(target);
         // Fix-up column value for enums, integer as boolean, etc.
         if (fcInfo.getConverter() != null) {
            value = fcInfo.getConverter().convertToDatabaseColumn(value);
         } else if (fcInfo.enumConstants != null && value != null) {
            value = (fcInfo.enumType == EnumType.ORDINAL ? ((Enum<?>) value).ordinal() : ((Enum<?>) value).name());
         }

         return value;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @param target The target object.
    * @param fcInfo The column name.
    * @param value The column value.
    */
   void set(Object target, FieldColumnInfo fcInfo, Object value)
   {
      if (fcInfo == null) {
         throw new RuntimeException("FieldColumnInfo must not be null. Type is " + target.getClass().getCanonicalName());
      }

      try {
         final Class<?> fieldType = fcInfo.fieldType;
         Class<?> columnType = value.getClass();
         Object columnValue = value;

         if (fcInfo.getConverter() != null) {
            columnValue = fcInfo.getConverter().convertToEntityAttribute(columnValue);
         } else if (fieldType != columnType) {
            // Fix-up column value for enums, integer as boolean, etc.
            if (fieldType == boolean.class && columnType == Integer.class) {
               columnValue = (((Integer) columnValue) != 0);
            }
            else if (columnType == BigDecimal.class) {
               if (fieldType == BigInteger.class) {
                  columnValue = ((BigDecimal) columnValue).toBigInteger();
               }
               else if (fieldType == Integer.class) {
                  columnValue = (int) ((BigDecimal) columnValue).longValue();
               }
               else if (fieldType == Long.class) {
                  columnValue = ((BigDecimal) columnValue).longValue();
               }
            }
            else if (columnType == java.util.UUID.class && fieldType == String.class) {
               columnValue = columnValue.toString();
            }
            else if (fcInfo.enumConstants != null) {
               columnValue = fcInfo.enumConstants.get(columnValue);
            }
            else if (columnValue instanceof Clob) {
               columnValue = readClob((Clob) columnValue);
            }
            else if ("PGobject".equals(columnType.getSimpleName()) && "citext".equalsIgnoreCase(((PGobject) columnValue).getType())) {
               columnValue = ((PGobject) columnValue).getValue();
            }
         }

         fcInfo.field.set(target, columnValue);
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Determines whether this class has join columns.
    *
    * @return true if this class has @JoinColumn annotations
    */
   public boolean hasSelfJoinColumn()
   {
      return selfJoinFCInfo != null;
   }

   /**
    * Check if the introspected class has a self-join column defined.
    *
    * @param columnName the column name to check
    * @return true if the specified column is a self-join column
    */
   public boolean isSelfJoinColumn(String columnName)
   {
      return selfJoinFCInfo.getColumnName().equals(columnName);
   }

   /**
    * Get the self-join column, if one is defined for this class.
    *
    * @return the self-join column, or null
    */
   public String getSelfJoinColumn()
   {
      return selfJoinFCInfo != null ? selfJoinFCInfo.getColumnName() : null;
   }

   /**
    * @see #getSelfJoinColumn()
    */
   FieldColumnInfo getSelfJoinColumnInfo()
   {
      return selfJoinFCInfo;
   }

   /**
    * Get all of the columns defined for this introspected class. In case of delimited column names the column name surrounded by delimiters.
    *
    * @return and array of column names
    */
   public String[] getColumnNames()
   {
      return delimitedColumnNames;
   }

   /**
    * Get all of the table names associated with the columns for this introspected class. In case of delimited field names surrounded by delimiters.
    *
    * @return an array of column table names
    */
   public String[] getColumnTableNames()
   {
      return columnTableNames;
   }

   /**
    * Get all of the ID columns defined for this introspected class. In case of delimited field names surrounded by delimiters.
    *
    * @return and array of column names
    */
   public String[] getIdColumnNames()
   {
      return idColumnNames;
   }

   /**
    * Get all of the columns defined for this introspected class, minus the ID columns. In case of delimited field names surrounded by delimiters.
    *
    * @return and array of column names
    */
   public String[] getColumnsSansIds()
   {
      return delimitedColumnsSansIds;
   }

   public boolean hasGeneratedId()
   {
      return isGeneratedId;
   }

   /**
    * Get the insertable columns for this object.
    *
    * @return the insertable columns. In case of delimited column names the names are surrounded by delimiters.
    */
   public String[] getInsertableColumns()
   {
      return insertableColumns;
   }

   private void precalculateInsertableColumns() {
      insertableColumns = new String[insertableFcInfos.size()];
      insertableFcInfosArray = new FieldColumnInfo[insertableFcInfos.size()];
      for (int i = 0; i < insertableColumns.length; i++) {
         insertableColumns[i] = insertableFcInfos.get(i).getDelimitedColumnName();
         insertableFcInfosArray[i] = insertableFcInfos.get(i);
      }
   }

   /**
    * Get the updatable columns for this object.
    *
    * @return the updatable columns
    */
   public String[] getUpdatableColumns()
   {
      return updatableColumns;
   }

   private void precalculateUpdatableColumns() {
      updatableColumns = new String[updatableFcInfos.size()];
      updatableFcInfosArray = new FieldColumnInfo[updatableColumns.length];
      for (int i = 0; i < updatableColumns.length; i++) {
         updatableColumns[i] = updatableFcInfos.get(i).getDelimitedColumnName();
         updatableFcInfosArray[i] = updatableFcInfos.get(i);
      }
   }

   /**
    * Is this specified column insertable?
    *
    * @param columnName Same case as in name element or property name without delimeters.
    * @return true if insertable, false otherwise
    */
   public boolean isInsertableColumn(String columnName)
   {
      for (FieldColumnInfo fcInfo : getInsertableFcInfos()) {
         if (fcInfo.getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Is this specified column updatable?
    *
    * @param columnName Same case as in name element or property name without delimeters.
    * @return true if updatable, false otherwise
    */
   public boolean isUpdatableColumn(String columnName)
   {
      for (FieldColumnInfo fcInfo : getUpdatableFcInfos()) {
         if (fcInfo.getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   Object[] getActualIds(Object target)
   {
      if (idColumnNames.length == 0) {
         return null;
      }

      try {
         Object[] ids = new Object[idColumnNames.length];
         int i = 0;
         for (FieldColumnInfo fcInfo : idFieldColumnInfos) {
            ids[i++] = fcInfo.field.get(target);
         }
         return ids;
      }
      catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Get the table name defined for the introspected class.
    *
    * @return a table name
    */
   public String getTableName()
   {
      return tableName;
   }

   /**
    * CLARIFY Must be public?
    */
   public String getColumnNameForProperty(String propertyName)
   {
      for (FieldColumnInfo fcInfo : columnToField.values()) {
         if (fcInfo.getPropertyName().equals(propertyName)) {
            return fcInfo.getDelimitedColumnName();
         }
      }
      return null;
   }

   private void precalculateColumnInfos(List<FieldColumnInfo> idFcInfos)
   {
      idFieldColumnInfos = new FieldColumnInfo[idFcInfos.size()];
      idColumnNames = new String[idFcInfos.size()];
      String[] columnNames = new String[columnToField.size()];
      columnTableNames = new String[columnNames.length];
      caseSensitiveColumnNames = new String[columnNames.length];
      delimitedColumnNames = new String[columnNames.length];
      String[] columnsSansIds = new String[columnNames.length - idColumnNames.length];
      delimitedColumnsSansIds = new String[columnsSansIds.length];
      selectableFcInfosArray = new FieldColumnInfo[allFcInfos.size()];

      int fieldCount = 0, idCount = 0, sansIdCount = 0;

      for (FieldColumnInfo fcInfo : allFcInfos) {
         if (!fcInfo.isTransient) {
            columnNames[fieldCount] = fcInfo.getColumnName();
            caseSensitiveColumnNames[fieldCount] = fcInfo.getCaseSensitiveColumnName();
            delimitedColumnNames[fieldCount] = fcInfo.getDelimitedColumnName();
            columnTableNames[fieldCount] = fcInfo.columnTableName;
            selectableFcInfosArray[fieldCount] = fcInfo;
            if (!fcInfo.isIdField) {
               columnsSansIds[sansIdCount] = fcInfo.getColumnName();
               delimitedColumnsSansIds[sansIdCount] = fcInfo.getDelimitedColumnName();
               ++sansIdCount;
            }
            else {
               // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
               idColumnNames[idCount] = fcInfo.getDelimitedColumnName();
               idFieldColumnInfos[idCount] = fcInfo;
               ++idCount;
            }
         }
         ++fieldCount;
      }
      precalculateInsertableColumns();
      precalculateUpdatableColumns();
   }

   private static String readClob(Clob clob) throws IOException, SQLException
   {
      try (Reader reader = clob.getCharacterStream()) {
         StringBuilder sb = new StringBuilder();
         char[] cbuf = new char[1024];
         while (true) {
            int rc = reader.read(cbuf);
            if (rc == -1) {
               break;
            }
            sb.append(cbuf, 0, rc);
         }
         return sb.toString();
      }
   }

   String[] getCaseSensitiveColumnNames() {
      return caseSensitiveColumnNames;
   }

   FieldColumnInfo[] getInsertableFcInfos() {
      return insertableFcInfosArray;
   }

   FieldColumnInfo getIdColumnFcInfo() {
      return idFieldColumnInfos[0];
   }

   FieldColumnInfo[] getUpdatableFcInfos() {
      return updatableFcInfosArray;
   }

   /** Fields in same order as supplied by Type inspection */
   FieldColumnInfo[] getSelectableFcInfos() {
      return selectableFcInfosArray;
   }
}
