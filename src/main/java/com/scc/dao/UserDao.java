package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public UserDao() {
        createDefaultAdmin();
    }

    // CREATE DEFAULT ADMIN
    private void createDefaultAdmin() {

        String sql = """
                INSERT INTO users (username, password, roles, status)
                SELECT ?, ?, 'ADMIN', 'APPROVED'
                WHERE NOT EXISTS (
                    SELECT 1 FROM users WHERE roles = 'ADMIN'
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "admin");
            stmt.setString(2, "admin123");

            stmt.executeUpdate();

        } catch (SQLException e) {

            System.err.println(
                    "Could not create default admin: " + e.getMessage()
            );
        }
    }

    // REGISTER USER
    public boolean registerUser(
            String username, String password, String role) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || role == null) {

            System.out.println("All fields are required.");
            return false;
        }

        username = username.trim();
        role = role.trim().toUpperCase();

        if (!role.equals("STAFF") && !role.equals("CUSTOMER")) {

            System.out.println(
                    "Only STAFF and CUSTOMER registration is allowed."
            );

            return false;
        }

        if (password.length() < 8) {

            System.out.println(
                    "Password must contain at least 8 characters."
            );

            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);

            stmt.executeUpdate();

            System.out.println(
                    "Registration successful. Awaiting admin approval."
            );

            return true;

        } catch (SQLException e) {

            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("unique")) {

                System.out.println("Username already exists.");

            } else {

                System.err.println(
                        "Registration error: " + e.getMessage()
                );
            }

            return false;
        }
    }

    // LOGIN USER
    public User login(String username, String password) {

        if (username == null || username.isBlank()
                || password == null) {

            System.out.println("Username and password are required.");
            return null;
        }

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ? AND password = ?
                  AND status != 'DELETED'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {

                if (!rs.next()) {

                    System.out.println(
                            "Invalid username or password."
                    );

                    return null;
                }

                String status = rs.getString("status");

                if (!status.equalsIgnoreCase("APPROVED")) {

                    System.out.println(
                            switch (status.toUpperCase()) {
                                case "PENDING" ->
                                        "Your account is pending approval.";

                                case "REJECTED" ->
                                        "Your account was rejected.";

                                default ->
                                        "Your account is not active.";
                            }
                    );

                    return null;
                }

                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        status
                );
            }

        } catch (SQLException e) {

            System.err.println(
                    "Login error: " + e.getMessage()
            );

            return null;
        }
    }

    // GET ALL USERS
    public List<User> getAllUsers() {

        List<User> users = new ArrayList<>();

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE status != 'DELETED'
                ORDER BY id
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }

        } catch (SQLException e) {

            System.err.println(
                    "List error: " + e.getMessage()
            );
        }

        return users;
    }

    // UPDATE USER ROLE AND STATUS
    public boolean updateUserRoleAndStatus(
            int userId, String newRole, String newStatus) {

        if (newRole == null || newStatus == null) {

            System.out.println("Role and status are required.");
            return false;
        }

        newRole = newRole.trim().toUpperCase();
        newStatus = newStatus.trim().toUpperCase();

        // Validate role
        if (!newRole.equals("ADMIN")
                && !newRole.equals("STAFF")
                && !newRole.equals("CUSTOMER")) {

            System.out.println("Invalid role.");
            return false;
        }

        // Validate status
        if (!newStatus.equals("APPROVED")
                && !newStatus.equals("PENDING")) {

            System.out.println("Invalid status.");
            return false;
        }

        String sql = """
                UPDATE users
                SET roles = ?, status = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole);
            stmt.setString(2, newStatus);
            stmt.setInt(3, userId);

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {

                System.out.println(
                        "User role and status updated successfully!"
                );

                return true;
            }

            System.out.println("User ID not found.");
            return false;

        } catch (SQLException e) {

            System.err.println(
                    "Update error: " + e.getMessage()
            );

            return false;
        }
    }

    // UPDATE USER STATUS
    public boolean updateUserStatus(int id, String status) {

        if (status == null) {
            System.out.println("Invalid status.");
            return false;
        }

        status = status.trim().toUpperCase();

        if (!status.equals("APPROVED")
                && !status.equals("REJECTED")) {

            System.out.println("Invalid status.");
            return false;
        }

        String sql = """
                UPDATE users
                SET status = ?
                WHERE id = ?
                  AND roles != 'ADMIN'
                  AND status != 'DELETED'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, id);

            if (stmt.executeUpdate() > 0) {

                System.out.println("Account status updated.");
                return true;
            }

            System.out.println(
                    "Account not found or cannot be changed."
            );

            return false;

        } catch (SQLException e) {

            System.err.println(
                    "Update error: " + e.getMessage()
            );

            return false;
        }
    }

    // SOFT DELETE USER
    public boolean softDeleteUser(int id) {

        String sql = """
                UPDATE users
                SET status = 'DELETED'
                WHERE id = ?
                  AND roles != 'ADMIN'
                  AND status != 'DELETED'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            if (stmt.executeUpdate() > 0) {

                System.out.println(
                        "Account soft-deleted successfully."
                );

                return true;
            }

            System.out.println(
                    "Account not found or cannot be deleted."
            );

            return false;

        } catch (SQLException e) {

            System.err.println(
                    "Delete error: " + e.getMessage()
            );

            return false;
        }
    }
}