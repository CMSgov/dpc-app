package gov.cms.dpc.api.converters;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.hibernate.MappingException;

import jakarta.persistence.AttributeConverter;
import java.io.IOException;

/**
 * {@link AttributeConverter} which stores a {@link SubjectPublicKeyInfo} as a {@link Byte} array.
 */
public class PublicKeyBytesConverter implements AttributeConverter<SubjectPublicKeyInfo, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn(SubjectPublicKeyInfo attribute) {
        try {
            return attribute.getEncoded();
        } catch (IOException e) {
            throw new MappingException("Cannot encode Public Key", e);
        }
    }

    @Override
    public SubjectPublicKeyInfo convertToEntityAttribute(byte[] dbData) {
        return SubjectPublicKeyInfo.getInstance(dbData);
    }
}
