package com.sage.bif.diary.entity.converter;

import jakarta.persistence.AttributeConverter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.symmetric.AES;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.parameters.P;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
public class DiaryContentEncryptConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final String DIARY_SALT = "-diary-encryption-salt";

    @Value("${jwt.secret}")
    private String jwtSecret;

    private String getAesKey(){
        try {
            String combined = jwtSecret + DIARY_SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0,32);
        } catch (Exception e) {
            log.error("AES키 생성 실패");
            return jwtSecret.length()>=32?
                    jwtSecret.substring(0,32):(jwtSecret + "00000000000000000000000000000000").substring(0,32);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if(attribute == null || attribute.isEmpty()){
            return attribute;
        }
        try {
            String aesKey = getAesKey();
            SecretKeySpec keySpec = new SecretKeySpec(
                    aesKey.getBytes(StandardCharsets.UTF_8),ALGORITHM
            );
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH*8,iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, parameterSpec);

            byte[] encryptedText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedWithIv = new byte[iv.length + encryptedText.length];

            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(encryptedText, 0, encryptedWithIv, iv.length, encryptedText.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            log.error("일기 내용 암호화 실패", e);
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if(dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        try {
            byte[] decodedText = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decodedText,0,iv,0,iv.length);

            byte[] encrypted = new byte[decodedText.length-GCM_IV_LENGTH];
            System.arraycopy(decodedText,GCM_IV_LENGTH,encrypted,0,encrypted.length);

            String aesKey = getAesKey();
            SecretKeySpec keySpec = new SecretKeySpec(
                    aesKey.getBytes(StandardCharsets.UTF_8),ALGORITHM
            );

            Cipher cipher= Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH*8,iv);
            cipher.init(Cipher.DECRYPT_MODE,keySpec,parameterSpec);

            byte[] decryptedText = cipher.doFinal(encrypted);

            return new String(decryptedText,StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("일기 내용 복호화 실패",e);
            return dbData;
        }
    }

}
