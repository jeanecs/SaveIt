package saveit.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import saveit.model.DatabaseManager;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.collections.FXCollections;
import saveit.util.IconUtil;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for budget_goals.fxml
 * - Loads budgets from DB for given userId
 * - Computes spent amounts from transactions table by category + period
 * - Renders dynamic cards into a FlowPane
 * - Supports add / edit / delete (simple dialogs)
 */
public class BudgetGoalsController {

    @FXML private ProgressBar overallProgress;
    @FXML private Label totalBudgetedLabel;
    @FXML private Label totalSpentLabel;
    @FXML private Label remainingLabel;
    @FXML private FlowPane categoriesFlow;
    @FXML private Button addBudgetButton;

    private int userId = -1;

    // Data container
    private List<BudgetItem> budgets = new ArrayList<>();

    // Call this after login / when loading the layout
    public void setUserId(int userId) {
        this.userId = userId;
        loadBudgetsFromDB();
    }

    @FXML
    public void initialize() {
        // Basic FlowPane styling
        if (categoriesFlow != null) {
            categoriesFlow.setHgap(20);
            categoriesFlow.setVgap(20);
            categoriesFlow.setPadding(new Insets(0, 0, 10, 0));
        }
    }

    @FXML
    private void onAddBudgetClicked() {
        if (userId <= 0) {
            showAlert(Alert.AlertType.WARNING, "User not set", "Cannot add budget: user not set.");
            return;
        }
        showAddEditDialog(null);
    }

    // --- DB + UI logic ---

    private void loadBudgetsFromDB() {
        if (userId <= 0) return;

        Task<Void> loader = new Task<>() {
            @Override
            protected Void call() {
                List<BudgetItem> list = new ArrayList<>();
                String sql = "SELECT id, user_id, category, budget_limit AS `limit`, period FROM budgets WHERE user_id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            BudgetItem b = new BudgetItem(
                                    rs.getInt("id"),
                                    rs.getString("category"),
                                    rs.getDouble("limit"),
                                    rs.getString("period")
                            );
                            list.add(b);
                        }
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to load budgets: " + ex.getMessage()));
                }

                // compute spent for each budget
                for (BudgetItem b : list) {
                    b.spent = computeSpentForCategory(b.category, b.period);
                }

                budgets = list;
                Platform.runLater(() -> {
                    renderBudgetCards();
                    updateOverall();
                });

