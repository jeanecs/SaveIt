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

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT id, username, password FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                int userId = rs.getInt("id");
                String retrievedUsername = rs.getString("username"); // Get the username


                if (dbPassword.equals(password)) {
                    messageLabel.setText("Login successful!");

                    // Open dashboard and pass userId
                    openDashboard(event, userId, retrievedUsername);

                } else {
                    messageLabel.setText("Invalid username or password.");
                }

            } else {
                messageLabel.setText("User not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error. Please try again.");
        }
    }

    private void openDashboard(ActionEvent event, int userId, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/main_layout.fxml"));
            Parent root = loader.load();

            // Get MainLayoutController, not DashboardController
            MainLayoutController controller = loader.getController();
            controller.setLoggedInUserId(userId); // this will also trigger showDashboard() in initialize()
            controller.setUsername(username); // Set the username in the main layout controller

            if (username != null && !username.isEmpty()) {
                controller.setProfileInitial(username.substring(0, 1));
            }

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SaveIT - Dashboard");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Failed to load dashboard.");
        }
    }


    @FXML
    private void handleRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SaveIT - Register");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Error loading register page.");
        }
    }
}
