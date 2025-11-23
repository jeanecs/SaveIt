package saveit.controller;

import java.sql.Connection;
import saveit.model.DatabaseManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import java.io.IOException;

public class MainLayoutController {

    // Removed @FXML private Label welcomeLabel; (no longer in FXML)
    @FXML private StackPane contentArea;

    // FXML fields for sidebar buttons
    @FXML private Button dashboardButton;
    @FXML private Button addTransactionButton;
    @FXML private Button budgetButton;
    // Removed btnLogout (unused)
    @FXML private Button AllTransactionsButton;
    @FXML private Button settingsButton; // NEW FXML FIELD: Settings Button


    // FXML field for the profile initial (T)
    @FXML private Label profileInitialLabel;
    @FXML private Label usernameLabel;


    private Button currentActiveButton;
    private int loggedInUserId;

    private static MainLayoutController instance;
    private DashboardController dashboardController; // Reference for refreshing

    public static MainLayoutController getInstance() {
        return instance;
    }

    // Method to set the profile initial (optional, for personalization)
    public void setProfileInitial(String initial) {
        if (profileInitialLabel != null) {
            profileInitialLabel.setText(initial.toUpperCase());
        }
    }

    public void setUsername(String username) {
        if (usernameLabel != null) {
            usernameLabel.setText(username);
        }
    }


    // --- Initialization and User ID Setting ---

    public void setLoggedInUserId(int userId) {
        this.loggedInUserId = userId;

        // Ensure UI updates happen on the JavaFX Application Thread
        // AND after all FXML fields (like dashboardButton) are initialized.
        javafx.application.Platform.runLater(() -> {
            if (contentArea != null && dashboardButton != null) {
                // Set initial active state and load dashboard
                setActiveButton(dashboardButton);
                showDashboard(null);
            }
        });
    }

    @FXML
    public void initialize() {
        try {
            Connection conn = DatabaseManager.getConnection();
            System.out.println("Connected to: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            e.printStackTrace();
        }
        instance = this;
        // NOTE: We no longer call showDashboard() here. It's deferred to setLoggedInUserId.
    }

    // --- Active Button Logic ---

    /**
     * Handles setting the active style for the selected sidebar button.
     */
    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            // Remove 'active' and ensure 'secondary' is present on the previous button
            currentActiveButton.getStyleClass().remove("active");
            if (!currentActiveButton.getStyleClass().contains("secondary")) {
                currentActiveButton.getStyleClass().add("secondary");
            }
        }

        // Set the new button to active
        button.getStyleClass().remove("secondary");
        button.getStyleClass().add("active");
        currentActiveButton = button;
    }

    // --- Navigation Handlers ---

    // Overloaded method to allow calling from initialize or setLoggedInUserId without an event
    public void showDashboard() {
        showDashboard(null);
    }

    @FXML
    public void showDashboard(ActionEvent event) {
        loadPage("/FXML/dashboard.fxml");
        setActiveButton(dashboardButton);
    }

    public void showAllTransactions(ActionEvent event) {
        loadPage("/FXML/all_transactions.fxml");
        setActiveButton(AllTransactionsButton);
    }

    @FXML
    public void showAddTransaction(ActionEvent event) {
        loadPageWithUser("/FXML/add_transaction.fxml");
        setActiveButton(addTransactionButton);
    }


    @FXML
    public void showBudget(ActionEvent event) {
        loadPageWithUser("/FXML/budget.fxml");
        setActiveButton(budgetButton);
    }

    public void showSettings(ActionEvent event) {
        loadPageWithUser("/FXML/settings.fxml"); // NEW: Load the settings page
        setActiveButton(settingsButton);
    }


    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SaveIT - Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Page Loaders ---

    // Generic loader without user ID
    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    // Attempt to set userId on the content controller
                    controller.getClass().getMethod("setUserId", int.class).invoke(controller, loggedInUserId);
                } catch (NoSuchMethodException ignored) {}
                // Optionally store dashboard controller reference
                if (controller instanceof DashboardController) {
                    this.dashboardController = (DashboardController) controller;
                }
            }

            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Convenience method for controllers that need user ID
    private void loadPageWithUser(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass().getMethod("setUserId", int.class).invoke(controller, loggedInUserId);
                } catch (NoSuchMethodException ignored) {}
            }

            contentArea.getChildren().setAll(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Placeholder for refresh logic
    public void refreshDashboard() {
        if (dashboardController != null) {
            javafx.application.Platform.runLater(() -> dashboardController.loadDashboardData());
        }
    }
}