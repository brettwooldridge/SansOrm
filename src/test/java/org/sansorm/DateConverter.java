package org.sansorm;

import java.time.Instant;
import java.util.Date;

import javax.persistence.AttributeConverter;

public class DateConverter implements AttributeConverter<Date, Number> {
   @Override
   public Number convertToDatabaseColumn(Date value) {
      return value == null ? null : value.getTime();
   }

   @Override
   public Date convertToEntityAttribute(Number value) {
      return Date.from(Instant.ofEpochMilli(value.longValue()));
   }
}
