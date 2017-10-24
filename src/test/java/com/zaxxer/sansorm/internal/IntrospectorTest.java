package com.zaxxer.sansorm.internal;

import org.junit.Test;
import org.sansorm.TargetClass1;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class IntrospectorTest
{
   @Test
   public void shouldCacheClassMeta()
   {
      Introspected is1 = Introspector.getIntrospected(TargetClass1.class);
      Introspected is2 = Introspector.getIntrospected(TargetClass1.class);
      assertNotNull(is1);
      assertSame(is1, is2);
   }
}
