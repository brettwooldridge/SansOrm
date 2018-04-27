package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.lang.reflect.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.04.18
 */
public class FieldInfo extends AttributeInfo {

   public FieldInfo(Field field, Class clazz) {
      super(field, clazz);
      field.setAccessible(true);
   }

   protected void extractFieldName(Field accessibleObject) {
      this.name = accessibleObject.getName();
   }

   @Override
   protected Transient extractTransientAnnotation() {
      return field.getAnnotation(Transient.class);
   }

   @Override
   protected JoinColumn extractJoinColumnAnnotation() {
      return field.getAnnotation(JoinColumn.class);
   }

   @Override
   protected Enumerated extractEnumeratedAnnotation() {
      return field.getAnnotation(Enumerated.class);
   }

   @Override
   protected GeneratedValue extractGeneratedValueAnnotation() {
      return field.getAnnotation(GeneratedValue.class);
   }

   public Object getValue(Object target) throws IllegalAccessException {
      return field.get(target);
   }

   public void setValue(Object target, Object value) throws IllegalAccessException {
      field.set(target, value);
   }

   @Override
   protected Column extractColumnAnnotation() {
      return field.getAnnotation(Column.class);
   }

   @Override
   protected Id extractIdAnnotation() {
      return field.getAnnotation(Id.class);
   }

   @Override
   protected Convert extractConvertAnnotation() {
      return field.getAnnotation(Convert.class);
   }
}
