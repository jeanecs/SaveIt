package saveit.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import saveit.model.DatabaseManager;

import java.sql.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField; // New field
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField; // New field
    @FXML private Label messageLabel;

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Basic validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        // Basic email format check (optional but recommended)
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 2. Check for uniqueness (username and email)
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                checkStmt.setString(2, email);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    messageLabel.setText("Username or Email already exists.");
                    conn.rollback(); // Rollback before exit
                    return;
                }
            }

            // 3. Insert new user
            String insertSql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, email);
                insertStmt.setString(3, password); // NOTE: Use hashing (e.g., bcrypt) in a real app!
                insertStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            messageLabel.setText("Registration successful! You can now log in.");

            // Transition back to the login page
            handleLogin(event);

        } catch (SQLException e) {
            // Handle specific unique constraint violation error if possible,
            // though the pre-check should prevent this.
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            messageLabel.setText("Registration failed: Database error.");

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SaveIT - Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading login page.");
        }
    }
}