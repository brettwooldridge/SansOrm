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
   protected OneToOne extractOneToOneAnnotation() {
      return field.getDeclaredAnnotation(OneToOne.class);
   }

   @Override
   protected ManyToOne extractManyToOneAnnotation() {
      return field.getDeclaredAnnotation(ManyToOne.class);
   }

   @Override
   protected ManyToMany extractManyToManyAnnotation() {
      return field.getDeclaredAnnotation(ManyToMany.class);
   }

   @Override
   protected OneToMany extractOneToManyAnnotation() {
      return field.getDeclaredAnnotation(OneToMany.class);
   }

   @Override
   protected JoinColumns extractJoinColumnsAnnotation() {
      return field.getDeclaredAnnotation(JoinColumns.class);
   }

   @Override
   protected Transient extractTransientAnnotation() {
      return field.getDeclaredAnnotation(Transient.class);
   }

   @Override
   protected JoinColumn extractJoinColumnAnnotation() {
      return field.getDeclaredAnnotation(JoinColumn.class);
   }

   @Override
   protected Enumerated extractEnumeratedAnnotation() {
      return field.getDeclaredAnnotation(Enumerated.class);
   }

   @Override
   protected GeneratedValue extractGeneratedValueAnnotation() {
      return field.getDeclaredAnnotation(GeneratedValue.class);
   }

   public Object getValue(Object target) throws IllegalAccessException {
      return field.get(target);
   }

   public void setValue(Object target, Object value) throws IllegalAccessException {
      field.set(target, value);
   }

   @Override
   protected Column extractColumnAnnotation() {
      return field.getDeclaredAnnotation(Column.class);
   }

   @Override
   protected Id extractIdAnnotation() {
      return field.getDeclaredAnnotation(Id.class);
   }

   @Override
   protected Convert extractConvertAnnotation() {
      return field.getDeclaredAnnotation(Convert.class);
   }
}
