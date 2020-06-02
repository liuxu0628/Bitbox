package unimelb.bitbox;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang.RandomStringUtils;
 
public class AES {
 
    private static SecretKeySpec secretKey;
    private static byte[] key;
 
    public static SecretKeySpec getKey(String myKey)
    {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes("UTF-8");
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return secretKey;
    }
 
    public static String Session_encrypt(String strToEncrypt, SecretKeySpec secretKey)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            int i = (int)(Math.random()*1000+1);
            String padding = RandomStringUtils.randomAlphabetic(i);
            strToEncrypt = strToEncrypt + "\n" + padding;
            byte [] encry = strToEncrypt.getBytes("UTF-8");
            while (encry.length % 16!=0){
                strToEncrypt += RandomStringUtils.randomAlphabetic(1);
                encry = strToEncrypt.getBytes("UTF-8");
            }
            String content = Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
            return content;
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
            return null;
        }
    }
 
    public static String Session_decrypt(String strToDecrypt, SecretKeySpec secretKey)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            String content = new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
            
            return content.split("\n")[0];
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
            return null;
        }
    }
}