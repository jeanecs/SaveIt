package saveit.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class SidebarController {

    private MainLayoutController mainLayoutController;

    public void setMainLayoutController(MainLayoutController controller) {
        this.mainLayoutController = controller;
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        mainLayoutController.showDashboard(event);
    }

    @FXML
    private void handleAddTransaction(ActionEvent event) {
        mainLayoutController.showAddTransaction(event);
    }

    @FXML
    private void handleBudget(ActionEvent event) {
        mainLayoutController.showBudget(event);
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        mainLayoutController.handleLogout(event);
    }
}
