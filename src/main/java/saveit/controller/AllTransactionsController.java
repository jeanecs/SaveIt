package saveit.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.paint.Color; // Required for IconUtil coloring
import saveit.model.DatabaseManager;
import saveit.model.Transaction; // Assuming the external model class is used
import saveit.util.IconUtil; // Assuming IconUtil is available

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

/**
 * Controller for the All Transactions page (all_transactions.fxml).
 * This version integrates database functionality, complete filtering/sorting logic,
 * and consistent table styling/action handlers (modal edit, confirmation delete).
 */
public class AllTransactionsController {

    // --- FXML Element Bindings ---
    @FXML private TextField searchInput;
    @FXML private ComboBox<String> dateRangeDropdown;
    @FXML private ComboBox<String> categoryDropdown;
    @FXML private ComboBox<String> typeDropdown;
    @FXML private ComboBox<String> sortByDropdown;

    // Summary Labels
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netBalanceLabel;
    @FXML private Label transactionsCountLabel;

    @FXML private TableView<Transaction> transactionTable;
    // FIX 1: Change dateColumn type from String to LocalDate
    @FXML private TableColumn<Transaction, LocalDate> dateColumn;
    @FXML private TableColumn<Transaction, String> categoryColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, Double> amountColumn;
    @FXML private TableColumn<Transaction, String> notesColumn;
    @FXML private TableColumn<Transaction, Void> actionsColumn;

    // --- State and Data ---
    private int userId;
    private ObservableList<Transaction> masterTransactions = FXCollections.observableArrayList();
    private FilteredList<Transaction> filteredData;
    private SortedList<Transaction> sortedData;
    // IMPORTANT: Use this formatter consistently for display and internal search
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // Default icon color for neutral elements
    private final String NEUTRAL_COLOR = "#E0E0E0";

    /**
     * Public method called by MainLayoutController to pass the logged-in user ID.
     */
    public void setUserId(int userId) {
        this.userId = userId;
        // Load data once the user ID is available
        loadAllData();
    }

