package com.vivu.booking.dao;

import com.vivu.booking.entity.Voucher;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class VoucherDao extends BaseDao<Voucher, Long> {

    protected VoucherDao() {
        super(Voucher.class);
    }

    @Override
    public List<Voucher> findAll(int page, int size) {
        return super.findAll(page, size);
    }


    public Optional<Voucher> findByCode(String code){
        return  read(s->s.createQuery
                ("from Voucher where code= :code",Voucher.class).setParameter("code",code).uniqueResultOptional());
    }

    public boolean existByCode (String code) {
        return read(s->s.createQuery("select count(v) from Voucher v where code =:code",Long.class)
                .setParameter("code",code).getSingleResult()>0);
    }

}
