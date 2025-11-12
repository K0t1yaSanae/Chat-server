// server/src/main/java/com/chatroom/websocket/ChatWebSocketHandler.java
package net.kotiyasanae.chatserver.websocket;

import net.kotiyasanae.chatserver.service.ChatService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class ChatWebSocketHandler {
    private final ChatService chatService;

    public ChatWebSocketHandler() {
        this.chatService = new ChatService();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        chatService.handleConnect(session);
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        chatService.handleDisconnect(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws Exception {
        chatService.handleMessage(session, message);
    }
}