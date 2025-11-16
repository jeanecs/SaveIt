package saveit.model;

import java.time.LocalDate;

public class Transaction {
    private String category;
    private String type;
    private double amount;
    private LocalDate date;
    private String notes;

    public Transaction(String type, String category, double amount, LocalDate date, String notes) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getNotes() {
        return notes;
    }
}
