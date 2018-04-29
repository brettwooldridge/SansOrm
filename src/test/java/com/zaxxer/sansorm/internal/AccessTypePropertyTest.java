package com.zaxxer.sansorm.internal;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.persistence.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 28.04.18
 */
public class AccessTypePropertyTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void explicitPropertyAccess() throws IllegalAccessException {
      @Access(value = AccessType.PROPERTY)
      class Test {
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String value) {
            // To ensure property access
            this.field = value.toUpperCase();
         }
      }
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo field = introspected.getFieldColumnInfo("field");
      Test obj = new Test();
      field.setValue(obj, "changed");
      assertEquals("CHANGED", obj.getField());
   }

   @Test
   public void explicitPropertyAccessFieldWithoutAccessors() {
      @Access(value = AccessType.PROPERTY)
      class Test {
         private String field;
         private String fieldWithoutAccessors;

         public String getField() {
            return field;
         }

         public void setField(String value) {
            // To ensure property access
            this.field = value.toUpperCase();
         }
      }
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo field = introspected.getFieldColumnInfo("fieldWithoutAccessors");
      assertNotNull(field);
      field = introspected.getFieldColumnInfo("field");
      assertNotNull(field);
   }

   @Test
   public void inheritedPropertiesSameExplicitAccessType() throws IllegalAccessException {

      @MappedSuperclass @Access(value = AccessType.PROPERTY)
      class Test {
         private int id;

         public int getId() {
            return id;
         }

         public void setId(int id) {
            // To ensure property access
            this.id = ++id;
         }
      }

      @Access(value = AccessType.PROPERTY)
      class SubTest extends Test { }

      Introspected introspected = new Introspected(SubTest.class);
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertEquals(field.getClass(), PropertyInfo.class);
      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(2, obj.getId());
   }

   /**
    * "An access type for an individual entity class, mapped superclass, or embeddable class can be specified for that class independent of the default for the entity hierarchy" (JSR 317: JavaTM Persistence API, Version 2.0, Final Release, 2.3.2 Explicit Access Type)

    */
   @Test
   public void mixedExplicitAccessType() throws IllegalAccessException, InvocationTargetException {

      @MappedSuperclass @Access(value = AccessType.FIELD)
      class Test {
         private int id;

         public void setId(int id) {
            // To ensure field access (id must be not incremented when id field is set)
            this.id = ++id;
         }
      }

      @Access(value = AccessType.PROPERTY)
      class SubTest extends Test { }

      Introspected introspected = new Introspected(SubTest.class);
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertNotNull(field);
      assertEquals(field.getClass(), FieldInfo.class);

      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(1, field.getValue(obj));
   }

   @Test
   public void overridenMethodSameExplicitAccessType() throws IllegalAccessException, InvocationTargetException {
      @MappedSuperclass @Access(value = AccessType.PROPERTY)
      class Test {
         private int id;

         public void setId(int id) {
            // Does nothing to ensure the overriding method was called
         }

         public int getId() {
            return id;
         }
      }

      @Access(value = AccessType.PROPERTY)
      class SubTest extends Test {
         public void setId(int id) {
            super.id = ++id;
         }
      }

      Introspected introspected = new Introspected(SubTest.class);
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertNotNull(field);
      assertEquals(field.getClass(), PropertyInfo.class);

      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(2, field.getValue(obj));
   }

   /**
    * See {@link #mixedExplicitAccessType()} ()}. id must be accessed directly.
    */
   @Test
   public void overridenMethodDifferentExplicitAccessTypes() throws IllegalAccessException, InvocationTargetException {
      @MappedSuperclass @Access(value = AccessType.FIELD)
      class Test {
         private int id;

         public void setId(int id) {
            this.id = 123;
         }

         public int getId() {
            return id;
         }
      }

      @Access(value = AccessType.PROPERTY)
      class SubTest extends Test {
         public void setId(int id) {
            super.id = 456;
         }
      }

      Introspected introspected = new Introspected(SubTest.class);
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertNotNull(field);
      assertEquals(field.getClass(), FieldInfo.class);

      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(1, field.getValue(obj));
   }

   @Test
   public void defaultPropertyAccess() {
      class Test {
         private String field;

         @Basic
         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getFieldColumnInfo("field");
      assertEquals(PropertyInfo.class, info.getClass());
   }

   @Test
   public void defaultFieldAccess() {
      class Test {
         @Basic
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, info.getClass());
   }

   /**
    * Neither property nor field access specified.
    */
   @Test
   public void fallbackAccess() {
      class Test {
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, info.getClass());
   }

   /**
    * "All such classes in the entity hierarchy whose access type is defaulted in this way must be consistent in their placement of annotations on either fields or properties, such that a single, consistent default access type applies within the hierarchy ... It is an error if a default access type cannot be determined and an access type is not explicitly specified by means of annotations or the XML descriptor. The behavior of applications that mix the placement of annotations on fields and properties within an entity hierarchy without explicitly specifying the Access annotation is undefined." (JSR 317: JavaTM Persistence API, Version 2.0, 2.3.1 Default Access Type)
    */
   @Test
   public void mixedDefaultAccessType() {

      @MappedSuperclass
      class Test {
         private int id;

         @Id
         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }

      class SubTest extends Test {
         @Basic
         private String field;
         private String field2;
      }

      Introspected introspected = new Introspected(SubTest.class);
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(PropertyInfo.class, idInfo.getClass());
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, fieldInfo.getClass());
   }

   /**
    * See {@link #ambiguousAccessType()}.
    */
   @Test
   public void mixedDefaultAccessType2() {

      @MappedSuperclass
      class Test {
         private int id;

         @Id
         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }

      class SubTest extends Test {
         @Basic
         private String field;
         private String field2;

         @Access(value = AccessType.PROPERTY)
         public String getField2() {
            return "property access";
         }

         public void setField2(String field2) {
            this.field2 = field2;
         }
      }

      Introspected introspected = new Introspected(SubTest.class);
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(PropertyInfo.class, idInfo.getClass());
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, fieldInfo.getClass());
      AttributeInfo field2Info = introspected.getFieldColumnInfo("field2");
      assertEquals(FieldInfo.class, field2Info.getClass());
   }

   /**
    * See {@link #mixedDefaultAccessType()}. When the access type of a single class is ambiguous SansOrm defaults to field access.
    */
   @Test
   public void ambiguousAccessType() {

      class Test {

         private int id;

         @Column
         private int field;

         public void setId(int id) {
            this.id = 123;
         }

         @Column
         public int getId() {
            return id;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, fieldInfo.getClass());
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(FieldInfo.class, idInfo.getClass());
   }

   @Test
   public void explicitAccessTypeWithFieldSpecificOne() throws IllegalAccessException, InvocationTargetException {

      @Access(value = AccessType.PROPERTY)
      class Test {

         @Access(value = AccessType.FIELD)
         private int id;
         private int field;

         public int getId() {
            // To ensure that id is got via property access
            return 456;
         }

         public void setId(int id) {
            // To ensure that id is set via property access
            this.id = 123;
         }

         public int getField() {
            return field;
         }

         public void setField(int field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(PropertyInfo.class, fieldInfo.getClass());
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(FieldInfo.class, idInfo.getClass());
      Test obj = new Test();
      idInfo.setValue(obj, 1);
      assertEquals(1, idInfo.getValue(obj));
   }

   @Test
   public void illegalAnnotationOnProperty() {
      @Access(value = AccessType.PROPERTY)
      class Test {
         private int id;

         @Access(value = AccessType.FIELD)
         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }
      thrown.expectMessage("A method can not be of access type field");
      Introspected introspected = new Introspected(Test.class);
   }

   @Test
   public void illegalAnnotationOnField() {
      @Access(value = AccessType.FIELD)
      class Test {
         @Access(value = AccessType.PROPERTY)
         private int id;

         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }
      thrown.expectMessage("A field can not be of access type property");
      Introspected introspected = new Introspected(Test.class);
   }

   /**
    * With IntelliJ from database schema reverse engineered Entity. Associations must still be commected out or Introspection throws errors.
    */
   @Test
   public void annotatedGetters() throws IllegalAccessException {
      Introspected introspected = new Introspected(GetterAnnotatedPitMainEntity.class);
      AttributeInfo[] selectableFcInfos = introspected
         .getSelectableFcInfos();
      Assertions.assertThat(selectableFcInfos).hasSize(8);
      Assertions.assertThat(selectableFcInfos).allMatch(attributeInfo -> attributeInfo.getClass() == PropertyInfo.class);
      GetterAnnotatedPitMainEntity entity = new GetterAnnotatedPitMainEntity();
      AttributeInfo pitType = introspected.getFieldColumnInfo("PIT_TYPE");
      pitType.setValue(entity, "changed");
      assertEquals("changed", entity.getPitType());

   }

   @Test
   public void propertyChangeSupport() throws IllegalAccessException {
      class Test {
         private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
         private int id;

         public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
         }

         public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.removePropertyChangeListener(listener);
         }

         @Id
         public int getId() {
            return id;
         }

         public void setId(int id) {
            int old = this.id;
            this.id = id;
            pcs.firePropertyChange("id", old, this.id);
         }
      }
      Test obj = new Test();
      final boolean[] called = new boolean[1];
      PropertyChangeListener listener = evt -> {
         called[0] = true;
      };
      obj.addPropertyChangeListener(listener);
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      idInfo.setValue(obj, 1);
      assertTrue(called[0]);
   }
}
