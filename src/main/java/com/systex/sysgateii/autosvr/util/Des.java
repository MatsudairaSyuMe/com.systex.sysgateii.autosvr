package com.systex.sysgateii.autosvr.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.util.Base64;


/**
 * DES 小工具
 * 20210122 MatsudairaSyuMe
 * change to AES256 Algorithm
 */

public class Des {
//	public static final String ALGORITHM_DES = "DES/CBC/PKCS5Padding";
	private static String salt = "ssshhhhhhhhhhh!!!!";
	public static final String ALGORITHM_AES256 = "PBKDF2WithHmacSHA256";
	public static final String ALGORITHM_AES = "AES";
	//public static final String ALGORITHM_AESCTRPAD = "AES/CTR/PKCS5PADDING";
	//public static final String ALGORITHM_AESCBCPAD = "AES/CBC/PKCS5PADDING";
	public static final String ALGORITHM_AESGCMNOPAD = "AES/GCM/NoPadding";
	private static final int TAG_LENGTH_BIT = 128; // must be one of {128, 120, 112, 104, 96}
	private static final int IV_LENGTH_BYTE = 12;
	private static final int SALT_LENGTH_BYTE = 16;
	private static final Charset UTF_8 = StandardCharsets.UTF_8;
	public static byte[] iv = new SecureRandom().generateSeed(16);

	public static byte[] getRandomNonce(int numBytes) {
		byte[] nonce = new byte[numBytes];
		new SecureRandom().nextBytes(nonce);
		return nonce;
	}
	// Password derived AES 256 bits secret key
	public static SecretKey getAESKeyFromPassword(char[] password, byte[] salt)
			throws NoSuchAlgorithmException, InvalidKeySpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_AES256);
		// iterationCount = 65536
		// keyLength = 256
		KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
		SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITHM_AES);
		return secret;
	}
	/**
	 * DES演算法，加密
	 *
	 * @param strToEncrypt 待加密字串
	 * @param secret  加密私鑰
	 * @return 加密後的位元組陣列，一般結合Base64編碼使用
	 * @throws CryptException 異常
	 */
	public static String encode(String secret, String strToEncrypt) throws Exception {
		//20210618
		//MatsudaairaSyuMe change to use AES GCM
		/*
		try {
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_AES256);
			KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM_AES);

			Cipher cipher = Cipher.getInstance(ALGORITHM_AESCBCPAD);
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivspec);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			System.out.println("Error while encrypting: " + e.toString());
		}*/
        // 16 bytes salt
		byte[] salt = getRandomNonce(SALT_LENGTH_BYTE);

		// GCM recommended 12 bytes iv?
		byte[] iv = getRandomNonce(IV_LENGTH_BYTE);

		// secret key from password
		SecretKey aesKeyFromPassword = getAESKeyFromPassword(secret.toCharArray(), salt);

		Cipher cipher = Cipher.getInstance(ALGORITHM_AESGCMNOPAD);

		// ASE-GCM needs GCMParameterSpec
		cipher.init(Cipher.ENCRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

		byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(UTF_8));

		// prefix IV and Salt to cipher text
		byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length).put(iv).put(salt)
				.put(cipherText).array();

		// string representation, base64, send this string to other for decryption.
		return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);
	}


	/**
	 * 獲取編碼後的值
	 * 
	 * @param secret
	 * @param strToDecrypt
	 * @return
	 * @throws Exception
	 * @throws Exception
	 */
	public static String decodeValue(String secret, String strToDecrypt) throws Exception {
		//20210618
		//MatsudaairaSyuMe change to use AES GCM
		/*
		try {
			IvParameterSpec ivspec = new IvParameterSpec(iv);

			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM_AES256);
			KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt.getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM_AES);

			Cipher cipher = Cipher.getInstance(ALGORITHM_AESCBCPAD);
			cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivspec);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			System.out.println("Error while decrypting: " + e.toString());
		}
		*/
		byte[] decode = Base64.getDecoder().decode(strToDecrypt.getBytes(UTF_8));

		// get back the iv and salt from the cipher text
		ByteBuffer bb = ByteBuffer.wrap(decode);

		byte[] iv = new byte[IV_LENGTH_BYTE];
		bb.get(iv);

		byte[] salt = new byte[SALT_LENGTH_BYTE];
		bb.get(salt);

		byte[] cipherText = new byte[bb.remaining()];
		bb.get(cipherText);

		// get back the aes key from the same password and salt
		SecretKey aesKeyFromPassword = getAESKeyFromPassword(secret.toCharArray(), salt);

		Cipher cipher = Cipher.getInstance(ALGORITHM_AESGCMNOPAD);

		cipher.init(Cipher.DECRYPT_MODE, aesKeyFromPassword, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

		byte[] plainText = cipher.doFinal(cipherText);

		return new String(plainText, UTF_8);
	}
	//20210202 MatsudairaSyuMe
	//cut out main
	//----
}
