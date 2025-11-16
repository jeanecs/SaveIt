package saveit.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import saveit.model.DatabaseManager;
import saveit.model.Transaction;

import java.sql.*;
import java.time.LocalDate;

public class AddTransactionController {

    @FXML private ComboBox<String> typeComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField amountField;
    @FXML private DatePicker datePicker;
    @FXML private TextArea notesArea;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, String> typeCol;
    @FXML private TableColumn<Transaction, Double> amountCol;
    @FXML private TableColumn<Transaction, String> notesCol;
    @FXML private TableColumn<Transaction, String> dateCol;
    private int userId; // store logged-in user ID
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    /** Called by DashboardController after loading FXML */
    public void setUserId(int userId) {
        this.userId = userId;
        loadTransactionsFromDB(); // load transactions for this user
    }

    @FXML
    public void initialize() {
        // Setup type dropdown
        typeComboBox.setItems(FXCollections.observableArrayList("Income", "Expense"));
        typeComboBox.setValue("Expense");

        // Setup date picker
        datePicker.setValue(LocalDate.now());

        // Update categories when type changes
        updateCategoryOptions();
        typeComboBox.setOnAction(e -> updateCategoryOptions());

        // Setup table columns
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDate().toString()
        ));
        categoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAmount()));
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        amountCol.setCellFactory(col -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("₱%.2f", amount));
                }
            }
        });

        // Format date column
        dateCol.setCellFactory(col -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    try {
                        LocalDate localDate = LocalDate.parse(date);
                        setText(localDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                    } catch (Exception e) {
                        setText(date);
                    }
                }
            }
        });

        transactionTable.setItems(transactions);
    }



    private void updateCategoryOptions() {
        if ("Income".equals(typeComboBox.getValue())) {
            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "Salary", "Freelance", "Investment", "Bonus", "Other Income"
            ));
        } else {
            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "Food", "Bills", "Transportation", "Entertainment",
                    "Shopping", "Healthcare", "Education", "Other"
            ));
        }
        categoryComboBox.setValue(null);
    }

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            String type = typeComboBox.getValue();
            String category = categoryComboBox.getValue();
            double amount = Double.parseDouble(amountField.getText());
            LocalDate date = datePicker.getValue();
            String notes = notesArea.getText();

            if (category == null || amount <= 0 || date == null) {
                showAlert("Please fill in all required fields.");
                return;
            }

            Transaction transaction = new Transaction(type, category, amount, date, notes);

            // Save to DB
            saveTransactionToDB(transaction);

            // Reload transactions from DB to reflect changes
            loadTransactionsFromDB();

            // Reset form
            amountField.clear();
            notesArea.clear();
            categoryComboBox.setValue(null);
            typeComboBox.setValue("Expense");
            datePicker.setValue(LocalDate.now());

        } catch (NumberFormatException e) {
            showAlert("Invalid amount. Please enter a number.");
        }
    }

    private void saveTransactionToDB(Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, type, category, amount, date, notes) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, transaction.getType());
            stmt.setString(3, transaction.getCategory());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setDate(5, java.sql.Date.valueOf(transaction.getDate())); // LocalDate -> java.sql.Date
            stmt.setString(6, transaction.getNotes());

            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error saving transaction to database.");
        }
    }

    private void loadTransactionsFromDB() {
        if (userId == 0) return; // no user set yet

        transactions.clear();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM transactions WHERE user_id = ? AND date = ? ORDER BY date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(today)); // Only today's date
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String type = rs.getString("type");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                java.sql.Date sqlDate = rs.getDate("date");
                LocalDate date = (sqlDate != null) ? sqlDate.toLocalDate() : LocalDate.now();
                String notes = rs.getString("notes");

                Transaction transaction = new Transaction(type, category, amount, date, notes);
                transactions.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Failed to load transactions from database.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // Optionally navigate back to dashboard
        // saveit.Main.changeScene("/FXML/dashboard.fxml", "SaveIT - Dashboard");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
