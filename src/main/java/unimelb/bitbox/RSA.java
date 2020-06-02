package unimelb.bitbox;

import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class RSA {

	private Cipher cipher;
	private String identity;

	public RSA() throws NoSuchAlgorithmException, NoSuchPaddingException {
		this.cipher = Cipher.getInstance("RSA");
	}

	public static byte[] toPKCS8Format(final PrivateKey privateKey) throws IOException
	{
		String keyFormat = privateKey.getFormat();
		if (keyFormat.equals("PKCS#1")) {
			final byte[] encoded = privateKey.getEncoded();
			final PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(encoded);
			final ASN1Encodable asn1Encodable = privateKeyInfo.parsePrivateKey();
			final ASN1Primitive asn1Primitive = asn1Encodable.toASN1Primitive();
			final byte[] privateKeyPKCS8Formatted = asn1Primitive.getEncoded(ASN1Encoding.DER);
			return privateKeyPKCS8Formatted;			
        }
		return privateKey.getEncoded();
	}
	
	public static byte[] toX509Format(final PublicKey publicKey) throws IOException
	{
		String keyFormat = publicKey.getFormat();
		if (keyFormat.equals("PKCS#1")) {
			final byte[] encoded = publicKey.getEncoded();
			final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(encoded);
			final ASN1Encodable asn1Encodable = publicKeyInfo.parsePublicKey();
			final ASN1Primitive asn1Primitive = asn1Encodable.toASN1Primitive();
			final byte[] publicKeyX509Formatted = asn1Primitive.getEncoded(ASN1Encoding.DER);
			return publicKeyX509Formatted;			
        }
		return publicKey.getEncoded();
	}

	public String encrypt(String msg, PublicKey key) throws UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		this.cipher.init(Cipher.ENCRYPT_MODE, key);
		return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}
	
	public String encrypt(String msg, String publicKey) throws NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, InvalidKeySpecException, IOException, NoSuchPaddingException {
		
		byte[] publicKeyBytes = publicKey.getBytes();
		RSAPublicKeySpec PublicKeySpec = RSAKeyConverter.decodeOpenSSH(publicKeyBytes);
		this.setIdentity(RSAKeyConverter.identity);
		
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PublicKey PublicKey = kf.generatePublic(PublicKeySpec);
		publicKeyBytes = toX509Format(PublicKey);
		PublicKey key = kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

		this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		this.cipher.init(Cipher.ENCRYPT_MODE, key);
		return Base64.getEncoder().encodeToString(cipher.doFinal(msg.getBytes("UTF-8")));
	}

	public String decrypt(String msg, PrivateKey key) throws InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        this.cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		this.cipher.init(Cipher.DECRYPT_MODE, key);
		return new String(cipher.doFinal(Base64.getDecoder().decode(msg)));
	}
	
	public PrivateKey getPrivate(String privateKeyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException
	{	
		KeyFactory kf = KeyFactory.getInstance("RSA");
		Security.addProvider(new BouncyCastleProvider());
		@SuppressWarnings("resource")
		PEMParser pemParser = new PEMParser(new FileReader(privateKeyFile));
		JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
		Object object = pemParser.readObject();
		KeyPair kp = converter.getKeyPair((PEMKeyPair) object);
		PrivateKey privateKey = kp.getPrivate();
		byte[] privateKeyBytes = toPKCS8Format(privateKey);
		return kf.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
	}
	
	public PublicKey getPublic(String publicKeyFile) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		KeyFactory kf = KeyFactory.getInstance("RSA");
		Path fileLocation = Paths.get(publicKeyFile);
		byte[] publicKeyBytes = Files.readAllBytes(fileLocation);
		RSAPublicKeySpec PublicKeySpec = RSAKeyConverter.decodeOpenSSH(publicKeyBytes);
		this.setIdentity(RSAKeyConverter.identity);
		PublicKey PublicKey = kf.generatePublic(PublicKeySpec);
		publicKeyBytes = toX509Format(PublicKey);
		return kf.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}
}