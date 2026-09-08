package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.dto.request.WalletRequest;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.dto.response.WalletResponse;
import com.vivu.booking.dto.response.WalletTransactionResponse;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.WalletService;
import com.vivu.booking.service.impl.WalletServiceImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/api/wallets/*")
public class WalletServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private final WalletService walletService = new WalletServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        Long userId=getCurrentUserId(req);
        String path=req.getPathInfo();
        if(path==null||path.equals("/")){
            WalletResponse response=walletService.getMyWallet(userId);
            mapper.writeValue(resp.getWriter(),response);
            return;
        }
        if(path.equals("/transaction")){
            List<WalletTransactionResponse> response=walletService.getMyTransactions(userId);
            mapper.writeValue(resp.getWriter(),response);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND,"API khong ton tai");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)throws IOException{
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        Long userId = getCurrentUserId(req);
        WalletRequest request= mapper.readValue(req.getInputStream(), WalletRequest.class);
        WalletResponse response=walletService.create(userId, request);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        mapper.writeValue(resp.getWriter(), response);
    }
    private Long getCurrentUserId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            throw new BusinessException(401, "Bạn chưa đăng nhập");
        }

        UsersLoginResponse loginUser = (UsersLoginResponse) session.getAttribute("user");
        if (loginUser == null) {throw new BusinessException(401, "Bạn chưa đăng nhập");}
        return loginUser.getId();
    }
}
