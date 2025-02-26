package com.project.chat;

import com.project.chat.UserService;

import javax.swing.*;
import java.awt.event.*;

public class RegisterFrame extends JFrame {
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
                dispose();  // Close the registration window
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed! Try again.");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
        }
    }

    public static void main(String[] args) {
        new RegisterFrame();
    }
}