                return null;
            }
        };
        new Thread(loader).start();
    }

    private double computeSpentForCategory(String category, String period) {
        if (userId <= 0) return 0.0;
        double sum = 0.0;
        String sql;
        if ("yearly".equalsIgnoreCase(period)) {
            sql = "SELECT SUM(amount) AS total FROM transactions WHERE user_id = ? AND type = 'Expense' AND category = ? AND YEAR(`date`) = YEAR(CURDATE())";
        } else {
            // monthly (default)
            sql = "SELECT SUM(amount) AS total FROM transactions WHERE user_id = ? AND type = 'Expense' AND category = ? AND MONTH(`date`) = MONTH(CURDATE()) AND YEAR(`date`) = YEAR(CURDATE())";
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) sum = rs.getDouble("total");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return sum;
    }

    private void renderBudgetCards() {
        categoriesFlow.getChildren().clear();

        for (BudgetItem b : budgets) {
            VBox card = createBudgetCard(b);
            categoriesFlow.getChildren().add(card);
        }
    }

    private VBox createBudgetCard(BudgetItem b) {
        VBox root = new VBox(8);
        root.getStyleClass().add("category-card");
        root.setPadding(new Insets(14));
        root.setPrefWidth(320);

        // Title row
        HBox titleRow = new HBox(8);
        Label title = new Label(b.category);
        title.getStyleClass().add("category-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button();
        SVGPath editIcon = IconUtil.editIcon(.8, Color.LIGHTGRAY); // scale 1.2, color dark gray
        editBtn.setGraphic(editIcon);
        editBtn.setOnAction(e -> showAddEditDialog(b));
        editBtn.getStyleClass().add("icon-btn");

        Button delBtn = new Button();
        SVGPath delIcon = IconUtil.deleteIcon(.8, Color.LIGHTGRAY); // scale 1.2, color dark red
        delBtn.setGraphic(delIcon);
        delBtn.setOnAction(e -> {
            boolean ok = confirm("Delete budget", "Delete budget for '" + b.category + "'?");
            if (ok) deleteBudget(b);
        });
        delBtn.getStyleClass().add("icon-btn");

        titleRow.getChildren().addAll(title, spacer, editBtn, delBtn);

        Label period = new Label(capitalize(b.period));
        period.getStyleClass().add("category-subtitle");

        Label spentLabel = new Label("\u20B1" + formatMoney(b.spent));
        spentLabel.getStyleClass().add(b.spent > b.limit ? "value-negative" : "value-positive");

        HBox spentRow = new HBox();
        Label spentText = new Label("Spent");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        spentRow.getChildren().addAll(spentText, sp, spentLabel);

        // progress
        double progress = (b.limit > 0) ? Math.min(b.spent / b.limit, 1.0) : 0.0;
        ProgressBar pbar = new ProgressBar(progress);
        pbar.setPrefWidth(Double.MAX_VALUE);
        pbar.getStyleClass().add("budget-progress");

        HBox remainingRow = new HBox();
        Label remText = new Label(b.spent > b.limit ? "Over by" : "Remaining");
        Label remValue = new Label("\u20B1" + formatMoney(Math.abs(b.limit - b.spent)));
        remValue.getStyleClass().add(b.spent > b.limit ? "value-negative" : "value-positive");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        remainingRow.getChildren().addAll(remText, sp2, remValue);

        HBox limitRow = new HBox();
        Label limText = new Label("Budget Limit");
        Label limValue = new Label("\u20B1" + formatMoney(b.limit));
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        limitRow.getChildren().addAll(limText, sp3, limValue);

        root.getChildren().addAll(titleRow, period, spentRow, pbar, remainingRow, limitRow);
        return root;
    }

    private void updateOverall() {
        double totalBudgeted = budgets.stream().mapToDouble(b -> b.limit).sum();
        double totalSpent = budgets.stream().mapToDouble(b -> b.spent).sum();
        double overallProgressVal = (totalBudgeted > 0) ? Math.min(totalSpent / totalBudgeted, 1.0) : 0.0;

        totalBudgetedLabel.setText("\u20B1" + formatMoney(totalBudgeted));
        totalSpentLabel.setText("\u20B1" + formatMoney(totalSpent));
        remainingLabel.setText("\u20B1" + formatMoney(Math.max(totalBudgeted - totalSpent, 0.0)));
        overallProgress.setProgress(overallProgressVal);
    }

    // --- Add / Edit / Delete helpers ---

//    private void showAddEditDialog(BudgetItem edit) {
//        // Create a new Stage (modal)
//        Stage dialogStage = new Stage();
//        dialogStage.initModality(Modality.APPLICATION_MODAL); // makes it modal
//        dialogStage.setTitle(edit == null ? "Add Budget" : "Edit Budget");
//
//        // Create form
//        GridPane grid = new GridPane();
//        grid.setHgap(8);
//        grid.setVgap(8);
//        grid.setPadding(new Insets(15));
//
//        TextField categoryField = new TextField();
//        TextField limitField = new TextField();
//        ChoiceBox<String> periodBox = new ChoiceBox<>();
//        periodBox.getItems().addAll("monthly", "yearly");
//        periodBox.setValue("monthly");
//
//        if (edit != null) {
//            categoryField.setText(edit.category);
//            limitField.setText(String.valueOf(edit.limit));
//            periodBox.setValue(edit.period);
//        }
//
//        grid.add(new Label("Category"), 0, 0);
//        grid.add(categoryField, 1, 0);
//        grid.add(new Label("Limit (₱)"), 0, 1);
//        grid.add(limitField, 1, 1);
//        grid.add(new Label("Period"), 0, 2);
//        grid.add(periodBox, 1, 2);
//
//        // Buttons
//        Button okBtn = new Button("OK");
//        Button cancelBtn = new Button("Cancel");
//        HBox btnBox = new HBox(10, okBtn, cancelBtn);
//        btnBox.setPadding(new Insets(10));
//        grid.add(btnBox, 1, 3);
//
//        VBox root = new VBox(10, grid);
//        root.setPadding(new Insets(10));
//        Scene scene = new Scene(root);
//        // Apply your existing CSS
//        scene.getStylesheets().add(getClass().getResource("/CSS/budget-styles.css").toExternalForm());
//
//        dialogStage.setScene(scene);
//
//        // Button actions
//        okBtn.setOnAction(e -> {
//            try {
//                double lim = Double.parseDouble(limitField.getText().trim());
//                String cat = categoryField.getText().trim();
//                String per = periodBox.getValue();
//                if (cat.isEmpty() || lim <= 0) {
//                    showAlert(Alert.AlertType.WARNING, "Validation", "Please enter valid values.");
//                    return;
//                }
//
//                if (edit == null) {
//                    insertBudgetToDB(cat, lim, per);
//                } else {
//                    updateBudgetInDB(edit.id, cat, lim, per);
//                }
//
//                dialogStage.close();
//            } catch (NumberFormatException ex) {
//                showAlert(Alert.AlertType.WARNING, "Validation", "Limit must be a number.");
//            }
//        });
//
//        cancelBtn.setOnAction(e -> dialogStage.close());
//
//        dialogStage.showAndWait(); // show modal
//    }

    private void showAddEditDialog(BudgetItem edit) {

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setResizable(false);
        dialogStage.setTitle(edit == null ? "Add New Budget" : "Edit Budget");

        // ==== Main Card ====
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(25));
        card.setMinWidth(360);

        // Title
        Label title = new Label(edit == null ? "Add New Budget" : "Edit Budget");
        title.getStyleClass().add("page-title");

        // Category field
        Label catLabel = new Label("Category");
        catLabel.getStyleClass().add("small-label");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.setPromptText("Select category");
        categoryCombo.setPrefWidth(Double.MAX_VALUE);

// Expense categories (inspired by your snippet)
        categoryCombo.setItems(FXCollections.observableArrayList(
                "Food", "Bills", "Transportation", "Entertainment",
                "Shopping", "Healthcare", "Education", "Other"
        ));


        // Limit field
        Label limitLabel = new Label("Budget Limit (₱)");
        limitLabel.getStyleClass().add("small-label");

        TextField limitField = new TextField();
        limitField.setPromptText("Enter amount");

        // Period field
        Label periodLabel = new Label("Period");
        periodLabel.getStyleClass().add("small-label");

        ChoiceBox<String> periodBox = new ChoiceBox<>();
        periodBox.getItems().addAll("Monthly", "Yearly");
        periodBox.setValue("Monthly");

        // Fill fields when editing
        if (edit != null) {
            categoryCombo.setValue(edit.category);
            limitField.setText(String.valueOf(edit.limit));
            periodBox.setValue(edit.period.substring(0,1).toUpperCase() + edit.period.substring(1));
        }

        // Submit button
        Button submitBtn = new Button(edit == null ? "Add Budget" : "Save Changes");
        submitBtn.getStyleClass().add("primary-action-btn");
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        submitBtn.setOnAction(e -> {
            try {
                double lim = Double.parseDouble(limitField.getText().trim());
                String cat = categoryCombo.getValue();
                String per = periodBox.getValue().toLowerCase();

                if (cat.isEmpty() || lim <= 0) {
                    showAlert(Alert.AlertType.WARNING, "Validation", "Please enter valid values.");
                    return;
                }

                if (edit == null) {
                    insertBudgetToDB(cat, lim, per);
                } else {
                    updateBudgetInDB(edit.id, cat, lim, per);
                }

                dialogStage.close();

            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Limit must be a number.");
            }
        });

        // Add everything into card
        card.getChildren().addAll(
                title,
                catLabel, categoryCombo,
                limitLabel, limitField,
                periodLabel, periodBox,
                submitBtn
        );

        Scene scene = new Scene(card);
        scene.getStylesheets().add(
                getClass().getResource("/CSS/budget-styles.css").toExternalForm()
        );

        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }



    private void insertBudgetToDB(String category, double limit, String period) {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                String sql = "INSERT INTO budgets (user_id, category, budget_limit, period) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, category);
                    ps.setDouble(3, limit);
                    ps.setString(4, period);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to insert budget: " + ex.getMessage()));
                }

                // reload
                Platform.runLater(() -> loadBudgetsFromDB());
                return null;
            }
        };
        new Thread(t).start();
    }

    private void updateBudgetInDB(int id, String category, double limit, String period) {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                String sql = "UPDATE budgets SET category = ?, budget_limit = ?, period = ? WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, category);
                    ps.setDouble(2, limit);
                    ps.setString(3, period);
                    ps.setInt(4, id);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to update budget: " + ex.getMessage()));
                }

                Platform.runLater(() -> loadBudgetsFromDB());
                return null;
            }
        };
        new Thread(t).start();
    }

    private void deleteBudget(BudgetItem b) {
        Task<Void> t = new Task<>() {
            @Override
            protected Void call() {
                String sql = "DELETE FROM budgets WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, b.id);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "DB Error", "Failed to delete budget: " + ex.getMessage()));
                }

                Platform.runLater(() -> loadBudgetsFromDB());
                return null;
            }
        };
        new Thread(t).start();
    }

    // --- Utilities ---
    private boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.YES;
    }

    private void showAlert(Alert.AlertType type, String title, String body) {
        Alert a = new Alert(type, body, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private String formatMoney(double val) {
        // simple formatting; you can use NumberFormat/Locale if you prefer
        return String.format("%,.2f", val);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // --- Small helper classes ---
    private static class BudgetItem {
        int id;
        String category;
        double limit;
        String period; // monthly | yearly
        double spent;

        BudgetItem(int id, String category, double limit, String period) {
            this.id = id;
            this.category = category;
            this.limit = limit;
            this.period = (period == null || period.isEmpty()) ? "monthly" : period;
            this.spent = 0.0;
        }
    }

    private static class BudgetFormResult {
        final String category;
        final double limit;
        final String period;
        BudgetFormResult(String category, double limit, String period) {
            this.category = category; this.limit = limit; this.period = period;
        }
    }
}
