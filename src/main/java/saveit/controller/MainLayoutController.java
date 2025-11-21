package saveit.controller;

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

    @FXML private Label welcomeLabel;
    @FXML private StackPane contentArea;
    @FXML private Button btnDashboard;
    @FXML private Button btnAdd;
    //@FXML private Button btnReports;
    @FXML private Button btnLogout;


    private int loggedInUserId;

    // Set logged-in user ID and load dashboard after
    public void setLoggedInUserId(int userId) {
        this.loggedInUserId = userId;
        if (welcomeLabel != null) {
            welcomeLabel.setText("Welcome back! (User ID: " + userId + ")");
        }
        // Ensure UI updates happen on the JavaFX Application Thread and after injections
        javafx.application.Platform.runLater(() -> {
            if (contentArea != null) {
                String cssPath = "/CSS/dashboard.css";
                contentArea.getStylesheets().add(getClass().getResource("/CSS/sidebar.css").toExternalForm());
                contentArea.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                showDashboard(null);
            }
        });
    }

    @FXML
    public void initialize() {
        // No default loading; dashboard loads after userId is set
    }


    @FXML
    public void showDashboard(ActionEvent event) {
        loadPage("/FXML/dashboard.fxml");
    }

    @FXML
    public void showAddTransaction(ActionEvent event) {
        loadPageWithUser("/FXML/add_transaction.fxml");
    }


    @FXML
    public void showBudget(ActionEvent event) {
        loadPageWithUser("/FXML/budget.fxml");
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

    // Generic loader without user ID
    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass().getMethod("setUserId", int.class).invoke(controller, loggedInUserId);
                } catch (NoSuchMethodException ignored) {}
                try {
                    controller.getClass().getMethod("setLoggedInUserId", int.class).invoke(controller, loggedInUserId);
                } catch (NoSuchMethodException ignored) {}
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


}