    /**
     * Initializes the controller, setting up TableView and populating dropdowns/listeners.
     */
    @FXML
    public void initialize() {
        setupTableColumns();
        populateDropdowns();

        // Add listeners to re-filter and re-sort whenever input changes
        searchInput.textProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
        dateRangeDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
        categoryDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
        typeDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
        sortByDropdown.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> filterTransactions());
    }

    /**
     * Master method to load all data elements for the page.
     */
    private void loadAllData() {
        if (userId <= 0) {
            System.err.println("Error: userId not set for AllTransactionsController.");
            return;
        }
        loadTransactionData();
        loadSummaryData();
    }

    /**
     * Sets up the CellValueFactory and CellFactory for each TableColumn, applying visual styles.
     */
    private void setupTableColumns() {
        // 1. Basic Value Factories
        // dateColumn now expects a LocalDate, matching the model
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        notesColumn.setCellValueFactory(new PropertyValueFactory<>("notes"));

        // 2. Custom Cell Factories for consistent Styling

        // Date Column: Central alignment and formatting
        // FIX 2: Ensure the TableCell uses LocalDate as the item type for the Date column.
        dateColumn.setCellFactory(column -> new TableCell<Transaction, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setAlignment(Pos.CENTER);

                if (empty || date == null) {
                    setText(null);
                    setStyle(null);
                } else {
                    // Format the LocalDate object here
                    setText(dateFormatter.format(date));
                    setStyle("-fx-text-fill: #2F4858; -fx-alignment: CENTER;");
                }
            }
        });

        // Category Column: Central alignment
        categoryColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
                setStyle("-fx-text-fill: #2F4858; -fx-alignment: CENTER;");
            }
        });

        // Type Column: Colored "Chip/Pill" style (Consistent with Dashboard)
        typeColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                setAlignment(Pos.CENTER);

                if (empty || type == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label chip = new Label(type);
                // Inline styling from DashboardController
                String chipStyle = type.equalsIgnoreCase("Income")
                        ? "-fx-background-color: rgba(0,191,166,0.1); -fx-text-fill: #00BFA6; -fx-background-radius: 20; -fx-padding: 3 10 3 10; -fx-font-weight: normal;"
                        : "-fx-background-color: rgba(249,168,38,0.1); -fx-text-fill: #F9A826; -fx-background-radius: 20; -fx-padding: 3 10 3 10; -fx-font-weight: normal;";

                chip.setStyle(chipStyle);
                setGraphic(chip);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Amount Column: Currency formatting, color-coding, and central alignment (Consistent with Dashboard)
        amountColumn.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                setAlignment(Pos.CENTER); // Dashboard uses CENTER, not RIGHT

                if (empty || amount == null) {
                    setText(null);
                    setStyle(null); // Clear style
                } else {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    String type = transaction.getType();

                    setText("\u20B1" + String.format("%,.2f", amount));

                    // Inline styling from DashboardController
                    setStyle(
                            type.equalsIgnoreCase("Income")
                                    ? "-fx-text-fill: #00BFA6; -fx-alignment: CENTER;"
                                    : "-fx-text-fill: #F9A826; -fx-alignment: CENTER;"
                    );
                }
            }
        });

        // Notes Column: Central alignment
        notesColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER);
                setStyle("-fx-text-fill: #2F4858; -fx-alignment: CENTER;");
            }
        });

        // Actions Column: Buttons
        actionsColumn.setCellFactory(getActionsCellFactory());

        // Ensure table header alignment is consistent (Optional, but good practice)
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

    /**
     * Helper to create styled icon buttons using IconUtil (Consistent with Dashboard).
     */
    private Button createIconButton(SVGPath icon, String styleClass) {
        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add("icon-btn"); // Base style
        if (styleClass.contains("delete")) {
            button.getStyleClass().add("delete-icon-btn");
        }
        return button;
    }

    /**
     * Creates the CellFactory for the Actions column to insert Edit and Delete buttons (Consistent with Dashboard).
     */
    private Callback<TableColumn<Transaction, Void>, TableCell<Transaction, Void>> getActionsCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Transaction, Void> call(final TableColumn<Transaction, Void> param) {
                final TableCell<Transaction, Void> cell = new TableCell<>() {

                    private final Button deleteButton = createIconButton(IconUtil.deleteIcon(0.8, Color.web(NEUTRAL_COLOR)), "delete-button");
                    private final Button editButton = createIconButton(IconUtil.editIcon(0.8, Color.web(NEUTRAL_COLOR)), "edit-button");
                    private final HBox actionsBox = new HBox(8, editButton, deleteButton);


                    {
                        actionsBox.setAlignment(Pos.CENTER);
                        actionsBox.setPadding(new Insets(5, 0, 5, 0));


                        // Set up button actions
                        editButton.setOnAction(event -> {
                            Transaction data = getTableView().getItems().get(getIndex());
                            handleEditTransaction(data);
                        });

                        deleteButton.setOnAction(event -> {
                            Transaction data = getTableView().getItems().get(getIndex());
                            handleDeleteTransaction(data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(actionsBox);
                        }
                    }
                };
                return cell;
            }
        };
    }

    /**
     * Populates the ComboBoxes with options.
     */
    private void populateDropdowns() {
        dateRangeDropdown.setItems(FXCollections.observableArrayList("All Time", "This Month", "Last Month"));
        dateRangeDropdown.getSelectionModel().selectFirst();

        // Note: Combined all possible categories from both controllers for a complete list
        categoryDropdown.setItems(FXCollections.observableArrayList(
                "All Categories", "Food", "Salary", "Bills", "Transportation",
                "Entertainment", "Freelance", "Shopping", "Investment",
                "Bonus", "Other Income", "Healthcare", "Education", "Other"
        ));
        categoryDropdown.getSelectionModel().selectFirst();

        typeDropdown.setItems(FXCollections.observableArrayList("All Types", "Income", "Expense"));
        typeDropdown.getSelectionModel().selectFirst();

        sortByDropdown.setItems(FXCollections.observableArrayList("Date (Newest)", "Date (Oldest)", "Amount (Highest)", "Amount (Lowest)", "Category"));
        sortByDropdown.getSelectionModel().selectFirst();
    }

    /**
     * Loads the transaction data from the database for the logged-in user and initializes lists.
     */
    private void loadTransactionData() {
        masterTransactions.clear();
        // The Transaction model must have a constructor matching: (int id, String type, String category, double amount, LocalDate date, String notes)
        String sql = "SELECT id, date, category, type, amount, notes FROM transactions WHERE user_id = ? ORDER BY date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // FIX: Use rs.getDate("date") for reliable conversion of SQL DATE type
                Date sqlDate = rs.getDate("date");
                LocalDate transactionDate = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                // Instantiate the Transaction model object
                Transaction transaction = new Transaction(
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getDouble("amount"),
                        transactionDate, // Using the correctly converted LocalDate
                        rs.getString("notes")
                );
                masterTransactions.add(transaction);
            }

            // 1. Initialize FilteredList with the master data
            filteredData = new FilteredList<>(masterTransactions, p -> true); // Start showing all data

            // 2. Initialize SortedList based on the filtered data
            sortedData = new SortedList<>(filteredData);

            // 3. Set the sorted data to the TableView
            transactionTable.setItems(sortedData);

            // Apply initial sort/filter state
            filterTransactions();

        } catch (SQLException e) {
            System.err.println("Error loading transactions from database: " + e.getMessage());
        }
    }
    /**
     * Calculates and loads the total income, expenses, and net balance from the database.
     */
    private void loadSummaryData() {
        String sql = "SELECT " +
                "SUM(CASE WHEN type = 'Income' THEN amount ELSE 0 END) AS total_income, " +
                "SUM(CASE WHEN type = 'Expense' THEN amount ELSE 0 END) AS total_expense " +
                "FROM transactions WHERE user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double totalIncome = rs.getDouble("total_income");
                double totalExpense = rs.getDouble("total_expense");
                double netBalance = totalIncome - totalExpense;

                // Update summary labels
                totalIncomeLabel.setText("\u20B1" + String.format("%,.2f", totalIncome));
                totalExpensesLabel.setText("\u20B1" + String.format("%,.2f", totalExpense));

                // Set net balance color based on value (using CSS classes defined in allTransactions.css)
                netBalanceLabel.setText("\u20B1" + String.format("%,.2f", netBalance));
                if (netBalance >= 0) {
                    // net-color and income-color (for positive/zero)
                    netBalanceLabel.getStyleClass().setAll("summary-value", "net-color", "income-color");
                } else {
                    // net-color and expense-color (for negative)
                    netBalanceLabel.getStyleClass().setAll("summary-value", "net-color", "expense-color");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error loading summary data: " + e.getMessage());
        }
    }

    /**
     * Filters and sorts the transaction data based on current search and dropdown selections.
     */
    private void filterTransactions() {
        String search = searchInput.getText().toLowerCase();
        String dateRange = dateRangeDropdown.getValue();
        String category = categoryDropdown.getValue();
        String type = typeDropdown.getValue();
        String sortBy = sortByDropdown.getValue();

        // 1. Filtering Logic
        filteredData.setPredicate(transaction -> {
            if (transaction == null) return false;

            // --- A. Search Filter ---
            if (!search.isEmpty()) {
                // Ensure date is checked correctly against the formatted string
                String formattedDate = transaction.getDate() != null ? transaction.getDate().format(dateFormatter).toLowerCase() : "";
                if (!transaction.getCategory().toLowerCase().contains(search) &&
                        !transaction.getNotes().toLowerCase().contains(search) &&
                        !formattedDate.contains(search)) {
                    return false;
                }
            }

            // --- B. Category Filter ---
            if (category != null && !category.equals("All Categories")) {
                if (!transaction.getCategory().equals(category)) {
                    return false;
                }
            }

            // --- C. Type Filter (Income/Expense) ---
            if (type != null && !type.equals("All Types")) {
                if (!transaction.getType().equals(type)) {
                    return false;
                }
            }

            // --- D. Date Range Filter ---
            if (dateRange != null && !dateRange.equals("All Time")) {
                LocalDate transactionDate = transaction.getDate();
                if (transactionDate == null) return false; // Skip if date is null

                LocalDate now = LocalDate.now();
                LocalDate filterStart = null;
                LocalDate filterEnd = now; // Default end date is today

                if (dateRange.equals("This Month")) {
                    filterStart = now.with(TemporalAdjusters.firstDayOfMonth());
                } else if (dateRange.equals("Last Month")) {
                    LocalDate lastMonth = now.minusMonths(1);
                    filterStart = lastMonth.with(TemporalAdjusters.firstDayOfMonth());
                    filterEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth());
                }

                if (filterStart != null) {
                    if (transactionDate.isBefore(filterStart) || transactionDate.isAfter(filterEnd)) {
                        return false;
                    }
                }
            }

            return true; // Passed all filters
        });

        // 2. Sorting Logic (Custom sorting for ComboBox)
        if (sortBy != null) {
            Comparator<Transaction> comparator;
            switch (sortBy) {
                case "Amount (Highest)":
                    comparator = Comparator.comparing(Transaction::getAmount).reversed();
                    break;
                case "Amount (Lowest)":
                    comparator = Comparator.comparing(Transaction::getAmount);
                    break;
                case "Category":
                    comparator = Comparator.comparing(Transaction::getCategory);
                    break;
                case "Date (Oldest)":
                    // Handle null dates for sorting gracefully
                    comparator = Comparator.comparing(Transaction::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "Date (Newest)":
                default:
                    // Handle null dates for sorting gracefully
                    comparator = Comparator.comparing(Transaction::getDate, Comparator.nullsLast(Comparator.reverseOrder()));
                    break;
            }
            // Apply the custom comparator to the sorted list
            sortedData.setComparator(comparator);
        } else {
            // Default sort to Date (Newest) if nothing is selected
            sortedData.setComparator(Comparator.comparing(Transaction::getDate, Comparator.nullsLast(Comparator.reverseOrder())));
        }


        // 3. Update count label
        transactionsCountLabel.setText(filteredData.size() + " Transactions");
    }


    // ===================================
    // Button Action Handlers (Consistent with Dashboard)
    // ===================================

    @FXML
    private void handleExport() {
        // NOTE: Changed from Alert to System.out message as per constraints.
        System.out.println("ACTION: Transaction data successfully exported! (Mock Action)");
    }

    /**
     * Opens the AddTransaction modal pre-populated with the transaction data for editing.
     */
    private void handleEditTransaction(Transaction transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML/add_transaction.fxml"));
            Parent root = loader.load();

            AddTransactionController controller = loader.getController();
            controller.setUserId(userId);
            controller.setTransactionToEdit(transaction); // Set the transaction to edit

            Stage stage = new Stage();
            stage.setTitle("Edit Transaction");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(transactionTable.getScene().getWindow());

            stage.showAndWait(); // Wait for the edit window to close

            loadAllData(); // Refresh data after editing (includes table and summaries)

        } catch (IOException e) {
            System.err.println("Error opening edit transaction modal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shows a confirmation alert before deleting a transaction.
     */
    private void handleDeleteTransaction(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Transaction");
        alert.setHeaderText("Are you sure you want to delete this transaction?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (deleteTransactionFromDatabase(transaction.getId())) {
                // Refresh data after successful deletion
                loadAllData();
                System.out.println("Transaction " + transaction.getId() + " deleted successfully.");
            } else {
                System.err.println("Failed to delete transaction from database.");
                // Optionally show error alert here
            }
        }
    }

    /**
     * Performs the actual database deletion.
     */
    private boolean deleteTransactionFromDatabase(int transactionId) {
        String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, transactionId);
            stmt.setInt(2, userId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Database deletion error: " + e.getMessage());
            // Optionally show error alert here
            return false;
        }
    }
}