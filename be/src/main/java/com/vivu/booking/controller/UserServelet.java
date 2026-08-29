package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dto.request.UsersResquest;
import com.vivu.booking.dto.response.UsersResponse;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.enums.UserStatus;
import com.vivu.booking.enums.UserType;
import com.vivu.booking.service.UserService;
import com.vivu.booking.service.impl.UserServiceImpl;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@WebServlet("/api/users/*")
@MultipartConfig
public class UserServelet extends HttpServlet {
    private UserService userService ;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(){this.userService=new UserServiceImpl(new RoleDao());}

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        try {
            String path = req.getPathInfo(); // null or "/{id}"
            //export excel
            if("/excel".equals(path)){
                UserType type=parseEnum(req.getParameter("type"), UserType.class);
                UserStatus status=parseEnum(req.getParameter("status"), UserStatus.class);
                String keyword=req.getParameter("q");
                int page=ServletUtils.parseIntParam(req,"page",0);
                int size=ServletUtils.parseIntParam(req,"size",100);
                String fileName="user_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))+".xlsx";
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename=\""+fileName+"\"");
                userService.exportExcel(resp.getOutputStream(),null,null,null,0,10000);
                resp.getOutputStream().flush();
                return;
            }
            if (path == null || path.equals("/")) {
                UserType type = parseEnum(req.getParameter("type"), UserType.class);
                UserStatus status = parseEnum(req.getParameter("status"), UserStatus.class);
                String q = req.getParameter("q");
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = userService.list(type, status, q, page, size);
                ServletUtils.ok(req, resp, result);
            } else {
                Long id = parseId(path);
                ServletUtils.ok(req, resp, userService.getById(id));
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        try{
            Part filePart = req.getPart("file");

            if (filePart == null || filePart.getSize() == 0) {
                throw new IllegalArgumentException(
                        "Chưa chọn ảnh"
                );
            }
            String userJson = req.getParameter("user");
            if (userJson == null || userJson.isBlank()) {
                throw new IllegalArgumentException(
                        "Thiếu thông tin user"
                );
            }

            UsersResquest request =  objectMapper.readValue(userJson, UsersResquest.class);
            ValidationUtils.validate(request);
            UsersResponse created = userService.create(request, filePart);
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            e.printStackTrace();
            ServletUtils.created(req, resp, e);
        }
    }
    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        try{
            Part filePart = req.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                throw new IllegalArgumentException(
                        "Chưa chọn ảnh"
                );
            }
            String userJson = req.getParameter("user");
            if (userJson == null || userJson.isBlank()) {
                throw new IllegalArgumentException(
                        "Thiếu thông tin user"
                );
            }
            UsersResquest body=  objectMapper.readValue(userJson, UsersResquest.class);
            Long id= parseId(req.getPathInfo());
            UsersResponse updated=userService.update(id,body,filePart);
            ServletUtils.created(req,resp,updated);
        }catch (Exception e){
            ServletUtils.created(req,resp,e);
        }
    }
    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        try{
            Long id= parseId(req.getPathInfo());
            userService.deleteById(id);
            ServletUtils.ok(req, resp, java.util.Map.of("deletedId", id));
        }catch (Exception e){
            ServletUtils.created(req,resp,e);
        }
    }
    private static Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.equals("/"))
            throw new com.vivu.booking.exception.BusinessException(400, "Missing id in path");
        String s = pathInfo.replaceFirst("^/", "").split("/")[0];
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new com.vivu.booking.exception.BusinessException(400, "Invalid id: " + s);
        }
    }
    private static <E extends Enum<E>> E parseEnum(String val, Class<E> type) {
        if (val == null || val.isBlank()) return null;
        try {
            return Enum.valueOf(type, val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.vivu.booking.exception.BusinessException(400, "Invalid " + type.getSimpleName() + ": " + val);
        }
    }
}
