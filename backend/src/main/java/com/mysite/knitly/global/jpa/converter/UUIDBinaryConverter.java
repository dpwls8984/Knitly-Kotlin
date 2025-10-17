package com.mysite.knitly.global.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

// UUID <-> BINARY(16)
@Converter(autoApply = false) // 필요한 필드에 @Convert 사용
public class UUIDBinaryConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID attribute) {
        if (attribute == null) return null;
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(attribute.getMostSignificantBits());
        bb.putLong(attribute.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public UUID convertToEntityAttribute(byte[] dbData) {
        if (dbData == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(dbData);
        long most = bb.getLong();
        long least = bb.getLong();
        return new UUID(most, least);
    }
}
