package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Column information about a field
 */
final class FieldColumnInfo
{
   private final Class<?> clazz;
   private final String propertyName;

   final Field field;
   final Class<?> fieldType;

   private boolean isDelimited;
   private Boolean updatable;
   private Boolean insertable;
   private String columnName;
   /** name without delimiter: lower cased; delimited name: name as is with delimiters */
   String columnTableName = "";
   EnumType enumType;
   Map<Object, Object> enumConstants;
   private AttributeConverter converter;
   private String caseSensitiveColumnName;
   boolean isGeneratedId;
   boolean isIdField;
   private boolean isJoinColumn;
   boolean isTransient;
   private boolean isEnumerated;
   private boolean isColumnAnnotated;
   private String delimitedFieldName;
   private final String fullyQualifiedDelimitedFieldName;

   public FieldColumnInfo(Field field, Class<?> clazz) {
      this.field = field;
      this.clazz = clazz;
      this.propertyName = field.getName();
      this.fieldType = getFieldType();
      extractAnnotations();
      processFieldAnnotations();
      this.fullyQualifiedDelimitedFieldName =
         columnTableName.isEmpty() ? delimitedFieldName : columnTableName + "." + delimitedFieldName;
   }

   private Class<?> getFieldType() {
      final Class<?> type = field.getType();

      // remap safe conversions
      if (type == Date.class) {
         return Timestamp.class;
      }
      else if (type == int.class) {
         return Integer.class;
      }
      else if (type == long.class) {
         return Long.class;
      }
      else {
         return type;
      }
   }

   private void processFieldAnnotations()
   {
      if (isColumnAnnotated) {
         processColumnAnnotation();
      }
      else  {
         if (isJoinColumn) {
            processJoinColumnAnnotation();
         }
         else {
            if (isIdField) {
               // @Id without @Column annotation, so preserve case of property name.
               setColumnName(field.getName());
            }
            else {
               // CLARIFY Dead code? Never reached by tests.
               setColumnName(field.getName());
            }
         }
      }
      processConvertAnnotation();
   }

   private void extractAnnotations() {
      Id idAnnotation = field.getAnnotation(Id.class);
      if (idAnnotation != null) {
         isIdField = true;
         GeneratedValue generatedAnnotation = field.getAnnotation(GeneratedValue.class);
         isGeneratedId = (generatedAnnotation != null);
      }

      Enumerated enumAnnotation = field.getAnnotation(Enumerated.class);
      if (enumAnnotation != null) {
         isEnumerated = true;
         this.setEnumConstants(enumAnnotation.value());
      }
      JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
      if (joinColumnAnnotation != null) {
         isJoinColumn = true;
      }
      Transient transientAnnotation = field.getAnnotation(Transient.class);
      if (transientAnnotation != null) {
         isTransient = true;
      }
      Column columnAnnotation = field.getAnnotation(Column.class);
      if (columnAnnotation != null) {
         isColumnAnnotated = true;
      }
   }

   private void processConvertAnnotation()  {
      Convert convertAnnotation = field.getAnnotation(Convert.class);
      if (convertAnnotation != null) {
         Class<?> converterClass = convertAnnotation.converter();
         if (!AttributeConverter.class.isAssignableFrom(converterClass)) {
            throw new RuntimeException(
               "Convert annotation only supports converters implementing AttributeConverter");
         }
         try {
            setConverter((AttributeConverter) converterClass.newInstance());
         }
         catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
         }
      }
   }

   /**
    * Processes &#64;Column annotated fields.
    */
   private void processColumnAnnotation() {
      Column columnAnnotation = field.getAnnotation(Column.class);
      String columnName = columnAnnotation.name();
      setColumnName(columnName);

      this.columnTableName = columnAnnotation.table();
      insertable = columnAnnotation.insertable();
      updatable = columnAnnotation.updatable();
   }

   private void processJoinColumnAnnotation() {
      JoinColumn joinColumnAnnotation = field.getAnnotation(JoinColumn.class);
      // Is the JoinColumn a self-join?
      if (field.getType() == clazz) {
         setColumnName(joinColumnAnnotation.name());
      }
      else {
         throw new RuntimeException("JoinColumn annotations can only be self-referencing: " + field.getType().getCanonicalName() + " != "
            + clazz.getCanonicalName());
      }
   }

   private boolean isNotDelimited(final String columnName) {
      return !columnName.startsWith("\"") || !columnName.endsWith("\"");
   }

   private void setColumnName(final String columnName) {
      String colName = columnName.isEmpty()
         ? field.getName() // as per EJB specification, empty name in Column "defaults to the property or field name"
         : columnName;
      if (isNotDelimited(colName)) {
         this.columnName = colName.toLowerCase();
         caseSensitiveColumnName = colName;
         delimitedFieldName = colName;
      }
      else {
         this.columnName = colName.substring(1, colName.length() - 1);
         caseSensitiveColumnName = this.columnName;
         delimitedFieldName = colName;
         isDelimited = true;
      }
   }

   <T extends Enum<?>> void setEnumConstants(final EnumType type)
   {
      enumType = type;
      enumConstants = new HashMap<>();
      @SuppressWarnings("unchecked")
      final T[] enums = (T[]) field.getType().getEnumConstants();
      for (T enumConst : enums) {
         Object key = (type == EnumType.ORDINAL ? enumConst.ordinal() : enumConst.name());
         enumConstants.put(key, enumConst);
      }
   }

   @Override
   public String toString()
   {
      return field.getName() + "->" + getColumnName();
   }

   public void setConverter(final AttributeConverter converter) {
      this.converter = converter;
   }

   public AttributeConverter getConverter() {
      return converter;
   }

   boolean isSelfJoinField() {
      return isJoinColumn && field.getType() == clazz;
   }

   /** name without delimiter: lower cased; delimited name: name as is without delimiters */
   public String getColumnName() {
      return columnName;
   }

   public String getPropertyName() {
      return propertyName;
   }

   /**
    * @return If set &#64;Column name value else property name. In case of delimited fields without delimiters.
    */
   public String getCaseSensitiveColumnName() {
      return caseSensitiveColumnName;
   }

   /**
    *
    * @return case sensitive column name. In case of delimited fields surrounded by delimiters.
    */
   public String getDelimitedColumnName() {
      return delimitedFieldName;
   }

   /**
    *
    * @param tablePrefix Ignored when field has a non empty table element.
    */
   public String getFullyQualifiedDelimitedFieldName(String ... tablePrefix) {
      return columnTableName.isEmpty() && tablePrefix.length > 0
         ? tablePrefix[0] + "." + fullyQualifiedDelimitedFieldName
         : fullyQualifiedDelimitedFieldName;
   }

   public boolean isDelimited() {
      return isDelimited;
   }

   public boolean isEnumerated() {
      return isEnumerated;
   }

   /**
    *
    * @return null: no @Column annotation. true: @Column annotation. false @Column with updatable = false
    */
   Boolean isUpdatable() {
      return updatable;
   }

   /**
    *
    * @return null: no @Column annotation. true: @Column annotation. false @Column with insertable = false
    */
   Boolean isInsertable() {
      return insertable;
   }
}
