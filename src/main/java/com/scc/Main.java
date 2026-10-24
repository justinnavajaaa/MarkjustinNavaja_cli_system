
package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.Authentication;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        UserDao userDao = new UserDao();

        try (Scanner sc = new Scanner(System.in)) {
            Authentication auth = new Authentication(userDao, sc);
            boolean running = true;

            while (running) {
                System.out.println("\n====================================");
                System.out.println("  KIDS PARADISE: INDOOR PLAYGROUND");
                System.out.println("====================================");
                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");

                String choice = sc.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        User user = auth.login();

                        if (user != null) {
                            System.out.println(
                                    "\nWelcome, " + user.username()
                                            + "! Role: " + user.roles()
                            );

                            switch (user.roles().toUpperCase()) {
                                case "ADMIN" ->
                                        adminMenu(userDao, sc, user);

                                case "STAFF" ->
                                        staffMenu(user);

                                case "CUSTOMER" ->
                                        customerMenu(user);

                                default ->
                                        System.out.println("Unknown role.");
                            }
                        }
                    }

                    case "2" -> auth.register();

                    case "3" -> {
                        System.out.println("Thank you for visiting!");
                        running = false;
                    }

                    default ->
                            System.out.println(
                                    "Invalid option. Please try again."
                            );
                }
            }
        }
    }

    private static void adminMenu(
            UserDao userDao, Scanner sc, User currentUser) {

        if (!"ADMIN".equalsIgnoreCase(currentUser.roles())) {
            System.out.println("Access denied.");
            return;
        }

        boolean running = true;

        while (running) {
            System.out.println("\n========== ADMIN DASHBOARD ==========");
            System.out.println("1. View accounts");
            System.out.println("2. Approve account");
            System.out.println("3. Reject account");
            System.out.println("4. Soft-delete account");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");

            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> displayUsers(userDao);

                case "2" -> changeStatus(
                        userDao, sc, "APPROVED"
                );

                case "3" -> changeStatus(
                        userDao, sc, "REJECTED"
                );

                case "4" -> deleteAccount(userDao, sc);

                case "5" -> {
                    System.out.println("Admin logged out.");
                    running = false;
                }

                default ->
                        System.out.println("Invalid option.");
            }
        }
    }

    private static void displayUsers(UserDao userDao) {
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

    private static void changeStatus(
            UserDao userDao, Scanner sc, String status) {

        displayUsers(userDao);

        System.out.print("Enter account ID: ");
        String input = sc.nextLine().trim();

        try {
            int id = Integer.parseInt(input);
            userDao.updateUserStatus(id, status);
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid numeric ID.");
        }
    }

    private static void deleteAccount(
            UserDao userDao, Scanner sc) {

        displayUsers(userDao);

        System.out.print("Enter account ID to delete: ");
        String input = sc.nextLine().trim();

        try {
            int id = Integer.parseInt(input);
            userDao.softDeleteUser(id);
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid numeric ID.");
        }
    }

    private static void staffMenu(User user) {
        System.out.println("\n========== STAFF DASHBOARD ==========");
        System.out.println("Welcome, " + user.username());
        System.out.println("Staff-specific features are not implemented yet.");
        System.out.println("Returning to the main menu...");
    }

    private static void customerMenu(User user) {
        System.out.println("\n========== CUSTOMER DASHBOARD ==========");
        System.out.println("Welcome, " + user.username());
        System.out.println("Customer-specific features are not implemented yet.");
        System.out.println("Returning to the main menu...");
    }
}
