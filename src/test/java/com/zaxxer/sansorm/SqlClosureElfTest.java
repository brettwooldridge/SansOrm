package com.zaxxer.sansorm;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class SqlClosureElfTest {
   @Test
   public void getInClausePlaceholdersByItems()
   {
      assertThat(SqlClosureElf.getInClausePlaceholders()).isEqualTo(" ('s0me n0n-ex1st4nt v4luu') ");
      assertThat(SqlClosureElf.getInClausePlaceholders(0)).isEqualTo(" (?) ");
      assertThat(SqlClosureElf.getInClausePlaceholders("1")).isEqualTo(" (?) ");
      assertThat(SqlClosureElf.getInClausePlaceholders("1", "2", "3", "4", "5")).isEqualTo(" (?,?,?,?,?) ");
   }

   @Test
   public void getInClausePlaceholdersByCount()
   {
      assertThat(SqlClosureElf.getInClausePlaceholdersForCount(0)).isEqualTo(" ('s0me n0n-ex1st4nt v4luu') ");
      assertThat(SqlClosureElf.getInClausePlaceholdersForCount(1)).isEqualTo(" (?) ");
      assertThat(SqlClosureElf.getInClausePlaceholdersForCount(5)).isEqualTo(" (?,?,?,?,?) ");
      assertThatIllegalArgumentException().isThrownBy(() -> SqlClosureElf.getInClausePlaceholdersForCount(-1));
   }
}
