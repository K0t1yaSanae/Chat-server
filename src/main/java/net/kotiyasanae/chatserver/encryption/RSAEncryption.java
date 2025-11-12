package net.kotiyasanae.chatserver.encryption;

import net.kotiyasanae.chatserver.model.Message;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RSAEncryption {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final Base64.Encoder base64Encoder = Base64.getEncoder();
    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    public RSAEncryption() {
        generateKeyPair();
    }

    /**
     * 生成RSA密钥对
     */
    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA算法不支持", e);
        }
    }

    /**
     * 使用公钥加密消息
     */
    public Message encryptMessage(Message message) {
        if (message == null || message.getContent() == null) {
            return message;
        }

        try {
            String encryptedContent = encrypt(message.getContent(), publicKey);
            Message encryptedMessage = new Message();
            encryptedMessage.setType(message.getType());
            encryptedMessage.setSender(message.getSender());
            encryptedMessage.setTimestamp(message.getTimestamp());
            encryptedMessage.setRoom(message.getRoom());
            encryptedMessage.setContent("[RSA]" + encryptedContent);

            return encryptedMessage;
        } catch (Exception e) {
            System.err.println("RSA加密失败: " + e.getMessage());
            return message; // 加密失败返回原消息
        }
    }

    /**
     * 使用私钥解密消息
     */
    public Message decryptMessage(Message encryptedMessage) {
        if (encryptedMessage == null || encryptedMessage.getContent() == null) {
            return encryptedMessage;
        }

        String content = encryptedMessage.getContent();
        if (!content.startsWith("[RSA]")) {
            return encryptedMessage; // 不是RSA加密的消息
        }

        try {
            String encryptedContent = content.substring(5); // 去掉"[RSA]"前缀
            String decryptedContent = decrypt(encryptedContent, privateKey);

            Message decryptedMessage = new Message();
            decryptedMessage.setType(encryptedMessage.getType());
            decryptedMessage.setSender(encryptedMessage.getSender());
            decryptedMessage.setTimestamp(encryptedMessage.getTimestamp());
            decryptedMessage.setRoom(encryptedMessage.getRoom());
            decryptedMessage.setContent(decryptedContent);

            return decryptedMessage;
        } catch (Exception e) {
            System.err.println("RSA解密失败: " + e.getMessage());
            return encryptedMessage; // 解密失败返回原消息
        }
    }

    /**
     * RSA加密方法
     */
    private String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher encryptCipher = Cipher.getInstance("RSA");
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return base64Encoder.encodeToString(cipherText);
    }

    /**
     * RSA解密方法
     */
    private String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        byte[] bytes = base64Decoder.decode(cipherText);

        Cipher decryptCipher = Cipher.getInstance("RSA");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] decryptedBytes = decryptCipher.doFinal(bytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 获取公钥字符串（用于前端加密）
     */
    public String getPublicKeyString() {
        return base64Encoder.encodeToString(publicKey.getEncoded());
    }

    /**
     * 从字符串加载公钥
     */
    public void loadPublicKey(String publicKeyStr) throws Exception {
        byte[] publicBytes = base64Decoder.decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.publicKey = keyFactory.generatePublic(keySpec);
    }

    /**
     * 从字符串加载私钥
     */
    public void loadPrivateKey(String privateKeyStr) throws Exception {
        byte[] privateBytes = base64Decoder.decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        this.privateKey = keyFactory.generatePrivate(keySpec);
    }

    /**
     * 获取私钥字符串
     */
    public String getPrivateKeyString() {
        return base64Encoder.encodeToString(privateKey.getEncoded());
    }

}