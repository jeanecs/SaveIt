package saveit.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import saveit.model.Transaction;
import saveit.model.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableCell;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;



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


    private int userId;

    @FXML
    public void initialize() {
        setupTable();

        // 1. Alternating row colors
        recentTransactionsTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    if (getIndex() % 2 == 0) {
                        setStyle("-fx-background-color: #F8FAFB;");
                    } else {
                        setStyle("-fx-background-color: white;");
                    }
                }
            }
        });

        // 2. "Type" styled as colored chip
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);

                if (empty || type == null) {
                    setGraphic(null);
                    return;
                }

                Label chip = new Label(type);
                chip.setStyle(
                        type.equalsIgnoreCase("Income")
                                ? "-fx-background-color: rgba(0,191,166,0.1); -fx-text-fill: #00BFA6;"
                                : "-fx-background-color: rgba(249,168,38,0.1); -fx-text-fill: #F9A826;"
                );
                chip.setPadding(new Insets(5, 10, 5, 10));
                chip.setStyle(chip.getStyle() + "; -fx-background-radius: 20;");

                setGraphic(chip);
            }
        });

        // 3. Amount colored
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number amount, boolean empty) {
                super.updateItem(amount, empty);

                if (empty || amount == null) {
                    setText(null);
                    return;
                }

                Transaction row = getTableView().getItems().get(getIndex());

                setText("\u20B1" + String.format("%,.2f", amount.doubleValue()));

                setStyle(
                        row.getType().equalsIgnoreCase("Income")
                                ? "-fx-text-fill: #00BFA6;"
                                : "-fx-text-fill: #F9A826;"
                );
            }
        });

        // 4. Header style
        recentTransactionsTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            recentTransactionsTable.lookupAll("TableColumnHeader").forEach(node -> {
                node.setStyle("-fx-background-color: white; -fx-border-color: #F8FAFB; -fx-text-fill: #2F4858;");
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
        List<Transaction> transactions = new ArrayList<>();

        String sql = """
            SELECT category, type, amount, date, notes 
            FROM transactions 
            WHERE user_id=? 
            ORDER BY date DESC 
            LIMIT 5
        """;

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transactions.add(new Transaction(
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        rs.getDate("date").toLocalDate(),
                        rs.getString("notes")
                ));
            }

            recentTransactionsTable.getItems().setAll(transactions);

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

            applyBarColors(incomeSeries, "#00BFA6"); // teal
            applyBarColors(expenseSeries, "#F9A826"); // yellow


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyBarColors(XYChart.Series<String, Number> series, String colorHex) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + colorHex + ";");
                }
            });
        }
    }






}

