package com.zaxxer.sansorm.internal;

import javax.persistence.Column;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 28.04.18
 */
public class PropertyDescriptorTestClass {
   private String field;
   private boolean isBoolean;
   private boolean stored;
   int packagePrivate;
   protected String protectedProperty;



   @Column
   public String getField() {
      return field;
   }

   public void setField(String field) {
      this.field = field;
   }

   public boolean isBoolean() {
      return isBoolean;
   }

   public void setBoolean(boolean aBoolean) {
      isBoolean = aBoolean;
   }

   public boolean isStored() {
      return stored;
   }

   public void setStored(boolean stored) {
      this.stored = stored;
   }

   public int getPackagePrivate() {
      return packagePrivate;
   }

   public void setPackagePrivate(int packagePrivate) {
      this.packagePrivate = packagePrivate;
   }

   public String getProtectedProperty() {
      return protectedProperty;
   }

   public void setProtectedProperty(String protectedProperty) {
      this.protectedProperty = protectedProperty;
   }

   public void isNoSetter(String s) {

   }

   public void isNoSetterOrGetter() {

   }

   protected void isNoSetterProtected() {

   }
}
