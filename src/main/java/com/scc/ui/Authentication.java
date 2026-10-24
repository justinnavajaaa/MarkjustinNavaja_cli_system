
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner sc;

    public Authentication(UserDao userDao, Scanner sc) {
        this.userDao = userDao;
        this.sc = sc;
    }

    public User login() {
        System.out.println("\n========== LOGIN ==========");

        System.out.print("Username: ");
        String username = sc.nextLine().trim();

        System.out.print("Password: ");
        String password = sc.nextLine();

        return userDao.login(username, password);
    }

    public void register() {
        System.out.println("\n========== REGISTER ==========");

        System.out.print("Choose a username: ");
        String username = sc.nextLine().trim();

        System.out.print("Choose a password (minimum 8 characters): ");
        String password = sc.nextLine();

        System.out.println("\nSelect account type:");
        System.out.println("1. Staff");
        System.out.println("2. Customer");
        System.out.print("Choice: ");

        String choice = sc.nextLine().trim();

        String role = switch (choice) {
            case "1" -> "STAFF";
            case "2" -> "CUSTOMER";
            default -> null;
        };

        if (role == null) {
            System.out.println("Invalid role selection.");
            return;
        }

        userDao.registerUser(username, password, role);
    }
}
