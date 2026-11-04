package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final UserDao userDao;
    private final Scanner sc;
    private final User currentAdmin;

    public AdminMenu(
            UserDao userDao,
            Scanner sc,
            User currentAdmin) {

        this.userDao = userDao;
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
            System.out.println("6. Logout");

            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> displayUsers();

                case "2" -> updateUser();

                case "3" -> changeStatus("APPROVED");

                case "4" -> changeStatus("REJECTED");

                case "5" -> deleteAccount();

                case "6" -> {
                    System.out.println("Admin logged out.");
                    running = false;
                }

                default -> System.out.println(
                        "Invalid option. Please try again."
                );
            }
        }
    }

    // DISPLAY ALL USERS
    public void displayUsers() {

        List<User> users = userDao.getAllUsers();

        System.out.println("\n========== ACCOUNT LIST ==========");

        if (users.isEmpty()) {

            System.out.println("No accounts found.");
            return;
        }

        System.out.printf(
                "%-6s %-20s %-12s %-12s%n",
                "ID",
                "USERNAME",
                "ROLE",
                "STATUS"
        );

        System.out.println(
                "------------------------------------------------"
        );

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

    // UPDATE USER ROLE AND STATUS
    private void updateUser() {

        System.out.println(
                "\n========== UPDATE USER ROLE AND STATUS =========="
        );

        displayUsers();

        System.out.println("\nEnter 0 at any step to cancel.");

        // STEP 1: GET USER ID
        int targetUserId = readIntInput(
                "Enter User ID to update (0 to cancel): "
        );

        if (targetUserId == 0) {

            System.out.println("Update cancelled.");
            return;
        }

        // STEP 2: CHECK USER ID
        if (targetUserId < 0) {

            System.out.println("User ID must be positive.");
            return;
        }

        // STEP 3: PREVENT ADMIN SELF-EDIT
        if (targetUserId == currentAdmin.id()) {

            System.out.println(
                    "WARNING: Modifying the currently active admin account is restricted."
            );

            return;
        }

        // STEP 4: CHECK IF USER EXISTS
        boolean userExists = false;

        List<User> users = userDao.getAllUsers();

        for (User user : users) {

            if (user.id() == targetUserId) {
                userExists = true;
                break;
            }
        }

        if (!userExists) {

            System.out.println("User ID not found.");
            return;
        }

        // STEP 5: GET NEW ROLE
        String newRole = readValidRole();

        if (newRole == null) {

            System.out.println("Update cancelled.");
            return;
        }

        // STEP 6: GET NEW STATUS
        String newStatus = readValidStatus();

        if (newStatus == null) {

            System.out.println("Update cancelled.");
            return;
        }

        // STEP 7: DISPLAY UPDATE SUMMARY
        System.out.println("\n========== UPDATE SUMMARY ==========");

        System.out.println("User ID: " + targetUserId);
        System.out.println("New Role: " + newRole);
        System.out.println("New Status: " + newStatus);

        System.out.println("\nUpdating account...");

        // STEP 8: UPDATE DATABASE
        boolean success = userDao.updateUserRoleAndStatus(
                targetUserId,
                newRole,
                newStatus
        );

        if (success) {

            System.out.println(
                    "Account information updated successfully!"
            );

        } else {

            System.out.println(
                    "Failed to update account information."
            );
        }
    }

    // VALIDATE INTEGER INPUT
    private int readIntInput(String prompt) {

        while (true) {

            System.out.print(prompt);

            String input = sc.nextLine().trim();

            try {

                int number = Integer.parseInt(input);

                if (number < 0) {

                    System.out.println(
                            "Please enter 0 or a positive number."
                    );

                    continue;
                }

                return number;

            } catch (NumberFormatException e) {

                System.out.println(
                        "Invalid input! Please enter numbers only."
                );
            }
        }
    }

    // VALIDATE ROLE
    private String readValidRole() {

        while (true) {

            System.out.println("\n========== AVAILABLE ROLES ==========");

            System.out.println("1. ADMIN");
            System.out.println("2. STAFF");
            System.out.println("3. CUSTOMER");

            System.out.print(
                    "Enter new role (or 0 to cancel): "
            );

            String role = sc.nextLine().trim().toUpperCase();

            if (role.equals("0")) {
                return null;
            }

            if (role.equals("ADMIN")
                    || role.equals("STAFF")
                    || role.equals("CUSTOMER")) {

                return role;
            }

            System.out.println(
                    "Invalid role! Please enter ADMIN, STAFF, or CUSTOMER."
            );
        }
    }

    // VALIDATE STATUS
    private String readValidStatus() {

        while (true) {

            System.out.println("\n========== AVAILABLE STATUSES ==========");

            System.out.println("1. APPROVED");
            System.out.println("2. PENDING");

            System.out.print(
                    "Enter new status (or 0 to cancel): "
            );

            String status = sc.nextLine().trim().toUpperCase();

            if (status.equals("0")) {
                return null;
            }

            if (status.equals("APPROVED")
                    || status.equals("PENDING")) {

                return status;
            }

            System.out.println(
                    "Invalid status! Please enter APPROVED or PENDING."
            );
        }
    }

    // APPROVE OR REJECT ACCOUNT
    private void changeStatus(String status) {

        displayUsers();

        int id = readIntInput(
                "\nEnter account ID (0 to cancel): "
        );

        if (id == 0) {

            System.out.println("Operation cancelled.");
            return;
        }

        if (id == currentAdmin.id()) {

            System.out.println(
                    "WARNING: Modifying the currently active admin account is restricted."
            );

            return;
        }

        userDao.updateUserStatus(id, status);
    }

    // SOFT DELETE ACCOUNT
    private void deleteAccount() {

        displayUsers();

        int id = readIntInput(
                "\nEnter account ID to delete (0 to cancel): "
        );

        if (id == 0) {

            System.out.println("Delete operation cancelled.");
            return;
        }

        if (id == currentAdmin.id()) {

            System.out.println(
                    "WARNING: Modifying the currently active admin account is restricted."
            );

            return;
        }

        userDao.softDeleteUser(id);
    }
}