package saveit.model;

/**
 * Represents a user in the application, mirroring the 'users' table structure.
 */
public class User {
    private int id; // Corresponds to the primary key in the database
    private String username;
    private String email; // New field for uniqueness and registration
    private String password; // Should generally only be used for login/registration purposes

    /**
     * Constructor used when retrieving a user from the database.
     */
    public User(int id, String username, String email, String password) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    // IMPORTANT: In a production app, avoid returning the raw password.
    // This getter is mainly kept for compatibility with current logic,
    // but should only be used carefully (e.g., during validation check).
    public String getPassword() {
        return password;
    }
}