package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * "It is required that the entity class follow the method signature conventions for JavaBeans read/write properties (as defined by the JavaBeans Introspector class) for persistent properties when property access is used.” (JSR 317: JavaTM Persistence API, Version 2.0, 2.2 Persistent Fields and Properties)
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.04.18
 */
public class PropertyInfo extends AttributeInfo {

   private PropertyDescriptor propertyDescriptor;
   private Method readMethod;

   public PropertyInfo(Field field, Class clazz) {
      super(field, clazz);
   }

   protected void extractFieldName(final Field field) {
      try {
         propertyDescriptor = new PropertyDescriptor(field.getName(), clazz);
         readMethod = propertyDescriptor.getReadMethod();
         name = propertyDescriptor.getName();
      }
      catch (IntrospectionException ignored) {
         // In case of fields with no getters/setters according to JavaBean conventions.
         // Set name or NPE in setColumnName(String) will be thrown.
         name = field.getName();
      }
   }

   @Override
   protected OneToOne extractOneToOneAnnotation() {
      return readMethod.getDeclaredAnnotation(OneToOne.class);
   }

   @Override
   protected ManyToOne extractManyToOneAnnotation() {
      return readMethod.getDeclaredAnnotation(ManyToOne.class);
   }

   @Override
   protected ManyToMany extractManyToManyAnnotation() {
      return readMethod.getDeclaredAnnotation(ManyToMany.class);
   }

   @Override
   protected OneToMany extractOneToManyAnnotation() {
      return readMethod.getDeclaredAnnotation(OneToMany.class);
   }

   @Override
   protected JoinColumns extractJoinColumnsAnnotation() {
      return readMethod.getDeclaredAnnotation(JoinColumns.class);
   }

   @Override
   protected Transient extractTransientAnnotation() {
      return readMethod.getDeclaredAnnotation(Transient.class);
   }

   @Override
   protected JoinColumn extractJoinColumnAnnotation() {
      return readMethod.getDeclaredAnnotation(JoinColumn.class);
   }

   @Override
   protected Enumerated extractEnumeratedAnnotation() {
      return readMethod.getDeclaredAnnotation(Enumerated.class);
   }

   @Override
   protected GeneratedValue extractGeneratedValueAnnotation() {
      return readMethod.getDeclaredAnnotation(GeneratedValue.class);
   }

   @Override
   protected Id extractIdAnnotation() {
      return readMethod.getDeclaredAnnotation(Id.class);
   }

   @Override
   protected Convert extractConvertAnnotation() {
      return readMethod.getDeclaredAnnotation(Convert.class);
   }

   @Override
   protected Column extractColumnAnnotation() {
      return readMethod.getDeclaredAnnotation(Column.class);
   }

   public Object getValue(final Object target) throws IllegalAccessException, InvocationTargetException {
      if (!isSelfJoinField()) {
         return readMethod.invoke(target);
      }
      Object obj = readMethod.invoke(target);
      if (obj != null) {
         final Introspected introspected = new Introspected(obj.getClass());
         final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
         return generatedIdFcInfo.getValue(obj);
      }
      else {
         return null;
      }
   }

   public void setValue(final Object target, final Object value) throws IllegalAccessException {
      try {
         if (!isSelfJoinField()) {
            propertyDescriptor.getWriteMethod().invoke(target, value);
         }
         else {
            final Object obj = target.getClass().newInstance();
            final Introspected introspected = new Introspected(obj.getClass());
            final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
            generatedIdFcInfo.setValue(obj, value);
            propertyDescriptor.getWriteMethod().invoke(target, obj);
         }
      }
      catch (InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   @Override
   boolean isToBeConsidered() {
      return toBeConsidered && !isTransient;
   }

   protected void processJoinColumnAnnotation() {
      try {
         super.processJoinColumnAnnotation();
      }
      catch (Exception ignored) {
         // ignore java.lang.RuntimeException: JoinColumn annotations can only be self-referencing
         toBeConsidered = false;
      }
   }

   @Override
   public String toString() {
      return "PropertyInfo{" +
         "propertyDescriptor=" + propertyDescriptor +
         ", toBeConsidered=" + toBeConsidered +
         ", readMethod=" + readMethod +
         ", clazz=" + clazz +
         ", name='" + name + '\'' +
         ", field=" + field +
         ", type=" + type +
         ", isDelimited=" + isDelimited +
         ", updatable=" + updatable +
         ", insertable=" + insertable +
         ", columnName='" + columnName + '\'' +
         ", columnTableName='" + columnTableName + '\'' +
         ", enumType=" + enumType +
         ", enumConstants=" + enumConstants +
         ", converter=" + converter +
         ", caseSensitiveColumnName='" + caseSensitiveColumnName + '\'' +
         ", isGeneratedId=" + isGeneratedId +
         ", isIdField=" + isIdField +
         ", isJoinColumn=" + isJoinColumn +
         ", isTransient=" + isTransient +
         ", isEnumerated=" + isEnumerated +
         ", isColumnAnnotated=" + isColumnAnnotated +
         ", delimitedName='" + delimitedName + '\'' +
         ", fullyQualifiedDelimitedName='" + fullyQualifiedDelimitedName + '\'' +
         '}';
   }
}
