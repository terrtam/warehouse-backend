/*
* Generate password hashes for user passwords.
* Place within Flyway migration to import new users.
* */

package com.example.warehouse.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String managerPassword = "manager123";
        String staffPassword = "staff123";

        System.out.println("======================================");
        System.out.println("MANAGER HASH: " + passwordEncoder.encode(managerPassword));
        System.out.println("STAFF HASH:   " + passwordEncoder.encode(staffPassword));
        System.out.println("======================================");
    }
}
