package com.vivu.booking.controller;

import com.vivu.booking.dao.RoleDao;
import com.vivu.booking.dao.UsersDao;
import com.vivu.booking.dto.request.HostProfileRequest;
import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.dto.response.HostProfileResponse;
import com.vivu.booking.dto.response.UsersLoginResponse;
import com.vivu.booking.entity.HostProfile;
import com.vivu.booking.entity.User;
import com.vivu.booking.enums.HostStatus;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.service.HostProfileService;
import com.vivu.booking.service.impl.HostProfileImpl;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(urlPatterns = "/api/HostProfile/*")
public class HostProfileServlet extends HttpServlet {
       private HostProfileService hostProfileService;
       @Override
       public void init(){
              this.hostProfileService=new HostProfileImpl(new UsersDao(),new RoleDao());
       }

       @Override
       protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try{
                   String path=req.getPathInfo();
                   if(path==null||path.equals("/")){
                          HostStatus status=parseEnum(req.getParameter("status"), HostStatus.class);
                          String q=req.getParameter("q");
                          int page = ServletUtils.parseIntParam(req, "page", 0);
                          int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                          if (page < 0) page = 0;
                          if (size <= 0) size = 20;
                          var result= hostProfileService.list(status, q, page, size);
                          ServletUtils.ok(req, resp, result);
                   }else {
                          Long id=parseId(path);
                          ServletUtils.ok(req, resp, hostProfileService.getById(id));
                   }
            }catch (Exception e){
                   ServletUtils.handleException(req, resp, e);
            }
       }
       @Override
       protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
              try {

                     // 1. Lấy session hiện tại
                     HttpSession session = req.getSession(false);

                     if (session == null) {
                            throw new BusinessException(
                                    401,
                                    "Bạn chưa đăng nhập"
                            );
                     }

                     // 2. Lấy user đang đăng nhập
                     UsersLoginResponse loginUser =
                             (UsersLoginResponse) session.getAttribute("user");

                     if (loginUser == null) {
                            throw new BusinessException(
                                    401,
                                    "Bạn chưa đăng nhập"
                            );
                     }

                     // 3. Lấy userId từ session
                     Long userId = loginUser.getId();

                     if (userId == null) {
                            throw new BusinessException(
                                    401,
                                    "Không xác định được userId"
                            );
                     }

                     // 4. Đọc JSON
                     HostProfileRequest body =
                             ServletUtils.readBody(
                                     req,
                                     HostProfileRequest.class
                             );

                     // 5. Tạo HostProfile
                     HostProfileResponse created =
                             hostProfileService.create(
                                     body,
                                     userId
                             );

                     // 6. Trả về 201
                     ServletUtils.created(
                             req,
                             resp,
                             created
                     );

              } catch (Exception e) {
                     ServletUtils.handleException(
                             req,
                             resp,
                             e
                     );
              }
       }
       @Override
       protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
              try {
                     Long id = parseId(req.getPathInfo());
                     HostProfileRequest body = ServletUtils.readBody(
                                     req,
                                     HostProfileRequest.class
                             );

                     var updated = hostProfileService.update(id, body);
                     ServletUtils.ok(req, resp, updated);
              } catch (Exception e) {
                     ServletUtils.handleException(req, resp, e);
              }
       }
       @Override
       protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
              try {
                     Long id = parseId(req.getPathInfo());
                     hostProfileService.delete(id);
                     ServletUtils.ok(req, resp, java.util.Map.of("deletedId", id));
              } catch (Exception e) {
                     ServletUtils.handleException(req, resp, e);
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

