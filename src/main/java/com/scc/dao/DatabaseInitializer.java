
package com.scc.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() {
        String sql = """
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

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Could not initialize the database: "
                            + e.getMessage(), e
            );
        }
    }
}
