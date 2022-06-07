package com.systex.sysgateii.autosvr.util;

import com.systex.sysgateii.comm.util.DesTool;

/**
 * DES 小工具
 * 20210122 MatsudairaSyuMe
 * change to AES256 Algorithm
 */

public class Des {

	/**
	 * DES演算法，加密
	 *
	 * @param strToEncrypt 待加密字串
	 * @param secret  加密私鑰
	 * @return 加密後的位元組陣列，一般結合Base64編碼使用
	 * @throws CryptException 異常
	 */
	public static String encode(String secret, String strToEncrypt) throws Exception {
		//20220602 MatsudairaSyuMe
		return DesTool.encode(secret, strToEncrypt);
		//----
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
		//20220602 MatsudairaSyuMe
		return DesTool.decodeValue(secret, strToDecrypt);
		//----
	}
	//20210202 MatsudairaSyuMe
	//cut out main
	//----
}
