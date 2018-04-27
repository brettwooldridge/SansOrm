package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.lang.reflect.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Column information about a field
 */
abstract class AttributeInfo
{
   protected final Class<?> clazz;
   protected String name;

   final Field field;
   Class<?> type;

   protected boolean isDelimited;
   protected Boolean updatable;
   protected Boolean insertable;
   protected String columnName;
   /** name without delimiter: name as is; delimited name: name as is with delimiters */
   String columnTableName = "";
   EnumType enumType;
   Map<Object, Object> enumConstants;
   protected AttributeConverter converter;
   protected String caseSensitiveColumnName;
   protected boolean isGeneratedId;
   boolean isIdField;
   protected boolean isJoinColumn;
   protected boolean isTransient;
   protected boolean isEnumerated;
   protected boolean isColumnAnnotated;
   protected String delimitedName;
   protected final String fullyQualifiedDelimitedName;

   public AttributeInfo(Field field, Class<?> clazz) {
      this.field = field;
      this.clazz = clazz;
      extractFieldName(field);
      adjustType(extractType());
      extractAnnotations();
      processFieldAnnotations();
      this.fullyQualifiedDelimitedName =
         columnTableName.isEmpty() ? delimitedName : columnTableName + "." + delimitedName;
   }

   protected abstract void extractFieldName(Field field);

   private Class<?> extractType() {
      return field.getType();
   }

   private void adjustType(Class<?> type) {
      if (type == null) {
         throw new IllegalArgumentException("AccessibleObject has to be of type Field or Method.");
      }
      // remap safe conversions
      if (type == Date.class) {
         this.type = Timestamp.class;
      }
      else if (type == int.class) {
         this.type = Integer.class;
      }
      else if (type == long.class) {
         this.type = Long.class;
      }
      else {
         this.type = type;
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
               setColumnName(name);
            }
            else {
               // CLARIFY Dead code? Never reached by tests.
               setColumnName(name);
            }
         }
      }
      processConvertAnnotation();
   }

   private void extractAnnotations() {
      Id idAnnotation = extractIdAnnotation();
      if (idAnnotation != null) {
         isIdField = true;
         GeneratedValue generatedAnnotation = extractGeneratedValueAnnotation();
         isGeneratedId = (generatedAnnotation != null);
      }

      Enumerated enumAnnotation = extractEnumeratedAnnotation();
      if (enumAnnotation != null) {
         isEnumerated = true;
         this.setEnumConstants(enumAnnotation.value());
      }
      JoinColumn joinColumnAnnotation = extractJoinColumnAnnotation();
      if (joinColumnAnnotation != null) {
         isJoinColumn = true;
      }
      Transient transientAnnotation = extractTransientAnnotation();
      if (transientAnnotation != null) {
         isTransient = true;
      }
      Column columnAnnotation = extractColumnAnnotation();
      if (columnAnnotation != null) {
         isColumnAnnotated = true;
      }
   }

   protected abstract Transient extractTransientAnnotation();

   protected abstract JoinColumn extractJoinColumnAnnotation();

   protected abstract Enumerated extractEnumeratedAnnotation();

   protected abstract GeneratedValue extractGeneratedValueAnnotation();

   protected abstract Id extractIdAnnotation();

   private void processConvertAnnotation()  {
      Convert convertAnnotation = extractConvertAnnotation();
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

   protected abstract Convert extractConvertAnnotation();

   /**
    * Processes &#64;Column annotated fields.
    */
   private void processColumnAnnotation() {
      Column columnAnnotation = extractColumnAnnotation();
      String columnName = columnAnnotation.name();
      setColumnName(columnName);

      this.columnTableName = columnAnnotation.table();
      insertable = columnAnnotation.insertable();
      updatable = columnAnnotation.updatable();
   }

   protected abstract Column extractColumnAnnotation();

   protected void processJoinColumnAnnotation() {
      JoinColumn joinColumnAnnotation = extractJoinColumnAnnotation();
      // Is the JoinColumn a self-join?
      if (type == clazz) {
         setColumnName(joinColumnAnnotation.name());
      }
      else {
         throw new RuntimeException("JoinColumn annotations can only be self-referencing: " + type.getCanonicalName() + " != "
            + clazz.getCanonicalName());
      }
   }

   private boolean isNotDelimited(final String columnName) {
      return !columnName.startsWith("\"") || !columnName.endsWith("\"");
   }

   private void setColumnName(final String columnName) {
      String colName = columnName.isEmpty()
         ? name // as per EJB specification, empty name in Column "defaults to the property or field name"
         : columnName;
      if (isNotDelimited(colName)) {
         this.columnName = colName.toLowerCase();
         caseSensitiveColumnName = colName;
         delimitedName = colName;
      }
      else {
         this.columnName = colName.substring(1, colName.length() - 1);
         caseSensitiveColumnName = this.columnName;
         delimitedName = colName;
         isDelimited = true;
      }
   }

   <T extends Enum<?>> void setEnumConstants(final EnumType enumType)
   {
      this.enumType = enumType;
      enumConstants = new HashMap<>();
      @SuppressWarnings("unchecked")
      final T[] enums = (T[]) this.type.getEnumConstants();
      for (T enumConst : enums) {
         Object key = (this.enumType == EnumType.ORDINAL ? enumConst.ordinal() : enumConst.name());
         enumConstants.put(key, enumConst);
      }
   }

   @Override
   public String toString()
   {
      return name + "->" + getColumnName();
   }

   public void setConverter(final AttributeConverter converter) {
      this.converter = converter;
   }

   public AttributeConverter getConverter() {
      return converter;
   }

   boolean isSelfJoinField() {
      return isJoinColumn && type == clazz;
   }

   /** name without delimiter: lower cased; delimited name: name as is without delimiters */
   public String getColumnName() {
      return columnName;
   }

   public String getName() {
      return name;
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
      return delimitedName;
   }

   /**
    *
    * @param tablePrefix Ignored when field has a non empty table element.
    */
   public String getFullyQualifiedDelimitedFieldName(String ... tablePrefix) {
      return columnTableName.isEmpty() && tablePrefix.length > 0
         ? tablePrefix[0] + "." + fullyQualifiedDelimitedName
         : fullyQualifiedDelimitedName;
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

   public abstract Object getValue(Object target) throws IllegalAccessException, InvocationTargetException;

   public abstract void setValue(Object target, Object value) throws IllegalAccessException;

   boolean isTransient() {
      return isTransient;
   }

   boolean isToBeConsidered() {
      return !isTransient;
   }
}
