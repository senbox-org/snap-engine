package org.esa.snap.engine_utilities.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Created by jcoravu on 27/1/2020.
 */
public class CryptoUtils {

    private CryptoUtils() {
    }

    public static String encrypt(String textToEncrypt, String secretKey)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException {

        if (textToEncrypt != null && secretKey != null) {
            SecretKeySpec e = createKey(secretKey);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(1, e);
            return Base64.getEncoder().encodeToString(cipher.doFinal(textToEncrypt.getBytes("UTF-8")));
        }
        return null;
    }

    public static String decrypt(String textToDecrypt, String secretKey)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, InvalidKeyException {

        if (textToDecrypt != null && secretKey != null) {
            SecretKeySpec e = createKey(secretKey);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(2, e);
            return new String(cipher.doFinal(Base64.getDecoder().decode(textToDecrypt)));
        } else {
            return null;
        }
    }

    private static SecretKeySpec createKey(String secretKey) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        byte[] e = secretKey.getBytes("UTF-8");
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        e = sha.digest(e);
        e = Arrays.copyOf(e, 16);
        return new SecretKeySpec(e, "AES");
    }
}
