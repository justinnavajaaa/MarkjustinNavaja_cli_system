package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    // LOG USING AN EXISTING CONNECTION (part of a caller's transaction)
    public void logAction(
            Connection conn,
            int userId,
            String actionType,
            String description
    ) throws SQLException {

        String sql = """
                INSERT INTO activity_logs
                (user_id, action_type, description)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, actionType);
            stmt.setString(3, description);
            stmt.executeUpdate();
        }
    }

    // LOG USING ITS OWN CONNECTION
    public void logAction(
            int userId,
            String actionType,
            String description
    ) throws SQLException {

        try (Connection conn = DatabaseConnection.getConnection()) {
            logAction(conn, userId, actionType, description);
        }
    }

    // GET ALL LOGS (NEWEST FIRST)
    public List<ActivityLog> getAllLogs() throws SQLException {

        String sql = """
                SELECT id, user_id, action_type, description, timestamp
                FROM activity_logs
                ORDER BY id DESC
                """;

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        }

        return logs;
    }

    // GET LOGS FOR ONE USER
    public List<ActivityLog> getLogsByUserId(int userId) throws SQLException {

        String sql = """
                SELECT id, user_id, action_type, description, timestamp
                FROM activity_logs
                WHERE user_id = ?
                ORDER BY id DESC
                """;

        List<ActivityLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }

        return logs;
    }

    // CLEAR ALL LOGS
    public void clearAllLogs() throws SQLException {

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("DELETE FROM activity_logs");
        }
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {

        return new ActivityLog(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("action_type"),
                rs.getString("description"),
                rs.getString("timestamp")
        );
    }
}