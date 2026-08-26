package com.vivu.booking.dao;

import com.vivu.booking.entity.Role;
import com.vivu.booking.entity.User;

public class RoleDao extends BaseDao<Role,Long> {

    public RoleDao() {
        super(Role.class);
    }

}
