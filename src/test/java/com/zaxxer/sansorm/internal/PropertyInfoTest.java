package com.zaxxer.sansorm.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 28.04.18
 */
public class PropertyInfoTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void privateFieldWithGetterSetter() throws InvocationTargetException, IllegalAccessException {
      class Test {
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String value) {
            this.field = value;
         }
      }
      Field[] declaredFields = Test.class.getDeclaredFields();
      PropertyInfo propertyAccessor = new PropertyInfo(declaredFields[0], Test.class);
      assertTrue(propertyAccessor.isToBeConsidered());
      assertEquals("field", propertyAccessor.getName());
      Test target = new Test();
      assertEquals(null, propertyAccessor.getValue(target));
      target.setField("test");
      assertEquals("test", propertyAccessor.getValue(target));
      propertyAccessor.setValue(target, null);
      assertEquals(null, target.getField());

      FieldInfo fieldAccessor = new FieldInfo(declaredFields[0], Test.class);
      assertTrue(fieldAccessor.isToBeConsidered());
      assertEquals("field", fieldAccessor.getName());
//      thrown.expectMessage("FieldInfo can not access a member of class com.zaxxer.sansorm.internal.PropertyInfoTest$2Test with modifiers \"private\"");
      assertEquals(null, fieldAccessor.getValue(target));


   }
}
