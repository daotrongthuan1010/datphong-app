package com.vivu.booking.dao;

import com.vivu.booking.entity.SystemSetting;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

public class SystemSettingDao extends BaseDao<SystemSetting, Long> {

    public SystemSettingDao() {
        super(SystemSetting.class);
    }

    /** UNIQUE index trên setting_key - tra cứu cấu hình (commission %, platform fee...) */
    public Optional<SystemSetting> findByKey(String key, EntityManager em) {
        List<SystemSetting> result = em.createQuery(
                        "SELECT s FROM SystemSetting s WHERE s.settingKey = :key", SystemSetting.class)
                .setParameter("key", key)
                .getResultList();
        return result.stream().findFirst();
    }

    /** Cập nhật giá trị cấu hình bằng bulk update, tránh phải load Entity rồi merge lại. */
    public int updateValue(String key, String newValue, Long updatedBy, EntityManager em) {
        return em.createQuery(
                        "UPDATE SystemSetting s SET s.settingValue = :value, s.updatedBy.id = :updatedBy, " +
                                "s.updatedAt = CURRENT_TIMESTAMP WHERE s.settingKey = :key")
                .setParameter("value", newValue)
                .setParameter("updatedBy", updatedBy)
                .setParameter("key", key)
                .executeUpdate();
    }
}