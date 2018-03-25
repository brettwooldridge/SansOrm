package com.zaxxer.sansorm.internal;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.03.18
 */
@Table(name = "TEST_CLASS")
public class CaseSensitiveDatabasesClass {
   @Id
   private String id;
   @Column(name = "\"DELIMITED_FIELD_NAME\"")
   private String delimitedFieldName;
   @Column(name = "DEFAULT_CASE")
   private String defaultCase;

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
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
