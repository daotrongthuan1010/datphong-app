package com.vivu.booking.dao;

import com.vivu.booking.entity.Voucher;
import com.vivu.booking.enums.DiscountTypeEnum;
import com.vivu.booking.enums.VoucherOwnerType;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class VoucherDao extends BaseDao<Voucher, Long> {

    public VoucherDao() {
        super(Voucher.class);
    }

    @Override
    public List<Voucher> findAll(int page, int size) {
        return super.findAll(page, size);
    }


    public Optional<Voucher> findByCode(String code) {
        return read(s -> s.createQuery
                ("from Voucher where code= :code", Voucher.class).setParameter("code", code).uniqueResultOptional());
    }

    public boolean existByCode(String code) {
        return read(s -> s.createQuery("select count(v) from Voucher v where code =:code", Long.class)
                .setParameter("code", code).getSingleResult() > 0);
    }

    public List<Voucher> search(VoucherOwnerType type, DiscountTypeEnum status, String keyword, int page, int size) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("from Voucher v where 1=1");
            if (type != null) hql.append(" and v.type = :type");
            if (status != null) hql.append(" and v.status = :status");
            if (keyword != null && !keyword.isBlank()) {
                hql.append("  lower(v.code) like :kw");
            }
            hql.append(" order by v.id desc");

            var q = s.createQuery(hql.toString(), Voucher.class);
            if (type != null) q.setParameter("type", type);
            if (status != null) q.setParameter("status", status);
            if (keyword != null && !keyword.isBlank()) {
                q.setParameter("kw", "%" + keyword.toLowerCase().trim() + "%");
            }
            q.setFirstResult(page * size);
            q.setMaxResults(size);
            return q.getResultList();
        });
    }

    public long countSearch(VoucherOwnerType type, DiscountTypeEnum status, String keyword) {
        return read(s -> {
            StringBuilder hql = new StringBuilder("select count(v) from Voucher v where 1=1");
            if (type != null) hql.append(" and v.type = :type");
            if (status != null) hql.append(" and v.status = :status");
            if (keyword != null && !keyword.isBlank()) {
                hql.append(" and lower(v.code) like :kw");
            }

            var q = s.createQuery(hql.toString(), Long.class);
            if (type != null) q.setParameter("type", type);
            if (status != null) q.setParameter("status", status);
            if (keyword != null && !keyword.isBlank()) {
                q.setParameter("kw", "%" + keyword.toLowerCase().trim() + "%");
            }
            return q.getSingleResult();
        });

    }
}
