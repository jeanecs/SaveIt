package saveit.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import saveit.model.DatabaseManager;
import saveit.model.Transaction;
import saveit.util.IconUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class DashboardController {

    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpenseLabel;
    @FXML private Label balanceLabel;

    // New FXML fields for trend indicators
    @FXML private Label incomeTrendLabel;
    @FXML private Label expenseTrendLabel;
    @FXML private Label balanceTrendLabel;

    @FXML private PieChart expensePieChart;
    @FXML private BarChart<String, Number> monthlyBarChart;
    @FXML private TableView<Transaction> recentTransactionsTable;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, Number> amountCol;
    @FXML private TableColumn<Transaction, String> notesCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Void> actionCol;

    private int userId;

    @FXML
    public void initialize() {
        setupTable();
        setupActionColumn();

        // Ensure main labels have white text color (assuming dark dashboard background)
        String mainLabelStyle = "-fx-text-fill: white; -fx-font-weight: bold;";
        totalIncomeLabel.setStyle(mainLabelStyle + "-fx-font-size: 24px;");
        totalExpenseLabel.setStyle(mainLabelStyle + "-fx-font-size: 24px;");
        balanceLabel.setStyle(mainLabelStyle + "-fx-font-size: 24px;");


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
                setText("\u20B1" + String.format("%,.2f", amount.doubleValue()));

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

    public void loadDashboardData() {
        loadTotals();
        loadPieChart();
        loadMonthlyBarChart();
        loadRecentTransactions();
    }

    /* ------------------ TOTALS & TRENDS ------------------ */

    private static class MonthlySummary {
        double income = 0;
        double expense = 0;
    }

    /**
     * Fetches the total income and expense for a given month.
     * @param userId The ID of the user.
     * @param monthKey The month key in 'YYYY-MM' format.
     * @return A MonthlySummary object.
     */
    private MonthlySummary getMonthlySummary(int userId, String monthKey) {
        MonthlySummary summary = new MonthlySummary();
        String sql = """
            SELECT
                COALESCE(SUM(CASE WHEN type='Income' THEN amount ELSE 0 END), 0) AS total_income,
                COALESCE(SUM(CASE WHEN type='Expense' THEN amount ELSE 0 END), 0) AS total_expense
            FROM transactions
            WHERE user_id=? AND DATE_FORMAT(date, '%Y-%m') = ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, monthKey);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                summary.income = rs.getDouble("total_income");
                summary.expense = rs.getDouble("total_expense");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return summary;
    }

    private void loadTotals() {
        // Calculate the YYYY-MM key for the current and previous month
        LocalDate now = LocalDate.now();
        String currentMonthKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String previousMonthKey = now.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Fetch data
        MonthlySummary currentMonth = getMonthlySummary(userId, currentMonthKey);
        MonthlySummary previousMonth = getMonthlySummary(userId, previousMonthKey);

        // Current Month Totals
        double currIncome = currentMonth.income;
        double currExpense = currentMonth.expense;
        double currBalance = currIncome - currExpense;

        // Previous Month Totals
        double prevIncome = previousMonth.income;
        double prevExpense = previousMonth.expense;
        double prevBalance = prevIncome - prevExpense;

        // 1. Update Main Labels (Current Month)
        totalIncomeLabel.setText(String.format("\u20B1%,.2f", currIncome));
        totalExpenseLabel.setText(String.format("\u20B1%,.2f", currExpense));
        balanceLabel.setText(String.format("\u20B1%,.2f", currBalance));

        // 2. Update Trend Labels
        updateTrendLabel(incomeTrendLabel, currIncome, prevIncome, "income");
        updateTrendLabel(expenseTrendLabel, currExpense, prevExpense, "expense");
        updateTrendLabel(balanceTrendLabel, currBalance, prevBalance, "balance");
    }

    /**
     * Creates an HBox graphic containing the SVG icon and the trend text.
     */
    private HBox createTrendGraphic(SVGPath icon, String trendText, String colorHex) {
        Label textLabel = new Label(trendText);
        textLabel.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        // Update the icon fill color
        icon.setFill(Color.web(colorHex));

        HBox graphic = new HBox(5, icon, textLabel);
        graphic.setAlignment(Pos.CENTER_LEFT);
        return graphic;
    }

    /**
     * Calculates the percentage change and updates the trend label with the correct arrow (SVG) and color.
     * @param label The FXML Label to update.
     * @param current The current month's value.
     * @param previous The previous month's value.
     * @param type The type of metric (income, expense, or balance) to determine trend direction.
     */
    private void updateTrendLabel(Label label, double current, double previous, String type) {
        String colorHex;
        String trendText;
        double change;
        SVGPath trendIcon;

        // Default style for non-trend cases
        final String NEUTRAL_COLOR = "#E0E0E0"; // Light gray/white for visibility

        if (previous == 0) {
            trendIcon = IconUtil.plusIcon(0.8, Color.web(NEUTRAL_COLOR));
            if (current > 0) {
                trendText = "NEW DATA";
            } else {
                trendText = "No data last month";
            }
            label.setGraphic(createTrendGraphic(trendIcon, trendText, NEUTRAL_COLOR));
            label.setText(null); // Use graphic instead of text
            return;
        }

        change = ((current - previous) / previous) * 100;
        String sign = change >= 0 ? "+" : "";

        // Handle no change
        if (Math.abs(change) < 0.01) {
            trendIcon = IconUtil.plusIcon(0.8, Color.web(NEUTRAL_COLOR)); // Use plus or a dash icon for no change
            trendText = "Same as last month";
            label.setGraphic(createTrendGraphic(trendIcon, trendText, NEUTRAL_COLOR));
            label.setText(null);
            return;
        }

        // Determine icon and color based on metric type and change
        boolean isPositiveTrend;
        if (type.equalsIgnoreCase("income") || type.equalsIgnoreCase("balance")) {
            isPositiveTrend = change >= 0;
            // Income/Balance: Green for increase (good), Red for decrease (bad)
            trendIcon = isPositiveTrend ? IconUtil.arrowUpIcon(0.8, Color.web(NEUTRAL_COLOR)) : IconUtil.arrowDownIcon(0.8, Color.web(NEUTRAL_COLOR));
        } else { // Expense
            isPositiveTrend = change <= 0;
            // Expense: Green for decrease (good), Red for increase (bad)
            trendIcon = isPositiveTrend ? IconUtil.arrowDownIcon(0.8, Color.web(NEUTRAL_COLOR)) : IconUtil.arrowUpIcon(0.8, Color.web(NEUTRAL_COLOR));
        }

        trendText = String.format("%s%.1f%% from last month", sign, Math.abs(change));

        // Set the graphic (icon + text) instead of just the text
        label.setGraphic(createTrendGraphic(trendIcon, trendText, NEUTRAL_COLOR));
        label.setText(null); // Clear text content since we are using the graphic
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

        // Filter by current month for relevance
        LocalDate now = LocalDate.now();
        String currentMonthKey = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        String sql = """
            SELECT category, SUM(amount) AS total
            FROM transactions
            WHERE type='Expense' AND user_id=? AND DATE_FORMAT(date, '%Y-%m') = ?
            GROUP BY category
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, currentMonthKey);

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

    private void loadMonthlyBarChart() {
        monthlyBarChart.getData().clear();

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expense");

        // The query is fine, but I'll ensure it only shows the last 6 months for better visual clarity
        String sql = """
    SELECT
        DATE_FORMAT(date, '%Y-%m') AS month_key,
        MONTHNAME(date) AS month,
        COALESCE(SUM(CASE WHEN type='Income' THEN amount END), 0) AS income,
        COALESCE(SUM(CASE WHEN type='Expense' THEN amount END), 0) AS expense
    FROM transactions
    WHERE user_id=? AND date >= DATE_SUB(LAST_DAY(CURRENT_DATE()), INTERVAL 5 MONTH)
    GROUP BY month_key, month
    ORDER BY month_key
""";

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String month = rs.getString("month");
                double income = rs.getDouble("income");
                double expense = rs.getDouble("expense");

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
            // Use IconUtil for cleaner code and consistent sizing/coloring logic
            final String NEUTRAL_COLOR = "#E0E0E0";
            private final Button deleteButton = createIconButton(IconUtil.deleteIcon(0.8, Color.web(NEUTRAL_COLOR)), "delete-button");
            private final Button editButton = createIconButton(IconUtil.editIcon(0.8, Color.web(NEUTRAL_COLOR)), "edit-button");
            private final HBox pane = new HBox(8, editButton, deleteButton);

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

    private Button createIconButton(SVGPath icon, String styleClass) {
        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add("icon-btn"); // Use a style class for consistency
        // Add specific class for delete if needed for color override
        if (styleClass.contains("delete")) {
            button.getStyleClass().add("delete-icon-btn");
        }
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

    // Removed the manual createDeleteIcon and createEditIcon as they are now handled by IconUtil
    // private SVGPath createDeleteIcon() {...}
    // private SVGPath createEditIcon() {...}

}