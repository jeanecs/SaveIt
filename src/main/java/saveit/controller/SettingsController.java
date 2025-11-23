package saveit.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField; // Import PasswordField
import saveit.model.DatabaseManager;
import saveit.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsController {

    @FXML private TextField fullNameField;
    @FXML private TextField emailField;

    // FXML FIELDS for Password Change
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;

    @FXML private Label messageLabel;

    private int loggedInUserId;
    private User currentUser;

    public void setUserId(int userId) {
        this.loggedInUserId = userId;
        loadUserSettings();
    }

    @FXML
    public void initialize() {
        // Initialization logic if needed
    }

    /**
     * Loads the user's current profile settings (Username, Email, and Password) from the database.
     * The password is required in-memory to verify the 'Current Password' field before allowing an update.
     */
    private void loadUserSettings() {
        if (loggedInUserId == 0) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            // FIX: Added 'password' to the SELECT clause to fetch it for verification.
            String sql = "SELECT username, email, password FROM users WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, loggedInUserId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String username = rs.getString("username");
                String email = rs.getString("email");
                String password = rs.getString("password");

                // FIX: Store the user data including the current raw password (for old password verification)
                currentUser = new User(loggedInUserId, username, email, password);

                // Use username as the 'Full Name' field content, as requested
                fullNameField.setText(username);
                emailField.setText(email);

                // Security settings (like 2FA) are placeholders and not loaded from DB in this stub
            } else {
                messageLabel.setText("Error: User data not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Error loading user settings.");
        }
    }

    @FXML
    private void handleSaveProfile() {
        // Since you removed the separate Full Name field, we treat this as updating the username.
        String newUsername = fullNameField.getText().trim();
        String newEmail = emailField.getText().trim();

        if (newEmail.isEmpty() || newUsername.isEmpty()) {
            messageLabel.setText("Username and Email cannot be empty.");
            return;
        }

        // Basic email validation
        if (!newEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            // NOTE: In a real app, you must check if the new username/email already exists for another user.

            // FIX: Updated SQL to only set 'username' and 'email'.
            String updateSql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newUsername);
            stmt.setString(2, newEmail);
            stmt.setInt(3, loggedInUserId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                messageLabel.setText("Profile updated successfully!");
                // Optionally refresh the username/initial in the main layout header
                if (MainLayoutController.getInstance() != null) {
                    MainLayoutController.getInstance().setUsername(newUsername);
                    MainLayoutController.getInstance().setProfileInitial(newUsername.substring(0, 1));

                    // Update the in-memory user object
                    currentUser = new User(loggedInUserId, newUsername, newEmail, currentUser.getPassword());
                }
            } else {
                messageLabel.setText("Failed to update profile.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error during profile save.");
        }
    }


    /**
     * Handles the logic for updating the user's password.
     * Requires current password verification.
     */
    @FXML
    private void handleChangePassword() {
        String oldPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmNewPasswordField.getText();

        messageLabel.setText(""); // Clear previous messages

        // 1. Validate fields
        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Please fill in all password fields.");
            return;
        }

        // 2. Check if new passwords match
        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("New passwords do not match.");
            return;
        }

        // 3. Check if current password is correct
        // This check requires that 'currentUser' was loaded successfully with the password.
        if (currentUser == null || !oldPassword.equals(currentUser.getPassword())) {
            messageLabel.setText("The current password you entered is incorrect.");
            return;
        }

        // 4. Update the password in the database
        try (Connection conn = DatabaseManager.getConnection()) {
            // NOTE: Use hashing (e.g., bcrypt) in a real app!
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(updateSql);
            stmt.setString(1, newPassword);
            stmt.setInt(2, loggedInUserId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                messageLabel.setText("Password updated successfully! Please remember your new password.");

                // Clear the fields after a successful update
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmNewPasswordField.clear();

                // Since the password was changed, we reload user settings to update the
                // in-memory `currentUser` object's password for subsequent checks.
                // This is crucial to prevent the "old password is correct" state from lingering.
                loadUserSettings();

            } else {
                messageLabel.setText("Failed to update password.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error during password update.");
        }
    }

    // Toggle handlers are mostly placeholders for now

}