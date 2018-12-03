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
   final List<FieldColumnInfo> idFcInfos;
   private String tableName;
   /** Fields in case insensitive lexicographic order */
   private final TreeMap<String, FieldColumnInfo> columnToField;

   private final Map<String, FieldColumnInfo> propertyToField;
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
   Introspected(final Class<?> clazz) {

      this.clazz = clazz;
      this.columnToField = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // support both in- and case-sensitive DBs
      this.propertyToField = new HashMap<>();
      this.insertableFcInfos = new ArrayList<>();
      this.updatableFcInfos = new ArrayList<>();
      this.allFcInfos = new ArrayList<>();
      this.idFcInfos = new ArrayList<>();

      extractClassTableName();

      try {
         for (Field field : getDeclaredFields()) {
            final int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers)) {
               continue;
            }

            field.setAccessible(true);
            final FieldColumnInfo fcInfo = new FieldColumnInfo(field, clazz);

            if (!fcInfo.isTransient) {
               columnToField.put(fcInfo.getCaseSensitiveColumnName(), fcInfo);
               propertyToField.put(fcInfo.getPropertyName(), fcInfo);
               allFcInfos.add(fcInfo);
               if (fcInfo.isIdField) {
                  // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
                  idFcInfos.add(fcInfo);
                  isGeneratedId = isGeneratedId || fcInfo.isGeneratedId;
                  if (isGeneratedId && idFcInfos.size() > 1) {
                     throw new IllegalStateException("Cannot have multiple @Id annotations and @GeneratedValue at the same time.");
                  }
                  if (!fcInfo.isGeneratedId) {
                     if (fcInfo.isInsertable() == null || fcInfo.isInsertable()) {
                        insertableFcInfos.add(fcInfo);
                     }
                     if (fcInfo.isUpdatable() == null || fcInfo.isUpdatable()) {
                        updatableFcInfos.add(fcInfo);
                     }
                  }
               }
               else if (fcInfo.isSelfJoinField()) {
                  selfJoinFCInfo = fcInfo;
               }
               else {
                  if (fcInfo.isInsertable() == null || fcInfo.isInsertable()) {
                     insertableFcInfos.add(fcInfo);
                  }
                  if (fcInfo.isUpdatable() == null || fcInfo.isUpdatable()) {
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
    * Get the {@link FieldColumnInfo} for the specified column name.
    *
    * @param columnName case insensitive column name without delimiters.
    */
   FieldColumnInfo getFieldColumnInfo(final String columnName) {
      return columnToField.get(columnName);
   }

   /**
    * Get the declared {@link Field}s for the class, including declared fields from mapped
    * superclasses.
    */
   private Collection<Field> getDeclaredFields() {
      final LinkedList<Field> declaredFields = new LinkedList<>(Arrays.asList(clazz.getDeclaredFields()));
      for (Class<?> c = clazz.getSuperclass(); c != null; c = c.getSuperclass()) {
         // support fields from MappedSuperclass(es).
         // Do not support ambiguous annotation. Spec says:
         // "A mapped superclass has no separate table defined for it".
         if (c.getAnnotation(MappedSuperclass.class) != null) {
            if (c.getAnnotation(Table.class) == null) {
               declaredFields.addAll(Arrays.asList(c.getDeclaredFields()));
            }
            else {
               throw new RuntimeException("Class " + c.getName() + " annotated with @MappedSuperclass cannot also have @Table annotation");
            }
         }
      }
      return declaredFields;
   }

   /**
    * Get the table name specified by the {@link Table} annotation.
    */
   private void extractClassTableName() {
      final Table tableAnnotation = clazz.getAnnotation(Table.class);
      if (tableAnnotation != null) {
         final String tableName = tableAnnotation.name();
         this.tableName = tableName.isEmpty()
            ? clazz.getSimpleName() // as per documentation, empty name in Table "defaults to the entity name"
            : tableName;
      }
   }

   /**
    * Get the value of the specified field from the specified target object, possibly after applying a
    * {@link AttributeConverter}.
    *
    * @param target the target instance
    * @param fcInfo the {@link FieldColumnInfo} used to access the field value
    * @return the value of the field from the target object, possibly after applying a {@link AttributeConverter}
    */
   Object get(final Object target, final FieldColumnInfo fcInfo)
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
    * Set a field value of the specified target object.
    *
    * @param target the target instance
    * @param fcInfo the {@link FieldColumnInfo} used to access the field value
    * @param value the value to set into the field of the target instance, possibly after applying a
    *              {@link AttributeConverter}
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
               else if (fieldType == Double.class) {
                  columnValue = ((BigDecimal) columnValue).doubleValue();
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
    * @return true if this class has {@link JoinColumn} annotations
    */
   public boolean hasSelfJoinColumn()
   {
      return selfJoinFCInfo != null;
   }

   /**
    * Determines whether the specified column is a self-join column.
    *
    * @param columnName The column name to check. Requires case sensitive match of name element or property name without delimiters.
    * @return true if the specified column is a self-join column
    */
   public boolean isSelfJoinColumn(final String columnName)
   {
      return selfJoinFCInfo.getCaseSensitiveColumnName().equals(columnName);
   }

   /**
    * Get the name of the self-join column, if one is defined for this class.
    *
    * @return the self-join column name, or null
    */
   public String getSelfJoinColumn()
   {
      return selfJoinFCInfo != null ? selfJoinFCInfo.getColumnName() : null;
   }

   /**
    * @see #getSelfJoinColumn()
    * return the {@link FieldColumnInfo} of the self-join column, if one is defined for this class.
    */
   FieldColumnInfo getSelfJoinColumnInfo()
   {
      return selfJoinFCInfo;
   }

   /**
    * Get all of the columns defined for this introspected class. In case of delimited column names
    * the column name surrounded by delimiters.
    *
    * @return and array of column names
    */
   public String[] getColumnNames()
   {
      return delimitedColumnNames;
   }

   /**
    * Get all of the table names associated with the columns for this introspected class. In case of
    * delimited field names surrounded by delimiters.
    *
    * @return an array of column table names
    */
   public String[] getColumnTableNames()
   {
      return columnTableNames;
   }

   /**
    * Get all of the ID columns defined for this introspected class. In case of delimited field names
    * surrounded by delimiters.
    *
    * @return and array of column names
    */
   public String[] getIdColumnNames()
   {
      return idColumnNames;
   }

   /**
    * Get all of the columns defined for this introspected class, minus the ID columns. In case of
    * delimited field names surrounded by delimiters.
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
    * Get the insertable column names for this object.
    *
    * @return the insertable columns. In case of delimited column names the names are surrounded
    *         by delimiters.
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
   public boolean isInsertableColumn(final String columnName)
   {
      // Use index iteration to avoid generating an Iterator as side-effect
      final FieldColumnInfo[] fcInfos = getInsertableFcInfos();
      for (int i = 0; i < fcInfos.length; i++) {
        if (fcInfos[i].getCaseSensitiveColumnName().equals(columnName)) {
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
   public boolean isUpdatableColumn(final String columnName)
   {
      // Use index iteration to avoid generating an Iterator as side-effect
      final FieldColumnInfo[] fcInfos = getUpdatableFcInfos();
      for (int i = 0; i < fcInfos.length; i++) {
         if (fcInfos[i].getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   Object[] getActualIds(final Object target)
   {
      if (idColumnNames.length == 0) {
         return null;
      }

      try {
         final FieldColumnInfo[] fcInfos = idFieldColumnInfos;
         final Object[] ids = new Object[idColumnNames.length];
         for (int i = 0; i < fcInfos.length; i++) {
            ids[i] = fcInfos[i].field.get(target);
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
    * Get the delimited column name for the specified property name, or {@code null} if
    * no such property exists.
    *
    * CLARIFY Must be public?
    *
    * @return the delimited column name or {@code null}
    */
   public String getColumnNameForProperty(final String propertyName)
   {
     return Optional.ofNullable(propertyToField.get(propertyName))
                    .map(fcInfo -> fcInfo.getDelimitedColumnName())
                    .orElse(null);
   }

   private void precalculateColumnInfos(final List<FieldColumnInfo> idFcInfos)
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

   private static String readClob(final Clob clob) throws IOException, SQLException
   {
      try (final Reader reader = clob.getCharacterStream()) {
         final StringBuilder sb = new StringBuilder();
         final char[] cbuf = new char[1024];
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

   FieldColumnInfo getGeneratedIdFcInfo() {
      // If there is a @GeneratedValue annotation only one @Id field can exist.
      return idFieldColumnInfos[0];
   }

   FieldColumnInfo[] getUpdatableFcInfos() {
      return updatableFcInfosArray;
   }

   /** Fields in same order as supplied by Type inspection */
   FieldColumnInfo[] getSelectableFcInfos() {
      return selectableFcInfosArray;
   }

   public List<FieldColumnInfo> getIdFcInfos() {
      return idFcInfos;
   }
}
