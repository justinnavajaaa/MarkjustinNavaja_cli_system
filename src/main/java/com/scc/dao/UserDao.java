package com.scc.dao;

import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao activityLogDao =
            new ActivityLogDao();

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
            System.err.println("Default admin error: " + e.getMessage());
        }
    }

    // REGISTER USER
    public boolean registerUser(
            String username,
            String password,
            String role
    ) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || role == null) {

            System.out.println("All fields are required.");
            return false;
        }

        username = username.trim();
        role = role.trim().toUpperCase();

        if (!role.equals("STAFF") && !role.equals("CUSTOMER")) {
            System.out.println("Invalid registration role.");
            return false;
        }

        if (password.length() < 8) {
            System.out.println("Password must contain at least 8 characters.");
            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, role);
                stmt.executeUpdate();
            }

            int newUserId;

            try (PreparedStatement stmt =
                         conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = stmt.executeQuery()) {

                rs.next();
                newUserId = rs.getInt(1);
            }

            activityLogDao.logAction(
                    conn,
                    newUserId,
                    "USER_REGISTERED",
                    "New " + role + " account registered: " + username
            );

            conn.commit();

            System.out.println(
                    "Registration successful. Awaiting admin approval."
            );

            return true;

        } catch (SQLException e) {

            System.err.println("Registration error: " + e.getMessage());
            return false;
        }
    }

    // LOGIN USER
    public User login(String username, String password) {

        if (username == null || username.isBlank() || password == null) {
            System.out.println("Username and password are required.");
            return null;
        }

        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ?
                  AND password = ?
                  AND status != 'DELETED'
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());
            stmt.setString(2, password);

            User user = null;
            String status = null;

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    status = rs.getString("status");

                    user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("roles"),
                            status
                    );
                }
            }

            if (user == null) {
                System.out.println("Invalid username or password.");
                return null;
            }

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

            activityLogDao.logAction(
                    conn,
                    user.id(),
                    "USER_LOGIN",
                    "User " + user.username()
                            + " (ID: " + user.id() + ") logged in."
            );

            return user;

        } catch (SQLException e) {

            System.err.println("Login error: " + e.getMessage());
            return null;
        }
    }

    // LOGOUT
    public void logLogout(User user) {

        if (user == null) {
            return;
        }

        try {
            activityLogDao.logAction(
                    user.id(),
                    "USER_LOGOUT",
                    "User " + user.username()
                            + " (ID: " + user.id() + ") logged out."
            );
        } catch (SQLException e) {
            System.err.println("Logout logging error: " + e.getMessage());
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
            System.err.println("List error: " + e.getMessage());
        }

        return users;
    }

    // UPDATE ROLE AND STATUS
    public boolean updateUserRoleAndStatus(
            int userId,
            String newRole,
            String newStatus,
            int adminId
    ) {

        if (newRole == null || newStatus == null) {
            System.out.println("Role and status are required.");
            return false;
        }

        newRole = newRole.trim().toUpperCase();
        newStatus = newStatus.trim().toUpperCase();

        if (!List.of("ADMIN", "STAFF", "CUSTOMER").contains(newRole)) {
            System.out.println("Invalid role.");
            return false;
        }

        if (!List.of("APPROVED", "PENDING").contains(newStatus)) {
            System.out.println("Invalid status.");
            return false;
        }

        String sql = """
                UPDATE users
                SET roles = ?, status = ?
                WHERE id = ? AND status != 'DELETED'
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newRole);
                stmt.setString(2, newStatus);
                stmt.setInt(3, userId);

                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    System.out.println("User ID not found.");
                    return false;
                }
            }

            activityLogDao.logAction(
                    conn,
                    adminId,
                    "USER_UPDATED",
                    "Admin ID " + adminId
                            + " changed User ID " + userId
                            + " to role " + newRole
                            + " and status " + newStatus
            );

            conn.commit();
            System.out.println("User role and status updated successfully!");
            return true;

        } catch (SQLException e) {
            System.err.println("Update error: " + e.getMessage());
            return false;
        }
    }

    // APPROVE OR REJECT ACCOUNT
    public boolean updateUserStatus(
            int id,
            String status,
            int adminId
    ) {

        if (status == null) {
            return false;
        }

        status = status.trim().toUpperCase();

        if (!List.of("APPROVED", "REJECTED").contains(status)) {
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

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, status);
                stmt.setInt(2, id);

                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    System.out.println("Account not found or cannot be changed.");
                    return false;
                }
            }

            String action = status.equals("APPROVED")
                    ? "USER_APPROVED"
                    : "USER_REJECTED";

            activityLogDao.logAction(
                    conn,
                    adminId,
                    action,
                    "Admin " + adminId + " changed User ID "
                            + id + " status to " + status
            );

            conn.commit();
            System.out.println("Account status updated.");
            return true;

        } catch (SQLException e) {
            System.err.println("Status update error: " + e.getMessage());
            return false;
        }
    }

    // SOFT DELETE USER
    public boolean softDeleteUser(int id, int adminId) {

        String sql = """
                UPDATE users
                SET status = 'DELETED'
                WHERE id = ?
                  AND roles != 'ADMIN'
                  AND status != 'DELETED'
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);

                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    System.out.println("Account not found or cannot be deleted.");
                    return false;
                }
            }

            activityLogDao.logAction(
                    conn,
                    adminId,
                    "USER_DELETED",
                    "Admin " + adminId + " soft-deleted User ID " + id
            );

            conn.commit();
            System.out.println("Account soft-deleted successfully.");
            return true;

        } catch (SQLException e) {
            System.err.println("Delete error: " + e.getMessage());
            return false;
        }
    }

    public ActivityLogDao getActivityLogDao() {
        return activityLogDao;
    }
}