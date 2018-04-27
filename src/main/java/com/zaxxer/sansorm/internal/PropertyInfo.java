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
   private boolean toBeConsidered;
   private Method readMethod;

   public PropertyInfo(Field field, Class clazz) {
      super(field, clazz);
   }

   protected void extractFieldName(Field field) {
      try {
         propertyDescriptor = new PropertyDescriptor(field.getName(), clazz);
         readMethod = propertyDescriptor.getReadMethod();
         name = propertyDescriptor.getName();
         toBeConsidered = true;
      }
      catch (IntrospectionException ignored) {
         // In case of fields with no getters/setters according to JavaBean conventions.
         // Set name or NPE in setColumnName(String) will be thrown.
         name = field.getName();
      }
   }

   @Override
   protected Transient extractTransientAnnotation() {
      return readMethod.getAnnotation(Transient.class);
   }

   @Override
   protected JoinColumn extractJoinColumnAnnotation() {
      return readMethod.getAnnotation(JoinColumn.class);
   }

   @Override
   protected Enumerated extractEnumeratedAnnotation() {
      return readMethod.getAnnotation(Enumerated.class);
   }

   @Override
   protected GeneratedValue extractGeneratedValueAnnotation() {
      return readMethod.getAnnotation(GeneratedValue.class);
   }

   @Override
   protected Id extractIdAnnotation() {
      return readMethod.getAnnotation(Id.class);
   }

   @Override
   protected Convert extractConvertAnnotation() {
      return readMethod.getAnnotation(Convert.class);
   }

   @Override
   protected Column extractColumnAnnotation() {
      return readMethod.getAnnotation(Column.class);
   }

   public Object getValue(Object target) throws IllegalAccessException, InvocationTargetException {
      return readMethod.invoke(target);
   }

   public void setValue(Object target, Object value) throws IllegalAccessException {
      try {
         propertyDescriptor.getWriteMethod().invoke(target, value);
      }
      catch (InvocationTargetException e) {
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

}
