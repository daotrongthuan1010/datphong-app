package com.vivu.booking.controller;

import com.vivu.booking.enums.RankNameType;
import com.vivu.booking.service.LoyaltyRankService;
import com.vivu.booking.service.impl.LoyaltyRankServiceImp;
import com.vivu.booking.utils.ServletUtils;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = "/api/loyalty/*")
public class LoyaltyRankServlet extends HttpServlet {
    private LoyaltyRankService loyaltyRankService;

    @Override
    public void init(){ this.loyaltyRankService = new LoyaltyRankServiceImp();}

    @Override
    protected  void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
       try {
           String path = req.getPathInfo(); //null || /loyalty/ranks
           //GET /loyalty/ranks - danh sách hạng & quyền lợi
            if (path != null && path.matches("/^\\d+/ranks/?$")){
                /** Substring: lấy index trong khoảng (begin,end)  */
                RankNameType rankName = parseEnum(req.getParameter("Rank"), RankNameType.class);

                var loyalty = loyaltyRankService.getByName(rankName);
                ServletUtils.ok(req, resp ,loyalty);
                return;
            }

       }
       catch (Exception e){
           ServletUtils.handleException(req,resp,e);
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


