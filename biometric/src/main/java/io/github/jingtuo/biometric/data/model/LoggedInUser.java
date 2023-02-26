package io.github.jingtuo.biometric.data.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 *
 * @author JingTuo
 */
public class LoggedInUser {

    private String userId;
    private String username;

    private String password;

    public LoggedInUser(String userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}