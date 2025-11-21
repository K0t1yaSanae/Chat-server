package net.kotiyasanae.chatserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kotiyasanae.chatserver.ChatServer;
import net.kotiyasanae.chatserver.encryption.AESEncryption;
import net.kotiyasanae.chatserver.model.Message;
import net.kotiyasanae.chatserver.util.Calculator;
import net.kotiyasanae.chatserver.util.Jokes;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);
    private static final Map<Session, String> userSessions = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final CommandService commandService;
    private final AESEncryption encryptionService;

    public ChatService() {
        this.commandService = new CommandService(userSessions);
        this.encryptionService = new AESEncryption();
    }

    public void handleConnect(Session session) throws IOException {
        logger.info("New connection: " + session.getRemoteAddress().getAddress());

        Message welcomeMsg = new Message(Message.MessageType.SYSTEM,
                "欢迎来到加密聊天室! 输入 .help 查看可用命令\n" +
                        "使用 .setkey [密钥] 设置解密密钥来查看加密消息", "系统");
        sendMessage(session, welcomeMsg);
    }

    public void handleDisconnect(Session session) {
        String username = userSessions.get(session);
        if (username != null) {
            userSessions.remove(session);
            // 清理用户的加密密钥
            encryptionService.cleanupUser(username);

            Message leaveMsg = new Message(Message.MessageType.LEAVE,
                    username + " 离开了聊天室", "系统");
            broadcastMessage(leaveMsg); // 这里不需要排除自己，因为已经断开连接了

            logger.info(username + " left the chat");
            updateOnlineUsers();
        }
    }

    public void handleMessage(Session session, String messageStr) throws IOException {
        try {
            Message message = mapper.readValue(messageStr, Message.class);

            switch (message.getType()) {
                case JOIN:
                    handleJoin(session, message);
                    break;
                case CHAT:
                    handleChat(session, message);
                    break;
                default:
                    logger.error("Unknown message type: " + message.getType());
            }
        } catch (JsonProcessingException e) {
            logger.error("Message parsing error " + e.getMessage());
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "消息格式错误", "系统");
            sendMessage(session, errorMsg);
        }
    }

    private void handleJoin(Session session, Message message) throws IOException {
        String username = message.getSender().trim();

        if (username.isEmpty() || username.length() > 20) {
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "用户名不能为空且不能超过20个字符", "系统");
            sendMessage(session, errorMsg);
            return;
        }

        if (userSessions.containsValue(username)) {
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "用户名已存在，请选择其他用户名", "系统");
            sendMessage(session, errorMsg);
            return;
        }

        userSessions.put(session, username);

        Message joinSuccess = new Message(Message.MessageType.SYSTEM,
                "欢迎 " + username + " 加入加密聊天室!\n" +
                        "请使用 .setkey [密钥] 设置解密密钥来查看加密消息", "系统");
        sendMessage(session, joinSuccess);

        Message joinMsg = new Message(Message.MessageType.JOIN,
                username + " 加入了聊天室", "系统");
        broadcastMessage(joinMsg); // 广播给所有人，包括自己

        updateOnlineUsers();
        logger.info(username + " joined the chat, online users: " + userSessions.size());
    }

    private void handleChat(Session session, Message message) throws IOException {
        String username = userSessions.get(session);
        if (username != null && message.getContent() != null && !message.getContent().trim().isEmpty()) {
            String content = message.getContent().trim();

            // 检查是否是笑话命令
            if (content.equals(".joke")) {
                String joke = Jokes.getRandomJoke();
                Message jokeMsg = new Message(Message.MessageType.CHAT, joke, "木柜子笑话");
                sendMessage(session, jokeMsg);
                return;
            }

            // 在 handleChat 方法中
            if (content.startsWith(".calc ")) {
                String expression = content.substring(".calc ".length()).trim();
                String result = Calculator.calculate(expression);
                Message calcMsg = new Message(Message.MessageType.CHAT, result, "计算器");
                sendMessage(session, calcMsg);
                return;
            }

            if (content.equals(".math")) {
                String problem = Calculator.generateRandomProblem();
                Message mathMsg = new Message(Message.MessageType.CHAT, problem, "数学挑战");
                sendMessage(session, mathMsg);
                return;
            }

            // 检查是否是命令
            if (content.startsWith(".")) {
                // 先检查是否是加密相关命令
                if (isEncryptionCommand(content)) {
                    // 交给加密服务处理加密相关命令
                    Message processedMessage = encryptionService.processMessage(message);
                    sendMessage(session, processedMessage);
                } else {
                    // 其他命令交给命令服务处理
                    boolean isCommand = commandService.handleCommand(session, message);
                    if (!isCommand) {
                        // 如果不是有效命令，发送错误消息
                        Message errorMsg = new Message(Message.MessageType.ERROR,
                                "未知命令，输入 .help 查看可用命令", "系统");
                        sendMessage(session, errorMsg);
                    }
                }
            } else {
                // 普通消息：进行加密
                Message encryptedMessage = encryptionService.processMessage(message);

                // 先给自己发送消息（确保自己能看见）
                sendMessage(session, encryptedMessage);

                // 然后广播给其他用户（排除自己）
                broadcastMessageToOthers(encryptedMessage, session);

                logger.info(username + " sent encrypted message");
            }
        }
    }

    // 更新isEncryptionCommand方法，添加笑话命令
    private boolean isEncryptionCommand(String content) {
        return content.startsWith(".setkey ") ||
                content.equals(".keystatus") ||
                content.equals(".clearkey") ||
                content.startsWith(".setserverkey ") ||
                content.startsWith(".decrypt ") ||
                content.startsWith(".encrypt ") ||
                content.equals(".joke"); // 添加笑话命令
    }

    /**
     * 广播消息给所有人（包括自己）
     */
    private void broadcastMessage(Message message) {
        userSessions.forEach((session, username) -> {
            if (session.isOpen()) {
                try {
                    // 根据用户密钥状态处理消息
                    Message processedMessage = encryptionService.processReceivedMessage(message, username);
                    session.getRemote().sendString(mapper.writeValueAsString(processedMessage));
                } catch (IOException e) {
                    logger.error("发送消息错误 - 用户: {}, 错误: {}", username, e.getMessage());
                    // 如果发送失败，可能是连接已断开，移除会话
                    if (!session.isOpen()) {
                        userSessions.remove(session);
                        encryptionService.cleanupUser(username);
                    }
                }
            }
        });
    }

    /**
     * 广播消息给其他用户（排除指定会话）
     */
    private void broadcastMessageToOthers(Message message, Session excludeSession) {
        userSessions.forEach((session, username) -> {
            if (session != excludeSession && session.isOpen()) {
                try {
                    // 根据用户密钥状态处理消息
                    Message processedMessage = encryptionService.processReceivedMessage(message, username);
                    session.getRemote().sendString(mapper.writeValueAsString(processedMessage));
                } catch (IOException e) {
                    logger.error("发送消息错误 - 用户: {}, 错误: {}", username, e.getMessage());
                    // 如果发送失败，可能是连接已断开，移除会话
                    if (!session.isOpen()) {
                        userSessions.remove(session);
                        encryptionService.cleanupUser(username);
                    }
                }
            }
        });
    }

    /**
     * 发送消息到单个会话 - 根据用户密钥状态自动解密
     */
    private void sendMessage(Session session, Message message) throws IOException {
        if (session.isOpen()) {
            String username = userSessions.get(session);
            Message processedMessage;

            if (username != null) {
                // 根据用户密钥状态处理消息
                processedMessage = encryptionService.processReceivedMessage(message, username);
            } else {
                // 用户还未登录，直接发送原始消息
                processedMessage = message;
            }

            session.getRemote().sendString(mapper.writeValueAsString(processedMessage));
        }
    }

    private void updateOnlineUsers() {
        String userList = String.join(", ", userSessions.values());

        // 创建系统消息
        Message userListMsg = new Message(Message.MessageType.SYSTEM,
                "当前在线用户 (" + userSessions.size() + "): " + userList, "系统");

        // 广播给所有人
        broadcastMessage(userListMsg);
    }

}