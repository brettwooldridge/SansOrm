package com.zaxxer.sansorm.internal;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.03.18
 */
@Table(name = "Test_Class")
public class CaseSensitiveDatabasesClass {
   @Id
   private String Id;
   @Column(name = "\"Delimited Field Name\"")
   private String delimitedFieldName;
   @Column(name = "Default_Case")
   private String defaultCase;

   public String getId() {
      return Id;
   }

   public void setId(String id) {
      this.Id = id;
   }

   public String getDelimitedFieldName() {
      return delimitedFieldName;
   }

   public void setDelimitedFieldName(String value) {
      this.delimitedFieldName = value;
   }

   public String getDefaultCase() {
      return defaultCase;
   }

   public void setDefaultCase(String value) {
      this.defaultCase = value;
   }
}
