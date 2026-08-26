package com.vivu.booking.utils;


import org.mindrot.jbcrypt.BCrypt;

public class PasswordUntil {
    public static String hashedPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
