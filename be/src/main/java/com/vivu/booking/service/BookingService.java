package com.vivu.booking.service;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dto.request.BookingCreateRequest;
import com.vivu.booking.dto.response.BookingResponse;

import java.time.LocalDate;

public interface BookingService {

    /**
     * Giữ chỗ: tạo Booking(HOLD, holdExpiresAt = now + 15p) và BLOCK các đêm {@code [checkin, checkout)}.
     * Song song 2 người cùng phòng cùng khoảng ngày thì 1 người nhận 409.
     *
     * @throws com.vivu.booking.exception.BusinessException 400 (ngày/sức chứa/voucher),
     *                                                      403 (tài khoản khóa), 409 (trùng lịch)
     */
    BookingResponse create(Long userId, BookingCreateRequest req);

    PageResponse<BookingResponse> listByUser(Long userId, int page, int size);

    BookingResponse getById(Long userId, Long id);

    /**
     * Hủy đặt của chính mình — trả các đêm về AVAILABLE nếu không còn booking khác phủ đêm đó.
     */
    BookingResponse cancel(Long userId, Long id);

    /** Kiểm tra có còn trống không (dùng trước khi bấm "Đặt ngay" để báo sớm). */
    boolean isAvailable(Long roomId, LocalDate checkin, LocalDate checkout);

    // ==================================================================== mở rộng Batch 1

    /**
     * Xác nhận thanh toán thành công — {@code HOLD/PENDING_PAYMENT → CONFIRMED} và
     * {@code RoomCalendar(BLOCKED → BOOKED)}. Idempotent: gọi lại khi đã CONFIRMED thì no-op.
     *
     * <p>Được gọi từ PaymentService (fake-bank VNPay) sau khi tiền đã về.
     * Nếu booking đã EXPIRED thì ném 409 để tầng trên biết mà hoàn tiền.
     */
    BookingResponse confirm(Long bookingId);

    /**
     * Bản "trong Session" của {@link #confirm} — dành cho tầng thanh toán khi cần đổi
     * trạng thái <b>Payment và Booking trong cùng một transaction</b>.
     *
     * <h2>Vì sao cần bản này?</h2>
     * <p>
     * {@link #confirm} tự mở transaction riêng. Nếu webhook làm "cập nhật Payment = SUCCESS"
     * ở transaction 1 rồi mới gọi {@code confirm} ở transaction 2, thì một sự cố giữa hai
     * bước (process chết, DB mất kết nối) để lại trạng thái <b>tiền đã báo nhận mà phòng
     * chưa được xác nhận</b> — khách đã trả tiền mà không có phòng. Gọi bản này bên trong
     * transaction của Payment thì hoặc cả hai cùng commit, hoặc cả hai cùng rollback.
     *
     * <p>Caller <b>phải</b> đã khoá Booking bằng {@code FOR UPDATE} trước khi gọi, và phải
     * theo đúng thứ tự khoá của toàn hệ thống: {@code Room → RoomCalendar → Booking → Payment}
     * (khoá ngược thứ tự giữa hai luồng là công thức gây deadlock).
     *
     * @return booking sau khi đổi trạng thái (vẫn attached vào Session của caller)
     */
    com.vivu.booking.entity.Booking confirmInSession(org.hibernate.Session s, Long bookingId);

    /**
     * Xoá key Redis {@code booking:hold:{id}} — gọi <b>sau khi commit</b>, khi booking không
     * còn ở trạng thái HOLD. Redis không phải nguồn chân lý (xem {@code BookingServiceImpl}),
     * key này chỉ để admin quan sát, nên xoá trễ hoặc xoá lỗi đều không ảnh hưởng nghiệp vụ.
     */
    void forgetHoldKey(Long bookingId);

    /**
     * Đánh dấu 1 HOLD quá hạn thành EXPIRED và trả lịch (BLOCKED → AVAILABLE).
     *
     * @return true nếu đã chuyển trạng thái, false nếu không cần làm gì (đã CONFIRMED/CANCELLED rồi)
     */
    boolean markExpired(Long bookingId);

    /**
     * Scheduler gọi mỗi phút để quét và hết hạn HOLD quá hạn (tối đa 100/lần).
     *
     * @return số booking đã chuyển sang EXPIRED
     */
    int expireHolds();

    /**
     * Admin gia hạn thời gian giữ chỗ (ví dụ khách xin thêm 10 phút).
     *
     * @param bookingId id booking đang HOLD
     * @param minutes   số phút cộng thêm vào {@code holdExpiresAt}
     * @return booking đã gia hạn
     */
    BookingResponse extendHold(Long bookingId, int minutes);
}
