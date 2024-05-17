package com.systex.sysgateii.autosvr.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecryptUtil {
    
    private static final Logger log = LoggerFactory.getLogger(DecryptUtil.class);
    
    public static byte[] hexStringToByte(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
    
    public static boolean isHex(String s) {
        return s.matches("^[0-9A-Fa-f]+$");
    }
    
    public static String decryptPd(String encryptedPd) throws Exception {
        encryptedPd = encryptedPd.trim(); 
        if (encryptedPd.startsWith("\"") && encryptedPd.endsWith("\"")) {
            encryptedPd = encryptedPd.substring(1, encryptedPd.length() - 1);
        }
        
        if (!isHex(encryptedPd)) {
            return encryptedPd;
        }
        
        byte[] cipherText = hexStringToByte(encryptedPd);
        if (cipherText.length < 12) {
            throw new IllegalArgumentException("Invalid encrypted data");
        }

        byte[] ivBytes = new byte[12];
        System.arraycopy(cipherText, 0, ivBytes, 0, 12); 

        String pd = "96e6acb32405438332115d714ccf615f"; 
        String salt = "3214A882E4650D5C4CEC3DACF7727B548952CFB9B976BBDEC353E5D9A9F9A76D";
        int iterationCount = 100000;
        int keyLength = 256;
        
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(pd.toCharArray(), salt.getBytes(), iterationCount, keyLength);
        SecretKeySpec secretKeySpec = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, ivBytes);
        
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            byte[] original = cipher.doFinal(cipherText, 12, cipherText.length - 12); 
            return new String(original, "UTF-8");
        } catch (Exception e) {
            log.error("Error type: {}", e.getClass().getSimpleName());
            log.error("Encryption message：Length={}, pd={}", encryptedPd.length(), encryptedPd);
            return null;
        }
    }


//    public static void main(String[] args) {
//        try {
//            String encryptedPd = ""; 
//            String decryptedPd = decryptPd(encryptedPd);
//            System.out.println("解密後的密碼: " + decryptedPd);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//    
//    
}

