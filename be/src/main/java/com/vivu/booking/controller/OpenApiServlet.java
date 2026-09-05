package com.vivu.booking.controller;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

@WebServlet("/openapi.json")
public class OpenApiServlet extends HttpServlet {

    private String jsonSwagger = "{}";

    @Override
    public void init() throws ServletException {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setInfo(new Info().title("Servlet Raw API").version("1.0.0"));
        Paths paths = new Paths();

        // Scan tất cả Servlet trong package controller/servlet của bạn
        Reflections reflections = new Reflections("com.example.servlet");
        Set<Class<?>> servletClasses = reflections.getTypesAnnotatedWith(WebServlet.class);

        for (Class<?> clazz : servletClasses) {
            WebServlet webServletAnno = clazz.getAnnotation(WebServlet.class);
            if (webServletAnno == null || webServletAnno.value().length == 0) continue;

            String urlPattern = webServletAnno.value()[0];
            PathItem pathItem = new PathItem();

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    io.swagger.v3.oas.annotations.Operation opAnno = method.getAnnotation(Operation.class);

                    io.swagger.v3.oas.models.Operation swaggerOp = new io.swagger.v3.oas.models.Operation()
                            .summary(opAnno.summary())
                            .description(opAnno.description());

                    String httpMethod = method.getName().toLowerCase();
                    switch (httpMethod) {
                        case "doget": pathItem.setGet(swaggerOp); break;
                        case "dopost": pathItem.setPost(swaggerOp); break;
                        case "doput": pathItem.setPut(swaggerOp); break;
                        case "dodelete": pathItem.setDelete(swaggerOp); break;
                    }
                }
            }
            paths.addPathItem(urlPattern, pathItem);
        }

        openAPI.setPaths(paths);
        this.jsonSwagger = Json.pretty(openAPI);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(jsonSwagger);
    }
}