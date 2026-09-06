package com.vivu.booking.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/payments/vnpay/return")
public class VnPayReturnServlet extends HttpServlet {
    // test vnpay
       @Override
       protected void doGet(HttpServletRequest req, HttpServletResponse resp)throws IOException {
           resp.setContentType("text/plain;charset=UTF-8");
           resp.getWriter().println("đã quay lại từ vnpay");
       }
}
