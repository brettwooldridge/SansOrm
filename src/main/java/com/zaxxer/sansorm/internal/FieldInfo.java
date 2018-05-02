package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.lang.reflect.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.04.18
 */
public class FieldInfo extends AttributeInfo {

   public FieldInfo(final Field field, final Class clazz) {
      super(field, clazz);
      field.setAccessible(true);
   }

   protected void extractFieldName(final Field accessibleObject) {
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

   public Object getValue(final Object target) throws IllegalAccessException, InvocationTargetException {
      if (!isSelfJoinField()) {
         return field.get(target);
      }
      Object obj = field.get(target);
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
            field.set(target, value);
         }
         else {
            final Object obj = target.getClass().newInstance();
            final Introspected introspected = new Introspected(obj.getClass());
            final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
            generatedIdFcInfo.setValue(obj, value);
            field.set(target, obj);
         }
      }
      catch (InstantiationException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
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
