package net.kotiyasanae.chatserver.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.kotiyasanae.chatserver.model.Message;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandService {
    private static final Logger logger = LoggerFactory.getLogger(CommandService.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Session, String> userSessions;

    public CommandService(Map<Session, String> userSessions) {
        this.userSessions = userSessions;
    }

    /**
     * 处理用户命令
     * @param session 用户会话
     * @param message 用户消息
     * @return 如果是命令返回true，否则返回false
     */
    public boolean handleCommand(Session session, Message message) throws IOException {
        String content = message.getContent().trim();
        String username = userSessions.get(session);

        if (!content.startsWith(".")) {
            return false; // 不是命令
        }

        String[] parts = content.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case ".ping":
                handlePingCommand(session, username);
                break;
            case ".help":
                handleHelpCommand(session, username);
                break;
            case ".users":
                handleUsersCommand(session, username);
                break;
            case ".time":
                handleTimeCommand(session, username);
                break;
            case ".clear":
                handleClearCommand(session, username);
                break;
            case ".me":
                handleMeCommand(session, username, argument);
                break;
            case ".msg":
                handlePrivateMessageCommand(session, username, argument);
                break;
            default:
                handleUnknownCommand(session, username, command);
                break;
        }

        return true;
    }

    /**
     * 处理 .ping 命令
     */
    private void handlePingCommand(Session session, String username) throws IOException {
        Message pongMsg = new Message(Message.MessageType.SYSTEM,
                "pong!", "系统");
        sendMessage(session, pongMsg);
        logger.info("用户 {} 执行了 .ping 命令", username);
    }

    /**
     * 处理 .help 命令 - 显示所有可用命令
     */
    private void handleHelpCommand(Session session, String username) throws IOException {
        String helpText =
                "可用命令:\n" +
                        ".ping - 测试服务器响应\n" +
                        ".help - 显示此帮助信息\n" +
                        ".users - 显示在线用户列表\n" +
                        ".time - 显示当前服务器时间\n" +
                        ".clear - 清空聊天记录\n" +
                        ".me <动作> - 发送动作消息\n" +
                        ".msg <用户名> <消息> - 发送私聊消息";

        Message helpMsg = new Message(Message.MessageType.SYSTEM,
                helpText, "系统");
        sendMessage(session, helpMsg);
        logger.info("用户 {} 执行了 .help 命令", username);
    }

    /**
     * 处理 .users 命令 - 显示在线用户
     */
    private void handleUsersCommand(Session session, String username) throws IOException {
        String userList = String.join(", ", userSessions.values());
        String usersText = "当前在线用户 (" + userSessions.size() + "): " + userList;

        Message usersMsg = new Message(Message.MessageType.SYSTEM,
                usersText, "系统");
        sendMessage(session, usersMsg);
        logger.info("用户 {} 执行了 .users 命令", username);
    }

    /**
     * 处理 .time 命令 - 显示服务器时间
     */
    private void handleTimeCommand(Session session, String username) throws IOException {
        String currentTime = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String timeText = "服务器当前时间: " + currentTime;

        Message timeMsg = new Message(Message.MessageType.SYSTEM,
                timeText, "系统");
        sendMessage(session, timeMsg);
        logger.info("用户 {} 执行了 .time 命令", username);
    }

    /**
     * 处理 .clear 命令 - 清空聊天记录（客户端功能）
     */
    private void handleClearCommand(Session session, String username) throws IOException {
        Message clearMsg = new Message(Message.MessageType.SYSTEM,
                "CLEAR_CHAT", "系统"); // 特殊标记，客户端识别后清屏
        sendMessage(session, clearMsg);
        logger.info("用户 {} 执行了 .clear 命令", username);
    }

    /**
     * 处理 .me 命令 - 发送动作消息
     */
    private void handleMeCommand(Session session, String username, String action) throws IOException {
        if (action.isEmpty()) {
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "用法: .me <动作>，例如: .me 正在喝茶", "系统");
            sendMessage(session, errorMsg);
            return;
        }

        String actionText = username + " " + action;
        Message actionMsg = new Message(Message.MessageType.SYSTEM,
                actionText, "系统");
        broadcastMessage(actionMsg, null);
        logger.info("用户 {} 执行了动作: {}", username, action);
    }

    /**
     * 处理 .msg 命令 - 发送私聊消息
     */
    private void handlePrivateMessageCommand(Session session, String username, String argument) throws IOException {
        String[] parts = argument.split("\\s+", 2);
        if (parts.length < 2) {
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "用法: .msg <用户名> <消息>，例如: .msg Alice 你好", "系统");
            sendMessage(session, errorMsg);
            return;
        }

        String targetUser = parts[0];
        String privateMessage = parts[1];

        // 查找目标用户
        Session targetSession = findSessionByUsername(targetUser);
        if (targetSession == null) {
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "用户 " + targetUser + " 不在线或不存在", "系统");
            sendMessage(session, errorMsg);
            return;
        }

        if (targetSession.equals(session)) {
            Message errorMsg = new Message(Message.MessageType.ERROR,
                    "不能给自己发送私聊消息", "系统");
            sendMessage(session, errorMsg);
            return;
        }

        // 发送私聊消息给目标用户
        String toTargetMsg = "[私聊] " + username + " 对你说: " + privateMessage;
        Message privateMsgToTarget = new Message(Message.MessageType.SYSTEM,
                toTargetMsg, "系统");
        sendMessage(targetSession, privateMsgToTarget);

        // 发送确认消息给发送者
        String toSenderMsg = "[私聊] 你对 " + targetUser + " 说: " + privateMessage;
        Message privateMsgToSender = new Message(Message.MessageType.SYSTEM,
                toSenderMsg, "系统");
        sendMessage(session, privateMsgToSender);

        logger.info("用户 {} 向 {} 发送私聊消息: {}", username, targetUser, privateMessage);
    }

    /**
     * 处理未知命令
     */
    private void handleUnknownCommand(Session session, String username, String command) throws IOException {
        Message errorMsg = new Message(Message.MessageType.ERROR,
                "未知命令: " + command + "，输入 .help 查看可用命令", "系统");
        sendMessage(session, errorMsg);
        logger.info("用户 {} 尝试了未知命令: {}", username, command);
    }

    /**
     * 根据用户名查找会话
     */
    private Session findSessionByUsername(String username) {
        for (Map.Entry<Session, String> entry : userSessions.entrySet()) {
            if (entry.getValue().equals(username) && entry.getKey().isOpen()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(Session session, Message message) throws IOException {
        if (session.isOpen()) {
            session.getRemote().sendString(mapper.writeValueAsString(message));
        }
    }

    /**
     * 广播消息给所有用户
     */
    private void broadcastMessage(Message message, Session excludeSession) {
        userSessions.forEach((session, username) -> {
            if (session != excludeSession && session.isOpen()) {
                try {
                    session.getRemote().sendString(mapper.writeValueAsString(message));
                } catch (IOException e) {
                    logger.error("发送消息错误: " + e.getMessage());
                }
            }
        });
    }
}