package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.util.Collection;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.04.18
 */
@Entity
@Table(name = "D_PIT_REFERENCE", schema = "dbo", catalog = "xxxxxxxxx")
public class GetterAnnotatedPitReferenceEntity {

   private int pirPitIdent;
   private int pirRefId;
   private int pirTnsTableId;
   private Collection<GetterAnnotatedPitMainEntity> notes;

   @Id @Basic @Column(name = "PIR_PIT_IDENT")
   public int getPirPitIdent() {
      return pirPitIdent;
   }

   public void setPirPitIdent(int pirPitIdent) {
      this.pirPitIdent = pirPitIdent;
   }

   @Basic
   @Column(name = "PIR_REF_ID")
   public int getPirRefId() {
      return pirRefId;
   }

   public void setPirRefId(int pirRefId) {
      this.pirRefId = pirRefId;
   }

   @Id @Basic @Column(name = "PIR_TNS_TABLE_ID")
   public int getPirTnsTableId() {
      return pirTnsTableId;
   }

   public void setPirTnsTableId(int pirTnsTableId) {
      this.pirTnsTableId = pirTnsTableId;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      GetterAnnotatedPitReferenceEntity that = (GetterAnnotatedPitReferenceEntity) o;

      if (pirPitIdent != that.pirPitIdent) {
         return false;
      }
      if (pirRefId != that.pirRefId) {
         return false;
      }
      if (pirTnsTableId != that.pirTnsTableId) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      int result = pirPitIdent;
      result = 31 * result + pirRefId;
      result = 31 * result + pirTnsTableId;
      return result;
   }

   @OneToMany(mappedBy = "pitReferenceByPitIdent")
   public Collection<GetterAnnotatedPitMainEntity> getNotes() {
      return notes;
   }

   public void setNotes(Collection<GetterAnnotatedPitMainEntity> notes) {
      this.notes = notes;
   }
}
