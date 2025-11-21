package net.kotiyasanae.chatserver.encryption;

import net.kotiyasanae.chatserver.model.Message;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AESEncryption {

    private String outMessageContent;
    private static final String ALGORITHM = "AES";

    // 服务器预设的加密密钥
    private static String serverKey = "defaultserverkey123"; // 默认服务器密钥
    private static SecretKeySpec secretKey;

    // 存储用户的解密状态和密钥
    private static final Map<String, Boolean> userDecryptionStatus = new HashMap<>();
    private static final Map<String, String> userDecryptionKeys = new HashMap<>();

    static {
        setServerKey(serverKey); // 初始化使用默认密钥
    }

    /**
     * 设置服务器密钥（由管理员设置）
     */
    public static void setServerKey(String key) {
        try {
            serverKey = key;
            // 确保密钥长度为16字节
            byte[] keyBytes = new byte[16];
            byte[] originalKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(originalKeyBytes, 0, keyBytes, 0, Math.min(originalKeyBytes.length, keyBytes.length));
            secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("设置服务器密钥失败", e);
        }
    }

    public Message processMessage(Message inputMessage) {
        Message outMessage = new Message();

        // 复制基本信息
        outMessage.setSender(inputMessage.getSender());
        outMessage.setTimestamp(inputMessage.getTimestamp());
        outMessage.setType(inputMessage.getType());

        String originalContent = inputMessage.getContent();
        String processedContent;

        // 检查是否是设置解密密钥的命令
        if (originalContent != null && originalContent.startsWith(".setkey ")) {
            processedContent = handleSetKeyCommand(inputMessage.getSender(), originalContent);
        }
        // 检查是否是查看解密状态的命令
        else if (originalContent != null && originalContent.equals(".keystatus")) {
            processedContent = handleKeyStatusCommand(inputMessage.getSender());
        }
        // 检查是否是清除密钥的命令
        else if (originalContent != null && originalContent.equals(".clearkey")) {
            processedContent = handleClearKeyCommand(inputMessage.getSender());
        }
        // 检查是否是设置服务器密钥的命令（管理员）
        else if (originalContent != null && originalContent.startsWith(".setserverkey ")) {
            processedContent = handleSetServerKeyCommand(originalContent);
        }
        // 检查是否是手动解密命令
        else if (originalContent != null && originalContent.startsWith(".decrypt ")) {
            processedContent = handleManualDecryptCommand(inputMessage.getSender(), originalContent);
        }
        // 普通消息：自动加密
        else {
            processedContent = encryptMessage(originalContent);
        }

        outMessage.setContent(processedContent);
        outMessageContent = processedContent;

        return outMessage;
    }

    /**
     * 处理接收到的消息 - 根据用户密钥状态自动解密
     */
    public Message processReceivedMessage(Message receivedMessage, String username) {
        Message processedMessage = new Message();

        // 复制基本信息
        processedMessage.setSender(receivedMessage.getSender());
        processedMessage.setTimestamp(receivedMessage.getTimestamp());
        processedMessage.setType(receivedMessage.getType());

        String receivedContent = receivedMessage.getContent();
        String processedContent;

        // 检查是否是加密消息
        if (isEncryptedMessage(receivedContent)) {
            // 检查用户是否设置了正确地解密密钥
            if (hasValidDecryptionKey(username)) {
                // 自动解密
                processedContent = autoDecryptMessage(receivedContent);
            } else {
                // 提示用户设置解密密钥
                processedContent = receivedContent + " [需要设置解密密钥: 使用 .setkey 命令]";
            }
        } else {
            // 不是加密消息，直接显示
            processedContent = receivedContent;
        }

        processedMessage.setContent(processedContent);
        return processedMessage;
    }

    /**
     * 处理设置解密密钥命令: .setkey [密钥]
     */
    private String handleSetKeyCommand(String username, String content) {
        try {
            String userKey = content.substring(".setkey ".length()).trim();

            if (userKey.isEmpty()) {
                return "错误: 请输入解密密钥";
            }

            // 验证密钥是否正确
            if (validateUserKey(userKey)) {
                userDecryptionKeys.put(username, userKey);
                userDecryptionStatus.put(username, true);
                return "成功: 解密密钥已设置，现在可以自动解密消息了！";
            } else {
                userDecryptionStatus.put(username, false);
                return "错误: 解密密钥不正确，请向管理员获取正确的密钥";
            }

        } catch (Exception e) {
            return "错误: 设置密钥失败 - " + e.getMessage();
        }
    }

    /**
     * 处理查看密钥状态命令
     */
    private String handleKeyStatusCommand(String username) {
        boolean hasKey = userDecryptionKeys.containsKey(username);
        boolean keyValid = userDecryptionStatus.getOrDefault(username, false);

        String status;
        if (!hasKey) {
            status = "未设置解密密钥\n使用命令: .setkey [密钥] 来设置解密密钥";
        } else if (keyValid) {
            status = "解密密钥已设置且正确\n可以自动解密加密消息";
        } else {
            status = "解密密钥不正确\n请使用 .setkey [正确密钥] 重新设置";
        }

        return "解密状态:\n" + status;
    }

    /**
     * 处理清除密钥命令
     */
    private String handleClearKeyCommand(String username) {
        userDecryptionKeys.remove(username);
        userDecryptionStatus.remove(username);
        return "已清除解密密钥，加密消息将不再自动解密";
    }

    /**
     * 处理设置服务器密钥命令（管理员）
     */
    private String handleSetServerKeyCommand(String content) {
        try {
            String newKey = content.substring(".setserverkey ".length()).trim();

            if (newKey.length() < 8) {
                return "错误: 服务器密钥至少需要8个字符";
            }

            setServerKey(newKey);

            // 清除所有用户的解密状态（因为服务器密钥变了）
            userDecryptionStatus.clear();
            userDecryptionKeys.clear();

            return "成功: 服务器加密密钥已更新，所有用户需要重新设置解密密钥";

        } catch (Exception e) {
            return "错误: 设置服务器密钥失败 - " + e.getMessage();
        }
    }

    /**
     * 处理手动解密命令
     */
    private String handleManualDecryptCommand(String username, String content) {
        try {
            String encryptedText = content.substring(".decrypt ".length()).trim();

            // 检查用户是否设置了正确的密钥
            if (!hasValidDecryptionKey(username)) {
                return "错误: 请先使用 .setkey [密钥] 设置正确的解密密钥";
            }

            String decryptedText = decrypt(encryptedText);
            return "手动解密结果: " + decryptedText;

        } catch (Exception e) {
            return "错误: 解密失败 - 请检查密钥是否正确";
        }
    }

    /**
     * 自动加密消息内容（发送时）
     */
    private String encryptMessage(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        try {
            String encryptedContent = encrypt(content);
            return "[加密] " + encryptedContent;
        } catch (Exception e) {
            return "加密失败: " + content;
        }
    }

    /**
     * 自动解密消息内容（接收时）
     */
    private String autoDecryptMessage(String encryptedContent) {
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            return encryptedContent;
        }

        try {
            // 移除加密标识
            String pureEncryptedText = encryptedContent;
            if (encryptedContent.startsWith("[加密] ")) {
                pureEncryptedText = encryptedContent.substring("[加密] ".length());
            }

            String decryptedText = decrypt(pureEncryptedText);
            return  "[已解密] " + decryptedText;

        } catch (Exception e) {
            return encryptedContent + " [解密失败]";
        }
    }

    /**
     * 验证用户输入的密钥是否正确
     */
    private boolean validateUserKey(String userKey) {
        return userKey.equals(serverKey);
    }

    /**
     * 检查用户是否有有效地解密密钥
     */
    private boolean hasValidDecryptionKey(String username) {
        return userDecryptionStatus.getOrDefault(username, false);
    }

    /**
     * 判断消息是否是加密消息
     */
    private boolean isEncryptedMessage(String content) {
        return content != null && content.startsWith("[加密] ");
    }

    /**
     * AES加密方法
     */
    private String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * AES解密方法
     */
    private String decrypt(String encryptedText) throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Exception("解密失败");
        }
    }

    /**
     * 用户退出时清理密钥
     */
    public void cleanupUser(String username) {
        userDecryptionKeys.remove(username);
        userDecryptionStatus.remove(username);
    }

    public String getOutMessageContent() {
        return outMessageContent;
    }
}