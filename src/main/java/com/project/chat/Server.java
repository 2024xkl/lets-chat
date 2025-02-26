package com.project.chat;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class Server {

    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static JTextArea logArea = new JTextArea();
    private static JLabel onlineUsersLabel = new JLabel("Online users: 0"); // 显示在线人数的标签
    private static final String MESSAGE_LOG_FILE = "chat_log.txt"; // 广播消息记录文件
    private static final String PRIVATE_MESSAGE_LOG_FILE = "private_message_log.txt"; // 私聊消息记录文件

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            logArea.setEditable(false);
            JFrame frame = new JFrame("Chat Server");
            frame.setSize(400, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // 添加 logArea 和 onlineUsersLabel 到 JFrame
            frame.setLayout(new BorderLayout());
            frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
            frame.add(onlineUsersLabel, BorderLayout.SOUTH); // 在线人数显示在底部

            frame.setVisible(true);

            log("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("New client connected: " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        logArea.append(message + "\n");
    }

    // 更新在线用户人数
    private static void updateOnlineUserCount() {
        SwingUtilities.invokeLater(() -> {
            onlineUsersLabel.setText("Online users: " + clientHandlers.size());
        });
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 获取用户名
                this.username = in.readLine();
                clientHandlers.add(this);
                log("User " + username + " joined.");

                // 更新在线用户人数
                updateOnlineUserCount();

                // 通知所有客户端在线用户
                sendOnlineUsers();

                String message;
                while ((message = in.readLine()) != null) {

                    if (message.contains(" to ")) {
                        handlePrivateMessage(message);
                    } else {
                        handleBroadcastMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientHandlers.remove(this);
                updateOnlineUserCount(); // 更新在线人数
                sendOnlineUsers();
                log(username + " disconnected.");
            }
        }

        private void handleBroadcastMessage(String message) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (clientHandler != this) {
                    clientHandler.out.println(username + ": " + message);
                }
            }
            log("BroadcastMessage message from "+username+": "+message);
            saveMessage(username + ": " + message, MESSAGE_LOG_FILE); // 保存广播消息到文件
        }

        private void handlePrivateMessage(String message) {
            // 拆分消息：先按 "to" 分割，然后按 ":" 分割消息内容
            String[] parts = message.split(" to ", 2);
            String senderUsername = parts[0].trim();  // 发送者
            String[] targetMessage = parts[1].split(": ", 2);  // 目标用户名和消息

            String targetUsername = targetMessage[0].trim();  // 目标用户名
            String privateMessage = targetMessage[1].trim();  // 消息内容

            log("Private message from " + senderUsername + " to " + targetUsername + ": " + privateMessage);  // 添加日志

            boolean userFound = false;
            synchronized (clientHandlers) {
                for (ClientHandler handler : clientHandlers) {
                    if (handler.username.equals(targetUsername)) {
                        handler.out.println(senderUsername + " to " + targetUsername + ": " + privateMessage); // 发送给目标用户
                        out.println(senderUsername + " to " + targetUsername + ": " + privateMessage); // 发送给发送者
                        saveMessage(senderUsername + " to " + targetUsername + ": " + privateMessage, PRIVATE_MESSAGE_LOG_FILE); // 保存私聊消息到文件
                        userFound = true;
                        break;
                    }
                }
            }

            if (!userFound) {
                out.println("User " + targetUsername + " not found.");
            }
        }

        private void sendOnlineUsers() {
            StringBuilder onlineUsers = new StringBuilder("Online users:");
            synchronized (clientHandlers) {
                for (ClientHandler clientHandler : clientHandlers) {
                    onlineUsers.append(" ").append(clientHandler.username);
                }
            }
            for (ClientHandler clientHandler : clientHandlers) {
                clientHandler.out.println(onlineUsers.toString());
            }
        }

        private void saveMessage(String message, String fileName) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
                writer.write(message); // 存储信息
                writer.newLine(); // 换行
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
