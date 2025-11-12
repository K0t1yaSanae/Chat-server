// server/src/main/java/net/kotiyasanae/chatserver/ChatServer.java
package net.kotiyasanae.chatserver;

import net.kotiyasanae.chatserver.websocket.ChatWebSocketHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);

    private Server server;
    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        // 配置ServletContextHandler
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        // 设置静态资源处理（客户端文件）
        //String webDir = this.getClass().getClassLoader().getResource("webapp").toExternalForm();
        //context.setResourceBase(webDir);
        context.addServlet(new ServletHolder("default", new org.eclipse.jetty.servlet.DefaultServlet()), "/");

        server.setHandler(context);

        // 配置WebSocket - 修正后的代码
        WebSocketUpgradeFilter wsFilter = WebSocketUpgradeFilter.configureContext(context);

        // 使用WebSocketCreator
        WebSocketCreator creator = (req, resp) -> new ChatWebSocketHandler();
        wsFilter.addMapping("/chat", creator);

        server.start();

        logger.info("Chat server started on port: {}", port);
        logger.info("Visit http://localhost:{}/ to enter the chat room", port);
        logger.info("WebSocket point: ws://localhost:{}/chat", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                logger.error("Server Close Error", e);
            }
        }));
    }

    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            logger.info("Chat server stopped");
        }
    }

    public static void main(String[] args) {
        int port = 9000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Invalid port number, using default port 8080");
            }
        }

        ChatServer chatServer = new ChatServer(port);
        try {
            chatServer.start();
            chatServer.server.join();
        } catch (Exception e) {
            logger.error("Server start failed", e);
            System.exit(1);
        }
    }
}