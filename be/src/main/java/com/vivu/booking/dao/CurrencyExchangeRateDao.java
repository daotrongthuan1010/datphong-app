package com.vivu.booking.dao;

import com.vivu.booking.entity.CurrencyExchangeRate;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class CurrencyExchangeRateDao extends BaseDao<CurrencyExchangeRate,Long>{

    protected CurrencyExchangeRateDao() {
        super(CurrencyExchangeRate.class);
    }
    /** UNIQUE(base_currency, target_currency) - quy đổi giá hiển thị theo currency_pref của user */
    public Optional<CurrencyExchangeRate> findByPair(String base, String target, EntityManager em) {
        List<CurrencyExchangeRate> result = em.createQuery(
                        "SELECT r FROM CurrencyExchangeRate r WHERE r.baseCurrency = :base " +
                                "AND r.targetCurrency = :target", CurrencyExchangeRate.class)
                .setParameter("base", base)
                .setParameter("target", target)
                .getResultList();
        return result.stream().findFirst();
    }
}
