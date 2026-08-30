package com.vivu.booking.dao;

import com.vivu.booking.entity.RoomCalendar;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RoomCalendarDao extends BaseDao<RoomCalendar, Long>  {
    public RoomCalendarDao() {
        super(RoomCalendar.class);
    }
    public List<RoomCalendar> findByRoomAndDateRange(Long roomId, LocalDate from, LocalDate to,
                                                     EntityManager em) {
        return em.createQuery(
                        "SELECT c FROM RoomCalendar c WHERE c.room.id = :roomId " +
                                "AND c.calendarDate BETWEEN :from AND :to ORDER BY c.calendarDate",
                        RoomCalendar.class)
                .setParameter("roomId", roomId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }
}
