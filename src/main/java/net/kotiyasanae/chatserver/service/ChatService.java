package net.kotiyasanae.chatserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kotiyasanae.chatserver.ChatServer;
import net.kotiyasanae.chatserver.model.Message;
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

    public ChatService() {
        this.commandService = new CommandService(userSessions);
    }

    public void handleConnect(Session session) throws IOException {
        logger.info("New connection: " + session.getRemoteAddress().getAddress());

        Message welcomeMsg = new Message(Message.MessageType.SYSTEM,
                "欢迎来到聊天室! 输入 .help 查看可用命令", "系统");
        sendMessage(session, welcomeMsg);
    }

    public void handleDisconnect(Session session) {
        String username = userSessions.get(session);
        if (username != null) {
            userSessions.remove(session);

            Message leaveMsg = new Message(Message.MessageType.LEAVE,
                    username + " 离开了聊天室", "系统");
            broadcastMessage(leaveMsg, session);

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
                "欢迎 " + username + " 加入聊天室!", "系统");
        sendMessage(session, joinSuccess);

        Message joinMsg = new Message(Message.MessageType.JOIN,
                username + " 加入了聊天室", "系统");
        broadcastMessage(joinMsg, session);

        updateOnlineUsers();
        logger.info(username + " joined the chat, online users: " + userSessions.size());
    }

    private void handleChat(Session session, Message message) throws IOException {
        String username = userSessions.get(session);
        if (username != null && message.getContent() != null && !message.getContent().trim().isEmpty()) {
            String content = message.getContent().trim();

            // 检查是否是命令
            if (content.startsWith(".")) {
                // 创建命令消息副本
                Message commandMessage = new Message();
                commandMessage.setType(Message.MessageType.CHAT);
                commandMessage.setSender(username);
                commandMessage.setContent(content);
                commandMessage.setTimestamp(message.getTimestamp());

                // 交给命令服务处理
                boolean isCommand = commandService.handleCommand(session, commandMessage);
                if (isCommand) {
                    return; // 命令已处理，不广播普通消息
                }
            }

            // 普通聊天消息
            message.setSender(username);
            message.setContent(content);
            broadcastMessage(message, null);
            logger.info(username + " sent message: " + content);
        }
    }

    private void sendMessage(Session session, Message message) throws IOException {
        if (session.isOpen()) {
            session.getRemote().sendString(mapper.writeValueAsString(message));
        }
    }

    private void broadcastMessage(Message message, Session excludeSession) {
        userSessions.forEach((session, username) -> {
            if (session != excludeSession && session.isOpen()) {
                try {
                    session.getRemote().sendString(mapper.writeValueAsString(message));
                } catch (IOException e) {
                    logger.error("Send message error: " + e.getMessage());
                }
            }
        });
    }

    private void updateOnlineUsers() {
        String userList = String.join(", ", userSessions.values());

        // 发送系统消息
        Message userListMsg = new Message(Message.MessageType.SYSTEM,
                "当前在线用户 (" + userSessions.size() + "): " + userList, "系统");
        broadcastMessage(userListMsg, null);
    }

    public int getOnlineUsersCount() {
        return userSessions.size();
    }
}