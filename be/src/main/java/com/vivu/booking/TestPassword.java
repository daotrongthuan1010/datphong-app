package com.vivu.booking;

import com.vivu.booking.utils.PasswordUntil;

public class TestPassword {
    public static void main(String[] args) {
        String hash = PasswordUntil.hashedPassword("123456");
        System.out.println(hash);
    }
}
