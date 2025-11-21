package saveit.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import saveit.model.Transaction;
import saveit.model.DatabaseManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.shape.SVGPath;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;


public class DashboardController {

    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label balanceLabel; // new balance label
    @FXML private PieChart expensePieChart;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private TableView<Transaction> recentTransactionsTable;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, Number> amountCol;
    @FXML private TableColumn<Transaction, String> notesCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Void> actionCol; // New action column



    private int userId;

    @FXML
    public void initialize() {
        setupTable();
        setupActionColumn();

        // Combined row factory with alternating colors, hover, and selection fix
        recentTransactionsTable.setRowFactory(tv -> new TableRow<Transaction>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                    setGraphic(null);
                } else {
                    updateRowStyle();
                }
            }

            private void updateRowStyle() {
                String baseStyle;

                // Alternating row colors
                if (getIndex() % 2 == 0) {
                    baseStyle = "-fx-background-color: #F8FAFB;";
                } else {
                    baseStyle = "-fx-background-color: white;";
                }

                // Selection style - keep text visible
                if (isSelected()) {
                    baseStyle = "-fx-background-color: #E3F2FD; -fx-text-fill: #2F4858;";
                }

                setStyle(baseStyle);

                // Add hover effect
                setOnMouseEntered(event -> {
                    if (!isEmpty()) {
                        if (isSelected()) {
                            setStyle("-fx-background-color: #BBDEFB; -fx-text-fill: #2F4858;");
                        } else {
                            setStyle("-fx-background-color: #E8E8E8; -fx-text-fill: #2F4858;");
                        }
                    }
                });

                setOnMouseExited(event -> {
                    if (!isEmpty()) {
                        updateRowStyle();
                    }
                });
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (!isEmpty()) {
                    updateRowStyle();
                }
            }
        });

        // 2. "Type" styled as colored chip - CENTERED
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || type == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label chip = new Label(type);
                String chipStyle = type.equalsIgnoreCase("Income")
                        ? "-fx-background-color: rgba(0,191,166,0.1); -fx-text-fill: #00BFA6; -fx-background-radius: 20; -fx-padding: 5 15 5 15;"
                        : "-fx-background-color: rgba(249,168,38,0.1); -fx-text-fill: #F9A826; -fx-background-radius: 20; -fx-padding: 5 15 5 15;";

                chip.setStyle(chipStyle);
                setGraphic(chip);
                setText(null);
            }
        });

        // 3. Amount colored - CENTERED
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number amount, boolean empty) {
                super.updateItem(amount, empty);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                Transaction row = getTableView().getItems().get(getIndex());
                setText("₱" + String.format("%,.2f", amount.doubleValue()));

                setStyle(
                        row.getType().equalsIgnoreCase("Income")
                                ? "-fx-text-fill: #00BFA6; -fx-alignment: CENTER;"
                                : "-fx-text-fill: #F9A826; -fx-alignment: CENTER;"
                );
            }
        });

        // 4. Date column formatting - CENTERED
        dateCol.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || date == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(formatter.format(date));
                    setStyle("-fx-text-fill: #2F4858; -fx-alignment: CENTER;");
                }
            }
        });

        // 5. Category and Notes columns - CENTERED
        categoryCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || category == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(category);
                    setStyle("-fx-text-fill: #2F4858; -fx-alignment: CENTER;");
                }
            }
        });

        notesCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String notes, boolean empty) {
                super.updateItem(notes, empty);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || notes == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(notes);
                    setStyle("-fx-text-fill: #2F4858; -fx-alignment: CENTER;");
                }
            }
        });

        // 6. Header style - CENTERED
        recentTransactionsTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            recentTransactionsTable.lookupAll(".column-header").forEach(node -> {
                node.setStyle("-fx-background-color: white; -fx-border-color: #F8FAFB; -fx-text-fill: #2F4858; -fx-font-weight: bold; -fx-alignment: CENTER;");
            });
            recentTransactionsTable.lookupAll(".column-header-background .label").forEach(node -> {
                if (node instanceof Label) {
                    ((Label) node).setAlignment(javafx.geometry.Pos.CENTER);
                }
            });
        });
    }

    /** Called by MainController AFTER user logs in */
    public void setUserId(int id) {
        this.userId = id;
        loadDashboardData();
    }

    private void setupTable() {
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        recentTransactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dateCol.setStyle("-fx-alignment: CENTER;");
        categoryCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setStyle("-fx-alignment: CENTER;");
        amountCol.setStyle("-fx-alignment: CENTER;");
        notesCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setStyle("-fx-alignment: CENTER;");

    }

    private void loadDashboardData() {
        loadTotals();
        loadPieChart();
        loadMonthlyBarChart(); // <- use this instead of loadBarChart()
        loadRecentTransactions();
    }

    /* ------------------ TOTALS ------------------ */

    private void loadTotals() {
        String incomeQuery =
                "SELECT SUM(amount) AS total_income FROM transactions WHERE category='Income' AND user_id=?";
        String expenseQuery =
                "SELECT SUM(amount) AS total_expense FROM transactions WHERE category='Expense' AND user_id=?";

        try (Connection conn = DatabaseManager.getConnection()) {

            // INCOME
            PreparedStatement stmt1 = conn.prepareStatement(incomeQuery);
            stmt1.setInt(1, userId);
            ResultSet rs1 = stmt1.executeQuery();
            double income = 0;
            if (rs1.next()) {
                income = rs1.getDouble("total_income");
                if (rs1.wasNull()) income = 0;
                totalIncomeLabel.setText(String.format("\u20B1%,.2f", income));

            }

            // EXPENSE
            PreparedStatement stmt2 = conn.prepareStatement(expenseQuery);
            stmt2.setInt(1, userId);
            ResultSet rs2 = stmt2.executeQuery();
            double expense = 0;
            if (rs2.next()) {
                expense = rs2.getDouble("total_expense");
                if (rs2.wasNull()) expense = 0;
                totalExpenseLabel.setText(String.format("\u20B1%,.2f", expense));

            }

            double balance = income - expense;
            balanceLabel.setText(String.format("\u20B1%,.2f", balance));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------ RECENT TRANSACTIONS ------------------ */

    private void loadRecentTransactions() {
        ObservableList<Transaction> transactions = FXCollections.observableArrayList();

        String sql = """
        SELECT id, category, type, amount, date, notes
        FROM transactions
        WHERE user_id=?
        ORDER BY date DESC, id DESC
        LIMIT 10
    """;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String category = rs.getString("category");
                String type = rs.getString("type");
                double amount = rs.getDouble("amount");
                LocalDate date = rs.getDate("date").toLocalDate();
                String notes = rs.getString("notes");

                // Use the constructor that includes the ID
                transactions.add(new Transaction(id, type, category, amount, date, notes));
            }

            recentTransactionsTable.setItems(transactions);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------ EXPENSE PIE CHART ------------------ */

    private void loadPieChart() {
        expensePieChart.getData().clear();

        String sql = """
            SELECT category, SUM(amount) AS total
            FROM transactions 
            WHERE type='Expense' AND user_id=?
            GROUP BY category
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                expensePieChart.getData().add(
                        new PieChart.Data(rs.getString("category"), rs.getDouble("total"))
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ------------------ MONTHLY BAR CHART ------------------ */

    private void loadBarChart() {
        monthlyBarChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Total");

        String sql = """
            SELECT MONTHNAME(date) AS month, SUM(amount) AS total
            FROM transactions
            WHERE user_id=?
            GROUP BY MONTH(date)
            ORDER BY MONTH(date)
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                series.getData().add(
                        new XYChart.Data<>(rs.getString("month"), rs.getDouble("total"))
                );
            }

            monthlyBarChart.getData().add(series);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMonthlyBarChart() {
        monthlyBarChart.getData().clear();

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expense");

        String sql = """
    SELECT
        DATE_FORMAT(date, '%Y-%m') AS month_key,
        MONTHNAME(date) AS month,
        MONTH(date) AS month_num,
        COALESCE(SUM(CASE WHEN type='Income' THEN amount END), 0) AS income,
        COALESCE(SUM(CASE WHEN type='Expense' THEN amount END), 0) AS expense
    FROM transactions
    WHERE user_id=?
    GROUP BY DATE_FORMAT(date, '%Y-%m'), MONTHNAME(date), MONTH(date)
    ORDER BY DATE_FORMAT(date, '%Y-%m')
""";

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String month = rs.getString("month");
                double income = rs.getDouble("income");
                double expense = rs.getDouble("expense");

                // handle nulls

                incomeSeries.getData().add(new XYChart.Data<>(month, income));
                expenseSeries.getData().add(new XYChart.Data<>(month, expense));
            }

            monthlyBarChart.getData().addAll(incomeSeries, expenseSeries);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = createIconButton(createDeleteIcon(), "delete-button");
            private final Button editButton = createIconButton(createEditIcon(), "edit-button");
            private final HBox pane = new HBox(8, deleteButton, editButton);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                pane.setPadding(new Insets(5, 0, 5, 0));

                deleteButton.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleDelete(transaction);
                });

                editButton.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    handleEdit(transaction);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    // Helper method to create circular icon buttons
    private Button createIconButton(SVGPath icon, String styleClass) {
        Button button = new Button();
        button.setGraphic(icon);

        String borderColor = styleClass.contains("delete") ? "#F45B69" : "#2F4858";

        button.setStyle("-fx-background-radius: 8; " +
                "-fx-border-radius: 8; " +
                "-fx-min-width: 32px; " +
                "-fx-max-width: 32px; " +
                "-fx-min-height: 32px; " +
                "-fx-max-height: 32px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0; " +
                "-fx-background-color: transparent; " +
                "-fx-border-color: " + borderColor + "; " +
                "-fx-border-width: 2;");

        // Add hover effect - fill background on hover
        button.setOnMouseEntered(e -> {
            String hoverColor = styleClass.contains("delete") ? "#F45B69" : "#2F4858";
            button.setStyle("-fx-background-radius: 8; " +
                    "-fx-border-radius: 8; " +
                    "-fx-min-width: 32px; " +
                    "-fx-max-width: 32px; " +
                    "-fx-min-height: 32px; " +
                    "-fx-max-height: 32px; " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 0; " +
                    "-fx-background-color: " + hoverColor + "; " +
                    "-fx-border-color: " + hoverColor + "; " +
                    "-fx-border-width: 2; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);");

            // Change icon to white on hover
            icon.setFill(javafx.scene.paint.Color.WHITE);
        });

        button.setOnMouseExited(e -> {
            String borderColor2 = styleClass.contains("delete") ? "#F45B69" : "#2F4858";
            button.setStyle("-fx-background-radius: 8; " +
                    "-fx-border-radius: 8; " +
                    "-fx-min-width: 32px; " +
                    "-fx-max-width: 32px; " +
                    "-fx-min-height: 32px; " +
                    "-fx-max-height: 32px; " +
                    "-fx-cursor: hand; " +
                    "-fx-padding: 0; " +
                    "-fx-background-color: transparent; " +
                    "-fx-border-color: " + borderColor2 + "; " +
                    "-fx-border-width: 2;");

            // Change icon back to border color
            javafx.scene.paint.Color iconColor = styleClass.contains("delete")
                    ? javafx.scene.paint.Color.web("#F45B69")
                    : javafx.scene.paint.Color.web("#2F4858");
            icon.setFill(iconColor);
        });

        // Add tooltip
        Tooltip tooltip = new Tooltip(styleClass.contains("delete") ? "Delete" : "Edit");
        button.setTooltip(tooltip);

        return button;
    }

    private void handleEdit(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/add_transaction.fxml"));
            Parent root = loader.load();

            AddTransactionController controller = loader.getController();
            controller.setUserId(userId);
            controller.setTransactionToEdit(transaction); // Pass transaction to edit

            Stage stage = new Stage();
            stage.setTitle("Edit Transaction");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait(); // Wait for the edit window to close

            loadDashboardData(); // Refresh data after editing

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDelete(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Transaction");
        alert.setHeaderText("Are you sure you want to delete this transaction?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteTransactionFromDB(transaction.getId());
            loadDashboardData(); // Refresh data
        }
    }

    private void deleteTransactionFromDB(int transactionId) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            // Show error alert
        }
    }

    private SVGPath createDeleteIcon() {
        SVGPath deleteIcon = new SVGPath();
        // Trash can icon
        deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        deleteIcon.setScaleX(0.55);
        deleteIcon.setScaleY(0.55);
        deleteIcon.setFill(javafx.scene.paint.Color.web("#F45B69")); // Red color for delete
        return deleteIcon;
    }

    private SVGPath createEditIcon() {
        SVGPath editIcon = new SVGPath();
        // Pencil/Edit icon
        editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        editIcon.setScaleX(0.55);
        editIcon.setScaleY(0.55);
        editIcon.setFill(javafx.scene.paint.Color.web("#2F4858")); // Dark blue color for edit
        return editIcon;
    }

}

