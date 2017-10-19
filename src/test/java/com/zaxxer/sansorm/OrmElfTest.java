package com.zaxxer.sansorm;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OrmElfTest
{
   @Test
   public void getInClausePlaceholdersByItems()
   {
      assertEquals(" ('s0me n0n-ex1st4nt v4luu') ", OrmElf.getInClausePlaceholders());
      assertEquals(" (?) ", OrmElf.getInClausePlaceholders(0));
      assertEquals(" (?) ", OrmElf.getInClausePlaceholders("1"));
      assertEquals(" (?,?,?,?,?) ", OrmElf.getInClausePlaceholders("1", "2", "3", "4", "5"));
   }

   @Test
   public void getInClausePlaceholdersByCount()
   {
      assertEquals(" ('s0me n0n-ex1st4nt v4luu') ", OrmElf.getInClausePlaceholdersForCount(0));
      assertEquals(" (?) ", OrmElf.getInClausePlaceholdersForCount(1));
      assertEquals(" (?,?,?,?,?) ", OrmElf.getInClausePlaceholdersForCount(5));
      try
      {
         OrmElf.getInClausePlaceholdersForCount(-1);
         fail("Should not get here");
      }
      catch (IllegalArgumentException ignored)
      {
      }
   }
}
