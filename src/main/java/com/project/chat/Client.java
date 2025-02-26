package com.project.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;



public class Client {
    private String username;
    private JLabel usernameLabel;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea messageArea;
    private JTextField messageField;
    private JList<String> onlineUsers;
    private DefaultListModel<String> usersListModel;
    private DefaultListModel<String> broadcastMessages;
    private DefaultListModel<String> privateMessages;

    private String targetUser = ""; // 初始化为空字符串

    //构造函数
    public Client(String username) {
        this.username = username;
    }
    //主入口函数
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());//开始一个新的窗口
    }

    // 启动聊天窗口
    public void startChatWindow() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane messageScroll = new JScrollPane(messageArea);

        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        // 用户列表
        usersListModel = new DefaultListModel<>();
        onlineUsers = new JList<>(usersListModel);
        onlineUsers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineUsers.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                targetUser = onlineUsers.getSelectedValue();
                displayMessages();
            }
        });

        // 设置JList的渲染器以高亮显示当前选中的私聊对象
        onlineUsers.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value.toString().equals(targetUser)) {
                    renderer.setBackground(Color.YELLOW); // 高亮显示
                }
                return renderer;
            }
        });

        // 新增按钮，点击后显示广播消息
        JButton broadcastButton = new JButton("Show Broadcast Messages");
        broadcastButton.addActionListener(e -> {
            targetUser = "";  // 清空目标用户
            displayMessages(); // 显示广播消息
        });

        frame.setLayout(new BorderLayout());

        // 顶部栏，包括“Logged in as”标签和广播消息按钮
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        usernameLabel = new JLabel("Logged in as: " + username);
        topPanel.add(usernameLabel, BorderLayout.WEST);
        topPanel.add(broadcastButton, BorderLayout.EAST);

        frame.add(topPanel, BorderLayout.NORTH);

        // 消息显示区域
        frame.add(messageScroll, BorderLayout.CENTER);
        frame.add(new JScrollPane(onlineUsers), BorderLayout.EAST);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        broadcastMessages = new DefaultListModel<>();
        privateMessages = new DefaultListModel<>();

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            new ReadMessagesThread().start();
            sendUsernameToServer(username);

            loadHistoryMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendUsernameToServer(String username) {
        try {
            out.println(username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText();//从文本框获取内容
        if (!message.isEmpty()) {
            if (targetUser != null && !targetUser.isEmpty()) {
                String messageToSend = username + " to " + targetUser + ": " + message;
                out.println(messageToSend);
                //privateMessages.addElement(username+" to " + targetUser + ": " + message);
                System.out.println("Message added: " + username + " to " + targetUser + ": " + message); // 打印消息确认x
            } else {
                out.println(message);
                broadcastMessages.addElement(username + ": " + message);
            }
            displayMessages();
            messageField.setText("");
        }
    }

    private void displayMessages() {
        SwingUtilities.invokeLater(() -> {
            messageArea.setText("");  // 清空当前消息区
            // 展示当前选择的消息（广播消息或私聊消息）
            if (targetUser != null && !targetUser.isEmpty()) {
                // 展示与目标用户的私聊消息
                for (int i = 0; i < privateMessages.getSize(); i++) {
                    String message = privateMessages.getElementAt(i);

                    // 只展示与目标用户相关的私聊消息
                    if (message.contains(username + " to " + targetUser) || message.contains(targetUser + " to " + username)) {
                        // 提取出发送者、目标用户和消息内容
                        String[] parts = message.split(" to ", 2);  // 按照 " to " 分割

                        String sender = parts[0].trim();  // 发送者
                        String[] messageParts = parts[1].split(": ", 2);  // 分割消息和内容

                        String target = messageParts[0].trim();  // 目标用户
                        String msgContent = messageParts[1].trim();  // 消息内容
                        messageArea.append(sender + ": " + msgContent + "\n");  // 格式化显示



                    }
                }
            }  else {
                // 展示所有广播消息
                for (int i = 0; i < broadcastMessages.getSize(); i++) {
                    messageArea.append(broadcastMessages.getElementAt(i) + "\n");
                }
            }
        });
    }

    private void loadHistoryMessages() {
        // 读取历史广播消息并展示
        try (BufferedReader reader = new BufferedReader(new FileReader("chat_log.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                broadcastMessages.addElement(line);  // 将广播消息添加到列表中
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 读取历史私聊消息并展示
        try (BufferedReader reader = new BufferedReader(new FileReader("private_message_log.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                privateMessages.addElement(line);  // 将私聊消息添加到列表中
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 显示所有消息
        displayMessages();
    }


    private class ReadMessagesThread extends Thread {
        public void run() {
            try {
                String message;

                //从客户端读入消息
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Online users:")) {
                        updateOnlineUsersList(message);
                    } else {
                        if (message.contains( " to ")) {
                            privateMessages.addElement(message);
                        } else {
                            broadcastMessages.addElement(message);
                        }
                        displayMessages();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateOnlineUsersList(String message) {
            String[] users = message.substring(13).split(" ");
            SwingUtilities.invokeLater(() -> {
                usersListModel.clear();
                for (String user : users) {
                    if (!user.trim().isEmpty()) {
                        usersListModel.addElement(user.trim());
                    }
                }
            });
        }
    }

    // 登录窗口
    public static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JButton loginButton;
        private JButton registerButton;

        public LoginFrame() {
            setTitle("Login");
            setSize(300, 200);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            usernameField = new JTextField(20);
            passwordField = new JPasswordField(20);
            loginButton = new JButton("Login");
            registerButton = new JButton("Register");

            loginButton.addActionListener(e -> loginUser());
            registerButton.addActionListener(e -> openRegisterWindow());

            JPanel panel = new JPanel();
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);
            panel.add(loginButton);
            panel.add(registerButton);

            add(panel);
            setVisible(true);
        }

        private void loginUser() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (!username.isEmpty() && !password.isEmpty()) {
                UserService userService = new UserService();
                if (userService.login(username, password)) {
                    JOptionPane.showMessageDialog(this, "Login successful!");
                    dispose();  // 关闭登录窗口
                    new Client(username).startChatWindow();  // 传递用户名并启动主聊天窗口
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            }
        }

        private void openRegisterWindow() {
            new RegisterFrame();
            dispose();  // 关闭登录窗口
        }
    }

    // 注册窗口
    public static class RegisterFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;
        private JButton registerButton;

        public RegisterFrame() {
            setTitle("Register");
            setSize(300, 200);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            usernameField = new JTextField(20);
            passwordField = new JPasswordField(20);
            registerButton = new JButton("Register");

            registerButton.addActionListener(e -> registerUser());

            JPanel panel = new JPanel();
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Password:"));
            panel.add(passwordField);
            panel.add(registerButton);

            add(panel);
            setVisible(true);
        }

        private void registerUser() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (!username.isEmpty() && !password.isEmpty()) {
                UserService userService = new UserService();
                if (userService.register(username, password)) {
                    JOptionPane.showMessageDialog(this, "Registration successful!");
                    dispose();  // 关闭注册窗口
                    new LoginFrame();  // 返回登录窗口
                } else {
                    JOptionPane.showMessageDialog(this, "Registration failed! Try again.");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            }
        }
    }
}
