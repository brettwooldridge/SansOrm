package org.sansorm;

import javax.persistence.AttributeConverter;

public class TestConverter implements AttributeConverter<String, Number> {

    @Override
    public Number convertToDatabaseColumn(String value) {
        try {
            return Integer.parseInt(value, 10);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String convertToEntityAttribute(Number value) {
        return value == null ? null : value.toString();

    }
}
