package org.sansorm;

import org.junit.Assert;
import org.junit.Test;
import org.sansorm.internal.Introspected;
import org.sansorm.internal.Introspector;

public class IntrospectorTest
{
    @Test
    public void test()
    {
        Introspected is1 = Introspector.getIntrospected(TargetClass1.class);
        Assert.assertNotNull(is1);

        
    }
}
