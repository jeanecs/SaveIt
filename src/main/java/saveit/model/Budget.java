package saveit.model;

public class Budget {
    private int id;
    private int userId;
    private String category;
    private double limit;
    private String period;

    public Budget(int id, int userId, String category, double limit, String period) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.limit = limit;
        this.period = period;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getCategory() { return category; }
    public double getLimit() { return limit; }
    public String getPeriod() { return period; }

    public void setCategory(String category) { this.category = category; }
    public void setLimit(double limit) { this.limit = limit; }
    public void setPeriod(String period) { this.period = period; }
}
