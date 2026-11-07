package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {

        String usersSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    password TEXT NOT NULL,
                    roles TEXT NOT NULL
                        CHECK (roles IN ('ADMIN', 'STAFF', 'CUSTOMER')),
                    status TEXT NOT NULL
                        CHECK (status IN (
                            'PENDING', 'APPROVED',
                            'REJECTED', 'DELETED'
                        ))
                )
                """;

        String logsSQL = """
                CREATE TABLE IF NOT EXISTS activity_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    action_type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                )
                """;

        String bookingsSQL = """
                CREATE TABLE IF NOT EXISTS bookings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    child_name TEXT NOT NULL,
                    session_date TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN (
                            'PENDING', 'APPROVED',
                            'COMPLETED', 'CANCELLED'
                        )),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(usersSQL);
            stmt.execute(logsSQL);
            stmt.execute(bookingsSQL);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_activity_logs_user_id
                    ON activity_logs(user_id)
                    """);

            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS
                    idx_activity_logs_timestamp
                    ON activity_logs(timestamp)
                    """);

            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Database initialization failed: " + e.getMessage(),
                    e
            );
        }
    }
}