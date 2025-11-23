package saveit.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import saveit.model.DatabaseManager;
import saveit.model.Transaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

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
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Void> actionCol;


    private int userId;
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private Transaction transactionToEdit;

    public void setUserId(int userId) {
        this.userId = userId;
        loadTransactionsFromDB();
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

        // Setup table with styling
        setupTableWithStyling();
        setupActionColumn();

        transactionTable.setItems(transactions);
    }

    private void setupTableWithStyling() {
        // Set cell value factories
        dateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getDate()));
        categoryCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        amountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAmount()));
        notesCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNotes()));

        // Center align all columns
        dateCol.setStyle("-fx-alignment: CENTER;");
        categoryCol.setStyle("-fx-alignment: CENTER;");
        typeCol.setStyle("-fx-alignment: CENTER;");
        amountCol.setStyle("-fx-alignment: CENTER;");
        notesCol.setStyle("-fx-alignment: CENTER;");

        // Alternating row colors with hover effect
        transactionTable.setRowFactory(tv -> new TableRow<Transaction>() {
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

                // Selection style
                if (isSelected()) {
                    baseStyle = "-fx-background-color: #E3F2FD; -fx-text-fill: #2F4858;";
                }

                setStyle(baseStyle);

                // Hover effect
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

        // Type column - colored chip style (CENTERED)
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

        // Amount column - colored by type (CENTERED)
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || amount == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                Transaction row = getTableView().getItems().get(getIndex());
                setText("\u20B1" + String.format("%,.2f", amount));

                setStyle(
                        row.getType().equalsIgnoreCase("Income")
                                ? "-fx-text-fill: #00BFA6; -fx-alignment: CENTER;"
                                : "-fx-text-fill: #F9A826; -fx-alignment: CENTER;"
                );
            }
        });

        // Date column - formatted (CENTERED)
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

        // Category column (CENTERED)
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

        // Notes column (CENTERED)
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

        // Header style (CENTERED)
        transactionTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            transactionTable.lookupAll(".column-header").forEach(node -> {
                node.setStyle("-fx-background-color: white; -fx-border-color: #F8FAFB; -fx-text-fill: #2F4858; -fx-font-weight: bold; -fx-alignment: CENTER;");
            });
            transactionTable.lookupAll(".column-header-background .label").forEach(node -> {
                if (node instanceof Label) {
                    ((Label) node).setAlignment(javafx.geometry.Pos.CENTER);
                }
            });
        });
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

            if (userId <= 0) {
                showAlert("No user set. Cannot save transaction to database.");
                return;
            }

            String type = typeComboBox.getValue();
            String category = categoryComboBox.getValue();
            double amount = Double.parseDouble(amountField.getText());
            LocalDate date = datePicker.getValue();
            String notes = notesArea.getText();

            if (category == null || amount <= 0 || date == null) {
                showAlert("Please fill in all required fields.");
                return;
            }

            if (transactionToEdit != null) {
                // This is an UPDATE
                Transaction updatedTransaction = new Transaction(
                        transactionToEdit.getId(), type, category, amount, date, notes
                );
                updateTransactionInDB(updatedTransaction);
                loadTransactionsFromDB();         // refresh UI after update
                transactionToEdit = null;
                handleCancel(event);
            } else {
                // This is a NEW transaction
                Transaction newTransaction = new Transaction( type, category, amount, date, notes);
                saveTransactionToDB(newTransaction);
                loadTransactionsFromDB();
                resetForm();
            }

        } catch (NumberFormatException e) {
            showAlert("Invalid amount. Please enter a number.");
        }
    }

    private void resetForm() {
        amountField.clear();
        notesArea.clear();
        categoryComboBox.setValue(null);
        typeComboBox.setValue("Expense");
        datePicker.setValue(LocalDate.now());
    }

    private void updateTransactionInDB(Transaction transaction) {
        String sql = "UPDATE transactions SET type = ?, category = ?, amount = ?, date = ?, notes = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, transaction.getType());
            stmt.setString(2, transaction.getCategory());
            stmt.setDouble(3, transaction.getAmount());
            stmt.setDate(4, java.sql.Date.valueOf(transaction.getDate()));
            stmt.setString(5, transaction.getNotes());
            stmt.setInt(6, transaction.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error updating transaction.");
        }
    }

    private void saveTransactionToDB(Transaction transaction) {
        if (userId <= 0) {
            showAlert("No user set. Cannot save transaction to database.");
            return;
        }
        String sql = "INSERT INTO transactions (user_id, type, category, amount, date, notes) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, transaction.getType());
            stmt.setString(3, transaction.getCategory());
            stmt.setDouble(4, transaction.getAmount());
            stmt.setDate(5, java.sql.Date.valueOf(transaction.getDate()));
            stmt.setString(6, transaction.getNotes());

            stmt.executeUpdate();

            MainLayoutController main = MainLayoutController.getInstance();
            if (main != null) main.refreshDashboard();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error saving transaction to database.");
        }
    }

    private void loadTransactionsFromDB() {
        if (userId == 0) return;

        transactions.clear();
        LocalDate today = LocalDate.now();
        String sql = "SELECT * FROM transactions WHERE user_id = ? AND date = ? ORDER BY date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(today));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String type = rs.getString("type");
                String category = rs.getString("category");
                double amount = rs.getDouble("amount");
                java.sql.Date sqlDate = rs.getDate("date");
                LocalDate date = (sqlDate != null) ? sqlDate.toLocalDate() : LocalDate.now();
                String notes = rs.getString("notes");

                Transaction transaction = new Transaction(id, type, category, amount, date, notes);
                transactions.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Failed to load transactions from database.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void setTransactionToEdit(Transaction transaction) {
        this.transactionToEdit = transaction;

        typeComboBox.setValue(transaction.getType());
        updateCategoryOptions();
        categoryComboBox.setValue(transaction.getCategory());
        amountField.setText(String.valueOf(transaction.getAmount()));
        datePicker.setValue(transaction.getDate());
        notesArea.setText(transaction.getNotes());

        // Hide the table when editing
        if (transactionTable.getParent() != null && transactionTable.getParent().getParent() != null) {
            transactionTable.getParent().getParent().setVisible(false);
            transactionTable.getParent().getParent().setManaged(false);
        }
    }

    private void setupActionColumn() {
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteButton = createIconButton(createDeleteIcon(), "delete-button");
            private final Button editButton = createIconButton(createEditIcon(), "edit-button");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(8, deleteButton, editButton);

            {
                pane.setAlignment(javafx.geometry.Pos.CENTER);
                pane.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));

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

    private Button createIconButton(javafx.scene.shape.SVGPath icon, String styleClass) {
        Button btn = new Button();
        btn.setGraphic(icon);
        btn.getStyleClass().add(styleClass);
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 4; -fx-cursor: hand;");
        return btn;
    }

    private javafx.scene.shape.SVGPath createDeleteIcon() {
        javafx.scene.shape.SVGPath deleteIcon = new javafx.scene.shape.SVGPath();
        deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        deleteIcon.setScaleX(0.55);
        deleteIcon.setScaleY(0.55);
        deleteIcon.setFill(javafx.scene.paint.Color.web("#F45B69"));
        return deleteIcon;
    }

    private javafx.scene.shape.SVGPath createEditIcon() {
        javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
        editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        editIcon.setScaleX(0.55);
        editIcon.setScaleY(0.55);
        editIcon.setFill(javafx.scene.paint.Color.web("#2F4858"));
        return editIcon;
    }

    private void handleDelete(Transaction transaction) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this transaction?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM transactions WHERE id = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, transaction.getId());
                stmt.executeUpdate();
                loadTransactionsFromDB();

                MainLayoutController main = MainLayoutController.getInstance();
                if (main != null) main.refreshDashboard();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Failed to delete transaction.");
            }
        }
    }

    private void handleEdit(Transaction transaction) {
        setTransactionToEdit(transaction);
    }


}