package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.model.User;
import com.scc.ui.Authentication;
import com.scc.ui.AdminMenu;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // INITIALIZE DATABASE
        DatabaseInitializer.initialize();

        // CREATE USER DAO
        UserDao userDao = new UserDao();

        try (Scanner sc = new Scanner(System.in)) {

            Authentication auth = new Authentication(userDao, sc);

            boolean running = true;

            while (running) {

                System.out.println(
                        "\n===================================="
                );

                System.out.println(
                        "  KIDS PARADISE: INDOOR PLAYGROUND"
                );

                System.out.println(
                        "===================================="
                );

                System.out.println("1. Login");
                System.out.println("2. Register");
                System.out.println("3. Exit");

                System.out.print("Choose an option: ");

                String choice = sc.nextLine().trim();

                switch (choice) {

                    // LOGIN
                    case "1" -> {

                        User user = auth.login();

                        if (user != null) {

                            System.out.println(
                                    "\nWelcome, " + user.username()
                                            + "! Role: " + user.roles()
                            );

                            switch (user.roles().toUpperCase()) {

                                case "ADMIN" -> {

                                    AdminMenu adminMenu = new AdminMenu(
                                            userDao,
                                            sc,
                                            user
                                    );

                                    adminMenu.showMenu();
                                }

                                case "STAFF" ->
                                        staffMenu(user);

                                case "CUSTOMER" ->
                                        customerMenu(user);

                                default ->
                                        System.out.println(
                                                "Unknown role."
                                        );
                            }
                        }
                    }

                    // REGISTER
                    case "2" -> auth.register();

                    // EXIT
                    case "3" -> {

                        System.out.println(
                                "Thank you for visiting Kids Paradise!"
                        );

                        running = false;
                    }

                    default -> System.out.println(
                            "Invalid option. Please try again."
                    );
                }
            }
        }
    }

    // STAFF DASHBOARD
    private static void staffMenu(User user) {

        System.out.println(
                "\n========== STAFF DASHBOARD =========="
        );

        System.out.println(
                "Welcome, " + user.username()
        );

        System.out.println(
                "Staff-specific features are not implemented yet."
        );

        System.out.println(
                "Returning to the main menu..."
        );
    }

    // CUSTOMER DASHBOARD
    private static void customerMenu(User user) {

        System.out.println(
                "\n========== CUSTOMER DASHBOARD =========="
        );

        System.out.println(
                "Welcome, " + user.username()
        );

        System.out.println(
                "Customer-specific features are not implemented yet."
        );

        System.out.println(
                "Returning to the main menu..."
        );
    }
}