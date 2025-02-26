package com.project.chat;

import com.project.chat.UserService;

import javax.swing.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {
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
        registerButton = new JButton("Go to register!");

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
                // Proceed to main chat window
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
        }
    }

    private void openRegisterWindow() {
        new RegisterFrame();
        dispose();  // Close the login window
    }

    public static void main(String[] args) {
        new LoginFrame();
    }
}
