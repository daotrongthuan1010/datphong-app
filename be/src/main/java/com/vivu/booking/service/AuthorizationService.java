package com.vivu.booking.service;

import com.vivu.booking.config.HibernateConfig;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phan quyen dua tren DB: load map roleCode -> {permissionCode} tu
 * roles JOIN role_permissions JOIN permissions. Cache 5 phut de khong truy
 * van DB moi request; reload khi TTL het han.
 *
 * <p><b>Negative cache:</b> ngay ca khi DB trong / role_permissions chua seed thi van ghi
 * dau thoi gian ({@code loadedAt}) va hien thi {@code WARN} 1 lan. Dieu nay tranh moi request
 * deu query DB (vo luc). Moi lan {@link com.vivu.booking.config.AppContextListener#seedPermissions}
 * ghi lien ket moi, keo {@link #invalidate()} de lam moi cache ngay lap tuc.
 */
public class AuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationService.class);
    private static final long TTL_MS = 5 * 60 * 1000L;

    private static volatile Map<String, Set<String>> cache = Collections.emptyMap();
    private static volatile long loadedAt = 0L;
    private static volatile boolean emptyWarned = false;

    private AuthorizationService() {}

    /** Toan bo permission code cua mot tap role (role code khong phan biet hoa thuong). */
    public static Set<String> permissionsOf(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) return Collections.emptySet();
        Map<String, Set<String>> map = current();
        Set<String> out = new HashSet<>();
        for (String rc : roleCodes) {
            if (rc == null) continue;
            // DB seed lowercase nhung JWT/session co the chua chu hoa — so sanh theo lowercase.
            String lower = rc.trim().toLowerCase();
            Set<String> perms = map.get(lower);
            if (perms != null) out.addAll(perms);
        }
        return out;
    }

    /** True neu user (tap role) co permission code yeu cau. */
    public static boolean has(Set<String> roleCodes, String requiredPermission) {
        if (requiredPermission == null) return true;
        return permissionsOf(roleCodes).contains(requiredPermission);
    }

    /** Buoc cache reload — goi sau khi Sua role_permissions (role CRUD, host approve, seed). */
    public static void invalidate() {
        loadedAt = 0L;
        emptyWarned = false;
    }

    private static Map<String, Set<String>> current() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < TTL_MS) return cache;
        synchronized (AuthorizationService.class) {
            now = System.currentTimeMillis();
            if (now - loadedAt < TTL_MS) return cache;
            Map<String, Set<String>> fresh = loadFromDb();
            cache = fresh; // ke ca khi rong — tranh query moi request
            loadedAt = System.currentTimeMillis();
            if (fresh.isEmpty()) {
                if (!emptyWarned) {
                    log.warn("[AuthorizationService] role_permissions trong/ chua seed — moi route co permission (admin, host...) deu tra 403 cho den khi AppContextListener.seedPermissions chay xong. Kiem tra Hibernate hbm2ddl/loi seed.");
                    emptyWarned = true;
                }
            } else {
                emptyWarned = false;
            }
            return cache;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<String>> loadFromDb() {
        try (Session s = HibernateConfig.getSessionFactory().openSession()) {
            // lower(code) de role code trong DB du hoa hay thuong deu map dung
            List<Object[]> rows = s.createQuery(
                    "select lower(r.code), p.code from RolePermission rp " +
                    "join rp.role r join rp.permission p", Object[].class)
                    .getResultList();
            Map<String, Set<String>> map = new HashMap<>();
            for (Object[] row : rows) {
                String roleCode = (String) row[0];
                String permCode = (String) row[1];
                map.computeIfAbsent(roleCode, k -> new HashSet<>()).add(permCode);
            }
            return map;
        } catch (RuntimeException e) {
            // DB chua san sang — tra rong, caller se tu choi write op (403) nhung
            // van cho request public qua. Khong span emptyWarned o day de lop current() tu lo.
            log.warn("[AuthorizationService] load role_permissions fail: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
