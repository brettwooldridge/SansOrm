package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.04.18
 */
@Entity
@Table(name = "D_PIT_MAIN", schema = "dbo", catalog = "xxxxxxxxx")
public class GetterAnnotatedPitMainEntity {

   private int pitIdent;
   private Timestamp pitCrdate;
   private Timestamp pitChgDate;
   private String pitType;
   private String pitVisibility;
   private String pitNote;
   private String pitUser;
   private Timestamp pitRejectedDate;
//   private GetterAnnotatedPitMainEntity pitMainByPitIdent;
//   private Collection<GetterAnnotatedPitMainEntity> notes;
//   private GetterAnnotatedPitReferenceEntity pitReferenceByPitIdent;

   @Id @Basic @Column(name = "PIT_IDENT")
   public int getPitIdent() {
      return pitIdent;
   }

   public void setPitIdent(int pitIdent) {
      this.pitIdent = pitIdent;
   }

   @Basic
   @Column(name = "PIT_CRDATE")
   public Timestamp getPitCrdate() {
      return pitCrdate;
   }

   public void setPitCrdate(Timestamp pitCrdate) {
      this.pitCrdate = pitCrdate;
   }

   @Basic
   @Column(name = "PIT_CHG_DATE")
   public Timestamp getPitChgDate() {
      return pitChgDate;
   }

   public void setPitChgDate(Timestamp pitChgDate) {
      this.pitChgDate = pitChgDate;
   }

   @Basic
   @Column(name = "PIT_TYPE")
   public String getPitType() {
      return pitType;
   }

   public void setPitType(String pitType) {
      this.pitType = pitType;
   }

   @Basic
   @Column(name = "PIT_VISIBILITY")
   public String getPitVisibility() {
      return pitVisibility;
   }

   public void setPitVisibility(String pitVisibility) {
      this.pitVisibility = pitVisibility;
   }

   @Basic
   @Column(name = "PIT_NOTE")
   public String getPitNote() {
      return pitNote;
   }

   public void setPitNote(String pitNote) {
      this.pitNote = pitNote;
   }

   @Basic
   @Column(name = "PIT_USER")
   public String getPitUser() {
      return pitUser;
   }

   public void setPitUser(String pitUser) {
      this.pitUser = pitUser;
   }

   @Basic
   @Column(name = "PIT_REJECTED_DATE")
   public Timestamp getPitRejectedDate() {
      return pitRejectedDate;
   }

   public void setPitRejectedDate(Timestamp pitRejectedDate) {
      this.pitRejectedDate = pitRejectedDate;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }

      GetterAnnotatedPitMainEntity that = (GetterAnnotatedPitMainEntity) o;

      if (pitIdent != that.pitIdent) {
         return false;
      }
      if (pitCrdate != null ? !pitCrdate.equals(that.pitCrdate) : that.pitCrdate != null) {
         return false;
      }
      if (pitChgDate != null ? !pitChgDate.equals(that.pitChgDate) : that.pitChgDate != null) {
         return false;
      }
      if (pitType != null ? !pitType.equals(that.pitType) : that.pitType != null) {
         return false;
      }
      if (pitVisibility != null ? !pitVisibility.equals(that.pitVisibility) : that.pitVisibility != null) {
         return false;
      }
      if (pitNote != null ? !pitNote.equals(that.pitNote) : that.pitNote != null) {
         return false;
      }
      if (pitUser != null ? !pitUser.equals(that.pitUser) : that.pitUser != null) {
         return false;
      }
      if (pitRejectedDate != null ? !pitRejectedDate.equals(that.pitRejectedDate) : that.pitRejectedDate != null) {
         return false;
      }

      return true;
   }

   @Override
   public int hashCode() {
      int result = pitIdent;
      result = 31 * result + (pitCrdate != null ? pitCrdate.hashCode() : 0);
      result = 31 * result + (pitChgDate != null ? pitChgDate.hashCode() : 0);
      result = 31 * result + (pitType != null ? pitType.hashCode() : 0);
      result = 31 * result + (pitVisibility != null ? pitVisibility.hashCode() : 0);
      result = 31 * result + (pitNote != null ? pitNote.hashCode() : 0);
      result = 31 * result + (pitUser != null ? pitUser.hashCode() : 0);
      result = 31 * result + (pitRejectedDate != null ? pitRejectedDate.hashCode() : 0);
      return result;
   }

//   @ManyToOne
//   @JoinColumn(name = "PIT_IDENT", referencedColumnName = "PIT_PARENT_ID", nullable = false)
//   public GetterAnnotatedPitMainEntity getPitMainByPitIdent() {
//      return pitMainByPitIdent;
//   }
//
//   public void setPitMainByPitIdent(GetterAnnotatedPitMainEntity pitMainByPitIdent) {
//      this.pitMainByPitIdent = pitMainByPitIdent;
//   }
//
//   @OneToMany(mappedBy = "pitMainByPitIdent")
//   public Collection<GetterAnnotatedPitMainEntity> getNotes() {
//      return notes;
//   }
//
//   public void setNotes(Collection<GetterAnnotatedPitMainEntity> notes) {
//      this.notes = notes;
//   }
//
//   @ManyToOne
//   @JoinColumn(name = "PIT_IDENT", referencedColumnName = "PIR_PIT_IDENT", nullable = false)
//   public GetterAnnotatedPitReferenceEntity getPitReferenceByPitIdent() {
//      return pitReferenceByPitIdent;
//   }
//
//   public void setPitReferenceByPitIdent(GetterAnnotatedPitReferenceEntity pitReferenceByPitIdent) {
//      this.pitReferenceByPitIdent = pitReferenceByPitIdent;
//   }
}
