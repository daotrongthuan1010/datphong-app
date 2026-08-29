package com.vivu.booking.dao;

import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;
import jakarta.persistence.EntityManager;

import java.util.List;
// Hiện tại UserRole chưa có entity riêng, An đang Join thẳng vào entity User
//public class UserRoleDao extends BaseDao<User,Long> {
//    List<Role> findRolesByUserId(Long userId, EntityManager em);
//    List<User> findUsersByRoleId(Long roleId, EntityManager em);
//    boolean hasRole(Long userId, String roleCode, EntityManager em);
//    UserRole grantRole(Long userId, Long roleId, Long grantedBy, EntityManager em);
//    void revokeRole(Long userId, Long roleId, EntityManager em);
//    List<UserRole> findAllByUserId(Long userId, EntityManager em); // kèm granted_by, granted_at để hiển thị lịch sử
//}