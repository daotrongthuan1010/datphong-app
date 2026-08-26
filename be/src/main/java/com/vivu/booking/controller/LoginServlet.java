package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.dto.request.UsersLoginRequest;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.service.UserService;
import com.vivu.booking.service.impl.UserServiceImpl;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init() {
        this.userService = new UserServiceImpl();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        UsersLoginRequest usersLoginRequest = objectMapper.readValue(
                req.getReader(), UsersLoginRequest.class
        );
        UsersLoginResponse usersLoginResponse = userService.login(
                usersLoginRequest.getUsername(),
                usersLoginRequest.getPassword()
        );
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        if (usersLoginResponse == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sai username hoặc password");
            return;
        }
        HttpSession session = req.getSession(true);
        session.setAttribute("user", usersLoginResponse);
        session.setAttribute("role", usersLoginResponse.getRole());
        objectMapper.writeValue(resp.getWriter(), usersLoginResponse);
    }
}
