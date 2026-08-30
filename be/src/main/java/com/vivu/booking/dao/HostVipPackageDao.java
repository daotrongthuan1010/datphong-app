package com.vivu.booking.dao;

import com.vivu.booking.entity.HostVipPackage;
import jakarta.persistence.EntityManager;
import java.util.List;

public class HostVipPackageDao extends BaseDao<HostVipPackage, Long> {

    public HostVipPackageDao() {
        super(HostVipPackage.class);
    }

    /** Bảng nhỏ, tham chiếu (danh sách gói VIP hiển thị public) - sắp theo giá tăng dần. */
    public List<HostVipPackage> findAllOrderedByPrice(EntityManager em) {
        return em.createQuery(
                        "SELECT p FROM HostVipPackage p ORDER BY p.price ASC", HostVipPackage.class)
                .getResultList();
    }
}
