package com.vivu.booking.dao;

import com.vivu.booking.entity.RoomCalendar;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO cho RoomCalendar — mỗi dòng là 1 ngày của 1 phòng, duy nhất trên (room_id, calendar_date).
 *
 * <h2>Vì sao bảng này tồn tại (đừng chỉ dựa vào Booking)</h2>
 * <ul>
 *   <li>FE cần "lịch tháng" 30 ô màu của 1 phòng: AVAILABLE / BLOCKED / BOOKED. Nếu chỉ có Booking
 *   thì mỗi lần render phải chồng lấn tính lại toàn bộ đơn — đắt và dễ sai ở biên ngày.</li>
 *   <li>Đây là nơi để host/admin "chặn ngày" (bảo trì, dùng riêng) mà KHÔNG cần tạo Booking giả.</li>
 *   <li>Là chỗ để ghi {@code priceOverride} theo mùa/lễ — giá 1 đêm khác giá mặc định của Room.</li>
 * </ul>
 *
 * <h2>Kỷ luật lock — đọc kỹ trước khi sửa</h2>
 * Luôn lock theo <b>1 thứ tự cố định</b>: Room (PESSIMISTIC_WRITE) → RoomCalendar (tăng dần theo ngày).
 * Lock theo thứ tự ngẫu nhiên là công thức kinh điển gây deadlock khi 2 transaction chạy song song.
 *
 * <p>{@code c.room.id = :roomId} KHÔNG sinh JOIN: Hibernate so trực tiếp cột khóa ngoại {@code room_id}
 * trên bảng room_calendar, nên {@code FOR UPDATE} chỉ khóa room_calendar chứ không khóa lại bảng rooms.
 */
public class RoomCalendarDao extends BaseDao<RoomCalendar, Long> {

    public RoomCalendarDao() {
        super(RoomCalendar.class);
    }

    private static final String RANGE_HQL = """
            from RoomCalendar c
            where c.room.id = :roomId and c.calendarDate >= :from and c.calendarDate < :to
            order by c.calendarDate
            """;

    /**
     * Khóa và lấy toàn bộ ngày [from, to) của 1 phòng, <b>tạo luôn dòng còn thiếu</b> (phòng mới
     * chưa từng được đặt sẽ không có dòng nào).
     *
     * <p>Phải gọi trong transaction đang mở, và caller đã lock Room trước đó (xem {@code BookingServiceImpl}).
     * Lock Room là mutex thật: nó khiến transaction thứ hai chờ đến khi transaction thứ nhất commit,
     * nên không thể xảy ra chuyện 2 bên cùng thấy "chưa có dòng" rồi cùng INSERT trùng (room_id, date).
     *
     * @param s    Session đang mở transaction
     * @param roomId id phòng
     * @param from   ngày nhận phòng (inclusive)
     * @param to     ngày trả phòng (exclusive — đêm cuối là to-1)
     * @return Map ngày → RoomCalendar, đủ mọi ngày trong khoảng, theo thứ tự tăng dần
     */
    public Map<LocalDate, RoomCalendar> findOrCreateForUpdate(Session s, Long roomId, LocalDate from, LocalDate to) {
        List<RoomCalendar> existing = s.createQuery(RANGE_HQL, RoomCalendar.class)
                .setParameter("roomId", roomId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        Map<LocalDate, RoomCalendar> byDate = new LinkedHashMap<>();
        for (RoomCalendar c : existing) {
            byDate.put(c.getCalendarDate(), c);
        }
        for (LocalDate d = from; d.isBefore(to); d = d.plusDays(1)) {
            if (!byDate.containsKey(d)) {
                RoomCalendar fresh = RoomCalendar.builder()
                        .room(s.getReference(com.vivu.booking.entity.Room.class, roomId))
                        .calendarDate(d)
                        .status(com.vivu.booking.enums.CalendarStatusType.AVAILABLE)
                        .build();
                s.persist(fresh);
                byDate.put(d, fresh);
            }
        }
        return byDate;
    }

    /**
     * Khóa và lấy ngày [from, to) <b>không</b> tạo dòng mới — dùng cho luồng trả lịch
     * (cancel/expire): ngày nào chưa có dòng thì vốn đã AVAILABLE, không cần đụng tới.
     */
    public Map<LocalDate, RoomCalendar> findForUpdate(Session s, Long roomId, LocalDate from, LocalDate to) {
        List<RoomCalendar> existing = s.createQuery(RANGE_HQL, RoomCalendar.class)
                .setParameter("roomId", roomId)
                .setParameter("from", from)
                .setParameter("to", to)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        Map<LocalDate, RoomCalendar> byDate = new LinkedHashMap<>();
        for (RoomCalendar c : existing) {
            byDate.put(c.getCalendarDate(), c);
        }
        return byDate;
    }

    /** Đọc không khóa — phục vụ API lịch tháng cho FE. */
    public List<RoomCalendar> findByRoomAndRange(Long roomId, LocalDate from, LocalDate to) {
        return read(s -> s.createQuery(RANGE_HQL, RoomCalendar.class)
                .setParameter("roomId", roomId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList());
    }

    /**
     * Lịch 1 tháng cho FE. Ngày nào chưa có dòng RoomCalendar thì <b>không xuất hiện</b> trong kết quả —
     * FE phải hiểu "thiếu = AVAILABLE" (xem RoomDetail.jsx), vì sinh sẵn hàng nghìn dòng trống cho mọi
     * phòng × mọi ngày là lãng phí vô ích.
     */
    public List<RoomCalendar> findByRoomAndMonth(Long roomId, YearMonth ym) {
        return findByRoomAndRange(roomId, ym.atDay(1), ym.atEndOfMonth().plusDays(1));
    }
}
