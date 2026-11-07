package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingDao {

    private final ActivityLogDao activityLogDao =
            new ActivityLogDao();

    // CREATE PLAYGROUND BOOKING
    public boolean createBooking(
            int userId,
            String childName,
            String sessionDate
    ) {

        if (childName == null || childName.isBlank()
                || sessionDate == null || sessionDate.isBlank()) {

            System.out.println("All booking fields are required.");
            return false;
        }

        String sql = """
                INSERT INTO bookings
                (user_id, child_name, session_date, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, childName.trim());
                stmt.setString(3, sessionDate.trim());

                stmt.executeUpdate();
            }

            int bookingId;

            try (PreparedStatement stmt =
                         conn.prepareStatement("SELECT last_insert_rowid()");
                 ResultSet rs = stmt.executeQuery()) {

                rs.next();
                bookingId = rs.getInt(1);
            }

            activityLogDao.logAction(
                    conn,
                    userId,
                    "BOOKING_CREATED",
                    "Created playground booking ID " + bookingId
                            + " for " + childName
                            + " on " + sessionDate
            );

            conn.commit();

            System.out.println(
                    "Booking created successfully. Booking ID: " + bookingId
            );

            return true;

        } catch (SQLException e) {

            System.err.println("Booking error: " + e.getMessage());
            return false;
        }
    }

    // UPDATE BOOKING STATUS
    public boolean updateBookingStatus(
            int bookingId,
            String newStatus,
            int userId
    ) {

        if (newStatus == null) {
            return false;
        }

        newStatus = newStatus.trim().toUpperCase();

        if (!newStatus.equals("PENDING")
                && !newStatus.equals("APPROVED")
                && !newStatus.equals("COMPLETED")
                && !newStatus.equals("CANCELLED")) {

            System.out.println("Invalid booking status.");
            return false;
        }

        String sql = """
                UPDATE bookings
                SET status = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            String oldStatus;

            try (PreparedStatement check =
                         conn.prepareStatement(
                                 "SELECT status FROM bookings WHERE id = ?")) {

                check.setInt(1, bookingId);

                try (ResultSet rs = check.executeQuery()) {

                    if (!rs.next()) {
                        conn.rollback();
                        System.out.println("Booking not found.");
                        return false;
                    }

                    oldStatus = rs.getString("status");
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, newStatus);
                stmt.setInt(2, bookingId);
                stmt.executeUpdate();
            }

            activityLogDao.logAction(
                    conn,
                    userId,
                    "BOOKING_STATUS_UPDATED",
                    "Updated booking ID " + bookingId
                            + " status from " + oldStatus
                            + " to " + newStatus
            );

            conn.commit();

            System.out.println("Booking status updated successfully.");
            return true;

        } catch (SQLException e) {

            System.err.println("Booking update error: " + e.getMessage());
            return false;
        }
    }
}