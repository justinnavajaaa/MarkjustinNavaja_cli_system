package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.dao.ActivityLogDao;
import com.scc.dao.BookingDao;
import com.scc.model.User;
import com.scc.model.ActivityLog;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;
    private final BookingDao bookingDao;
    private final Scanner sc;
    private final User currentAdmin;

    public AdminMenu(
            UserDao userDao,
            Scanner sc,
            User currentAdmin
    ) {
        this.userDao = userDao;
        this.activityLogDao = userDao.getActivityLogDao();
        this.bookingDao = new BookingDao();
        this.sc = sc;
        this.currentAdmin = currentAdmin;
    }

    // ADMIN DASHBOARD
    public void showMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n====================================");
            System.out.println("          ADMIN DASHBOARD");
            System.out.println("====================================");

            System.out.println("1. View accounts");
            System.out.println("2. Update user role and status");
            System.out.println("3. Approve account");
            System.out.println("4. Reject account");
            System.out.println("5. Soft-delete account");
            System.out.println("6. View System Audit Logs");
            System.out.println("7. Create Playground Booking");
            System.out.println("8. Update Booking Status");
            System.out.println("9. Logout");

            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> displayUsers();

                case "2" -> updateUser();

                case "3" -> changeStatus("APPROVED");

                case "4" -> changeStatus("REJECTED");

                case "5" -> deleteAccount();

                case "6" -> showAuditLogMenu();

                case "7" -> createBooking();

                case "8" -> updateBooking();

                case "9" -> {
                    userDao.logLogout(currentAdmin);
                    System.out.println("Admin logged out.");
                    running = false;
                }

                default -> System.out.println("Invalid option.");
            }
        }
    }

    // DISPLAY USERS
    private void displayUsers() {

        List<User> users = userDao.getAllUsers();

        System.out.println("\n========== ACCOUNT LIST ==========");

        if (users.isEmpty()) {
            System.out.println("No accounts found.");
            return;
        }

        System.out.printf(
                "%-6s %-20s %-12s %-12s%n",
                "ID", "USERNAME", "ROLE", "STATUS"
        );

        System.out.println("-----------------------------------------------");

        for (User user : users) {

            System.out.printf(
                    "%-6d %-20s %-12s %-12s%n",
                    user.id(),
                    user.username(),
                    user.roles(),
                    user.status()
            );
        }
    }

    // UPDATE USER
    private void updateUser() {

        displayUsers();

        int id = readInt("Enter User ID to update (0 to cancel): ");

        if (id == 0) {
            return;
        }

        if (id == currentAdmin.id()) {
            System.out.println("Modifying the active admin is restricted.");
            return;
        }

        boolean exists = userDao.getAllUsers()
                .stream()
                .anyMatch(user -> user.id() == id);

        if (!exists) {
            System.out.println("User ID not found.");
            return;
        }

        String role;

        while (true) {

            System.out.println("\n1. ADMIN");
            System.out.println("2. STAFF");
            System.out.println("3. CUSTOMER");

            System.out.print("Choose new role (0 to cancel): ");

            String choice = sc.nextLine().trim();

            role = switch (choice) {
                case "1" -> "ADMIN";
                case "2" -> "STAFF";
                case "3" -> "CUSTOMER";
                default -> null;
            };

            if (choice.equals("0")) {
                return;
            }

            if (role != null) {
                break;
            }

            System.out.println("Invalid role.");
        }

        System.out.println("\n1. APPROVED");
        System.out.println("2. PENDING");

        System.out.print("Choose new status (0 to cancel): ");

        String statusChoice = sc.nextLine().trim();

        String status = switch (statusChoice) {
            case "1" -> "APPROVED";
            case "2" -> "PENDING";
            default -> null;
        };

        if (status == null) {
            System.out.println("Update cancelled.");
            return;
        }

        userDao.updateUserRoleAndStatus(
                id,
                role,
                status,
                currentAdmin.id()
        );
    }

    // APPROVE OR REJECT ACCOUNT
    private void changeStatus(String status) {

        displayUsers();

        int id = readInt("Enter account ID (0 to cancel): ");

        if (id == 0) {
            return;
        }

        if (id == currentAdmin.id()) {
            System.out.println("Modifying the active admin is restricted.");
            return;
        }

        userDao.updateUserStatus(
                id,
                status,
                currentAdmin.id()
        );
    }

    // SOFT DELETE ACCOUNT
    private void deleteAccount() {

        displayUsers();

        int id = readInt("Enter account ID to delete (0 to cancel): ");

        if (id == 0) {
            return;
        }

        if (id == currentAdmin.id()) {
            System.out.println("You cannot delete the active admin.");
            return;
        }

        System.out.print("Confirm deletion? (Y/N): ");

        String answer = sc.nextLine().trim();

        if (answer.equalsIgnoreCase("Y")) {

            userDao.softDeleteUser(
                    id,
                    currentAdmin.id()
            );

        } else {
            System.out.println("Delete cancelled.");
        }
    }

    // AUDIT LOG MENU
    private void showAuditLogMenu() {

        boolean running = true;

        while (running) {

            System.out.println("\n====================================");
            System.out.println("          SYSTEM AUDIT LOGS");
            System.out.println("====================================");

            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Logs");
            System.out.println("4. Back");

            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> viewAllLogs();

                case "2" -> {

                    int userId = readInt("Enter User ID: ");

                    if (userId > 0) {
                        viewLogsByUserId(userId);
                    }
                }

                case "3" -> clearLogs();

                case "4" -> running = false;

                default -> System.out.println("Invalid option.");
            }
        }
    }

    // VIEW ALL LOGS
    private void viewAllLogs() {

        try {

            displayLogs(activityLogDao.getAllLogs());

        } catch (SQLException e) {

            System.err.println(
                    "Could not retrieve logs: " + e.getMessage()
            );
        }
    }

    // FILTER LOGS BY USER ID
    private void viewLogsByUserId(int userId) {

        try {

            displayLogs(
                    activityLogDao.getLogsByUserId(userId)
            );

        } catch (SQLException e) {

            System.err.println(
                    "Could not retrieve logs: " + e.getMessage()
            );
        }
    }

    // READABLE ACTIVITY LOG DISPLAY
    private void displayLogs(List<ActivityLog> logs) {

        System.out.println(
                "\n====================== SYSTEM ACTIVITY LOGS ======================"
        );

        if (logs.isEmpty()) {

            System.out.println("No activity logs found.");
            return;
        }

        for (ActivityLog log : logs) {

            System.out.println(
                    "------------------------------------------------------------------"
            );

            System.out.println("Log ID      : " + log.id());

            System.out.println("User ID     : " + log.userId());

            System.out.println("Action      : " + log.actionType());

            System.out.println("Description : " + log.description());

            System.out.println("Timestamp   : " + log.timestamp());
        }

        System.out.println(
                "------------------------------------------------------------------"
        );

        System.out.println("Total records: " + logs.size());
    }

    // CLEAR HISTORICAL LOGS
    private void clearLogs() {

        System.out.print(
                "Are you sure you want to permanently clear all logs? (Y/N): "
        );

        String answer = sc.nextLine().trim();

        if (!answer.equalsIgnoreCase("Y")) {

            System.out.println("Clear operation cancelled.");
            return;
        }

        try {

            activityLogDao.clearAllLogs();

            System.out.println("Historical activity logs cleared.");

        } catch (SQLException e) {

            System.err.println(
                    "Could not clear logs: " + e.getMessage()
            );
        }
    }

    // CREATE PLAYGROUND BOOKING
    private void createBooking() {

        System.out.println("\n========== CREATE PLAYGROUND BOOKING ==========");

        System.out.print("Child name: ");

        String childName = sc.nextLine().trim();

        System.out.print("Session date (YYYY-MM-DD): ");

        String sessionDate = sc.nextLine().trim();

        bookingDao.createBooking(
                currentAdmin.id(),
                childName,
                sessionDate
        );
    }

    // UPDATE BOOKING STATUS
    private void updateBooking() {

        System.out.println("\n========== UPDATE BOOKING STATUS ==========");

        int bookingId = readInt("Enter Booking ID: ");

        if (bookingId <= 0) {

            System.out.println("Invalid booking ID.");
            return;
        }

        System.out.println("1. PENDING");
        System.out.println("2. APPROVED");
        System.out.println("3. COMPLETED");
        System.out.println("4. CANCELLED");

        System.out.print("Choose new status: ");

        String choice = sc.nextLine().trim();

        String status = switch (choice) {
            case "1" -> "PENDING";
            case "2" -> "APPROVED";
            case "3" -> "COMPLETED";
            case "4" -> "CANCELLED";
            default -> null;
        };

        if (status == null) {

            System.out.println("Invalid status.");
            return;
        }

        bookingDao.updateBookingStatus(
                bookingId,
                status,
                currentAdmin.id()
        );
    }

    // INTEGER INPUT VALIDATION
    private int readInt(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = sc.nextLine().trim();

            try {

                int number = Integer.parseInt(input);

                if (number < 0) {

                    System.out.println(
                            "Enter 0 or a positive number."
                    );

                    continue;
                }

                return number;

            } catch (NumberFormatException e) {

                System.out.println(
                        "Please enter numbers only."
                );
            }
        }
    }
}