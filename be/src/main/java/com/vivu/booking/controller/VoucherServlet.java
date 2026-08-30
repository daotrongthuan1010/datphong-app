package com.vivu.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivu.booking.dto.request.RoomCreateRequest;
import com.vivu.booking.dto.request.RoomUpdateRequest;
import com.vivu.booking.dto.request.VoucherCreateRequest;
import com.vivu.booking.dto.request.VoucherUpdateRequest;
import com.vivu.booking.entity.Voucher;
import com.vivu.booking.entity.VoucherUsage;
import com.vivu.booking.enums.DiscountTypeEnum;
import com.vivu.booking.enums.RoomStatus;
import com.vivu.booking.enums.RoomType;
import com.vivu.booking.enums.VoucherOwnerType;
import com.vivu.booking.service.VoucherService;
import com.vivu.booking.service.impl.VoucherServiceImpl;
import com.vivu.booking.utils.ServletUtils;
import com.vivu.booking.utils.ValidationUtils;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns="/api/voucher/*")
public class VoucherServlet extends HttpServlet {
    private VoucherService voucherService;


    @Override
    public void init(){this.voucherService=new VoucherServiceImpl(); }

    //Test
    public void setVoucherService(VoucherService svc){
        this.voucherService=svc;
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getPathInfo(); // null or "/{id}"
            if (path == null || path.equals("/")) {
                VoucherOwnerType type = parseEnum(req.getParameter("type"), VoucherOwnerType.class);
                DiscountTypeEnum discountTypeEnum = parseEnum(req.getParameter("usage"), DiscountTypeEnum.class);
                String q = req.getParameter("q");
                int page = ServletUtils.parseIntParam(req, "page", 0);
                int size = Math.min(ServletUtils.parseIntParam(req, "size", 20), 100);
                if (page < 0) page = 0;
                if (size <= 0) size = 20;
                var result = voucherService.list(type,discountTypeEnum,q,page,size);
                ServletUtils.ok(req, resp, result);
            } else {
                Long id = parseId(path);
                ServletUtils.ok(req, resp, voucherService.getById(id));
            }
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            VoucherCreateRequest body = ServletUtils.readBody(req, VoucherCreateRequest.class);
            ValidationUtils.validate(body);
            var created = voucherService.create(body);
            ServletUtils.created(req, resp, created);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            VoucherUpdateRequest body = ServletUtils.readBody(req, VoucherUpdateRequest.class);
            var updated = voucherService.update(id, body);
            ServletUtils.ok(req, resp, updated);
        } catch (Exception e) {
            ServletUtils.handleException(req, resp, e);
        }
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Long id = parseId(req.getPathInfo());
            voucherService.delete(id);
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

