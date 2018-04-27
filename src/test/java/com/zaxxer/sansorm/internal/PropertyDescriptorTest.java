package com.zaxxer.sansorm.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.persistence.Column;
import java.beans.*;
import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Bean Methoden müssen public sein.
 * Introspector liefert alle Methoden der Bean, die public sind, nicht nur getter und setter.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 28.04.18
 */
public class PropertyDescriptorTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void stringProperty() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
      PropertyDescriptor descriptor = new PropertyDescriptor("field", PropertyDescriptorTestClass.class);
      Method getter = descriptor.getReadMethod();
      assertEquals("public java.lang.String com.zaxxer.sansorm.internal.PropertyDescriptorTestClass.getField()", getter.toString());
      PropertyDescriptorTestClass obj = new PropertyDescriptorTestClass();
      obj.setField("value");
      assertEquals("value", getter.invoke(obj));
   }

   @Test
   public void nonSpecConformMethodSignature() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
      thrown.expectMessage("Method not found: isIsBoolean");
      PropertyDescriptor descriptor = new PropertyDescriptor("isBoolean", PropertyDescriptorTestClass.class);
   }

   @Test
   public void booleanProperty() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
      PropertyDescriptor descriptor = new PropertyDescriptor("stored", PropertyDescriptorTestClass.class);
      Method getter = descriptor.getReadMethod();
      PropertyDescriptorTestClass obj = new PropertyDescriptorTestClass();
      obj.setStored(true);
      assertEquals(true, getter.invoke(obj));
   }

   @Test
   public void packagePrivate() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
      PropertyDescriptor descriptor = new PropertyDescriptor("packagePrivate", PropertyDescriptorTestClass.class);
      Method getter = descriptor.getReadMethod();
      PropertyDescriptorTestClass obj = new PropertyDescriptorTestClass();
      obj.setPackagePrivate(1);
      assertEquals(1, getter.invoke(obj));
      assertTrue(descriptor.getPropertyType() == int.class);
   }

   @Test
   public void protectedProperty() throws IntrospectionException, InvocationTargetException, IllegalAccessException {
      PropertyDescriptor descriptor = new PropertyDescriptor("protectedProperty", PropertyDescriptorTestClass.class);
      Method getter = descriptor.getReadMethod();
      PropertyDescriptorTestClass obj = new PropertyDescriptorTestClass();
      obj.setProtectedProperty("y");
      assertEquals("y", getter.invoke(obj));
      assertTrue(descriptor.getPropertyType() == String.class);
   }

   @Test
   public void getMethodDescriptors() throws IntrospectionException {
      BeanInfo beanInfo = Introspector.getBeanInfo(PropertyDescriptorTestClass.class);
      MethodDescriptor[] methodDescriptors = beanInfo.getMethodDescriptors();
      Arrays.stream(methodDescriptors).forEach(descriptor -> {
         System.out.println(descriptor);
      });
   }

   /**
    * fields ohne Getter/Setter werden nicht geliefert
    */
   @Test
   public void getPropertyDescriptors() throws IntrospectionException {
      class Test {
         private String field;
      }
      BeanInfo beanInfo = Introspector.getBeanInfo(Test.class);
      PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
      assertEquals(1, descriptors.length);
      assertEquals("class", descriptors[0].getName());
   }
}
