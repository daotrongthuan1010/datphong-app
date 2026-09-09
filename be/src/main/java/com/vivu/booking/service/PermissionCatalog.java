package com.vivu.booking.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Danh muc permission code cua he thong + phan quyen mac dinh cho tung role.
 *
 * <p>Tai sao can lop nay: bang {@code permissions}/{@code role_permissions} duoc Hibernate
 * tu tao ({@code hbm2ddl.auto=update}) nhung <b>rong</b> tren DB moi. {@code AuthorFilter}
 * lay quyen tu 2 bang do, nen neu khong seed thi moi route co permission deu tra 403 —
 * ke ca admin. File {@code documents/seed-data.sql} khong giai quyet duoc vi no gan quyen
 * theo {@code role_id} hardcode (1..6), ma id that phu thuoc thu tu insert cua DB.
 *
 * <p>Vi vay day la <b>mot nguon duy nhat</b>: {@link com.vivu.booking.config.AppContextListener}
 * seed theo {@code role.code} (khong theo id), idempotent — chay lai bao nhieu lan cung khong
 * tao ban ghi trung. Gia tri giong het {@code documents/seed-data.sql} de 2 duong seed khong lech nhau.
 */
public final class PermissionCatalog {

    /** Ma quyen -> (ten hien thi, mo ta). Thu tu giu nguyen de log/seed on dinh. */
    public static final Map<String, String[]> ALL = new LinkedHashMap<>();

    /** Quyen mac dinh cua tung role, key la {@code roles.code} viet thuong. */
    public static final Map<String, Set<String>> BY_ROLE = new LinkedHashMap<>();

    /** Role quan tri — lay tat ca quyen trong {@link #ALL}. */
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_HOST = "host";
    public static final String ROLE_USER = "user";

    static {
        add("USER_READ", "Xem người dùng", "Xem danh sách và chi tiết người dùng");
        add("USER_WRITE", "Quản lý người dùng", "Tạo/sửa/xóa người dùng");
        add("ROLE_READ", "Xem vai trò", "Xem danh sách vai trò");
        add("ROLE_WRITE", "Quản lý vai trò", "Tạo/sửa/xóa vai trò");
        add("ROOM_READ", "Xem phòng", "Xem danh sách và chi tiết phòng");
        add("ROOM_WRITE", "Quản lý phòng", "Tạo/sửa/xóa phòng");
        add("BOOKING_READ", "Xem đặt phòng", "Xem danh sách đặt phòng");
        add("BOOKING_WRITE", "Quản lý đặt phòng", "Tạo/sửa/hủy đặt phòng");
        add("PAYMENT_READ", "Xem thanh toán", "Xem lịch sử thanh toán");
        add("PAYMENT_WRITE", "Quản lý thanh toán", "Xử lý thanh toán/hoàn tiền");
        add("REVIEW_READ", "Xem đánh giá", "Xem đánh giá phòng");
        add("REVIEW_WRITE", "Quản lý đánh giá", "Duyệt/xóa đánh giá");
        add("VOUCHER_READ", "Xem voucher", "Xem danh sách voucher");
        add("VOUCHER_WRITE", "Quản lý voucher", "Tạo/sửa/xóa voucher");
        add("HOST_APPROVE", "Duyệt host", "Phê duyệt/từ chối hồ sơ host");
        add("SYSTEM_CONFIG", "Cấu hình hệ thống", "Sửa system_settings");
        add("AMENITY_WRITE", "Quản lý tiện nghi", "Tạo/sửa/xóa tiện nghi");
        add("REPORT_READ", "Xem báo cáo", "Xem báo cáo doanh thu/thống kê");
        add("LOYALTY_READ", "Xem hạng thành viên", "Xem danh sách hạng và quyền lợi loyalty");
        add("LOYALTY_WRITE", "Quản lý hạng thành viên", "Tạo/sửa/xóa hạng loyalty");

        BY_ROLE.put(ROLE_ADMIN, Set.copyOf(ALL.keySet()));
        BY_ROLE.put(ROLE_HOST, Set.of(
                "ROOM_READ", "ROOM_WRITE", "BOOKING_READ", "BOOKING_WRITE",
                "VOUCHER_READ", "VOUCHER_WRITE", "PAYMENT_READ",
                "REVIEW_READ", "REVIEW_WRITE", "AMENITY_WRITE", "LOYALTY_READ"));
        BY_ROLE.put(ROLE_USER, Set.of(
                "ROOM_READ", "BOOKING_READ", "BOOKING_WRITE",
                "PAYMENT_READ", "PAYMENT_WRITE", "REVIEW_READ", "REVIEW_WRITE",
                "VOUCHER_READ", "LOYALTY_READ"));
    }

    private static void add(String code, String name, String description) {
        ALL.put(code, new String[]{name, description});
    }

    private PermissionCatalog() {
    }

    /** Toan bo permission code, thu tu khai bao. */
    public static List<String> codes() {
        return List.copyOf(ALL.keySet());
    }
}
