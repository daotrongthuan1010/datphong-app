package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.dao.BookingDao;
import com.vivu.booking.dao.PaymentDao;
import com.vivu.booking.dto.request.PaymentRequest;
import com.vivu.booking.dto.response.PaymentResponse;
import com.vivu.booking.payment.gateway.PaymentGateway;
import com.vivu.booking.payment.gateway.vnpay.VnPayGateway;
import com.vivu.booking.service.PaymentService;
import com.vivu.booking.service.impl.PaymentServiceImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/payments")
public class PaymentServlet extends HttpServlet {
       private final ObjectMapper mapper = new ObjectMapper();
       private final PaymentService paymentService = new PaymentServiceImpl(new PaymentDao(), new BookingDao(), new VnPayGateway());

       @Override
       protected void doPost(HttpServletRequest req, HttpServletResponse resp)throws IOException{
           resp.setContentType("application/json");
           resp.setCharacterEncoding("UTF-8");
           PaymentRequest paymentRequest= mapper.readValue(req.getReader(), PaymentRequest.class);
           Long urerId=1L;
           String clientIp=req.getRemoteAddr();
           PaymentResponse response=paymentService.createPayment(paymentRequest, urerId, clientIp);
           resp.setStatus(HttpServletResponse.SC_CREATED);
           mapper.writeValue(resp.getWriter(), response);
       }
};


