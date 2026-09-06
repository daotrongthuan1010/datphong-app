package com.vivu.booking.service.impl;

import com.vivu.booking.dao.BookingDao;
import com.vivu.booking.dao.PaymentDao;
import com.vivu.booking.dto.request.PaymentRequest;
import com.vivu.booking.dto.response.PaymentResponse;
import com.vivu.booking.entity.Booking;
import com.vivu.booking.entity.Payment;
import com.vivu.booking.enums.BookingStatusType;
import com.vivu.booking.enums.PaymentMethodType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.PaymentMapper;
import com.vivu.booking.payment.gateway.PaymentGateway;
import com.vivu.booking.service.PaymentService;
import com.vivu.booking.payment.gateway.vnpay.VnPayGateway;

import java.time.LocalDateTime;

public class PaymentServiceImpl implements PaymentService {
    private final PaymentDao paymentDao;
    private final BookingDao bookingDao;
    private final PaymentGateway vnPayGateway;

    public PaymentServiceImpl(PaymentDao paymentDao, BookingDao bookingDao, PaymentGateway paymentGateway) {
        this.paymentDao = paymentDao;
        this.bookingDao = bookingDao;
        this.vnPayGateway = paymentGateway;
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest req,Long userId,String clientIp) {
        Long bookingId=req.getBookingId();
        Booking booking= bookingDao.findByIdWithRoom(bookingId)
                .orElseThrow(()->new ResourceNotFoundException("Booking not found"));
        if(!booking.getUser().getId().equals(userId)){
            throw new BusinessException(403,"Bạn không có quyền thanh toán booking này");
        }
        if(booking.getStatus()!= BookingStatusType.HOLD){
            throw new BusinessException(400,"Booking không  trạng thái thanh toán");
        }
        if(booking.getHoldExpiresAt()==null|| !booking.getHoldExpiresAt().isAfter(LocalDateTime.now())){
            booking.setStatus(BookingStatusType.EXPIRED);
            bookingDao.update(booking);
            throw new BusinessException(410,"Booking đã hết hạn thời gian giữ chỗ");
        }
        if(paymentDao.findByBookingId(bookingId).isPresent()){
            throw new BusinessException(409,"Booking này đã có giao dịch thanh toán");
        }
        if(req.getMethod()!= PaymentMethodType.VNPAY){
            throw new BusinessException(400,"Phương thức thanh toán hiện chưa được hỗ trợ: "+req.getMethod());
        }
        Payment payment=PaymentMapper.toEntity(req);
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalPrice());
        paymentDao.save(payment);
        bookingDao.update(booking);
        String paymenUrl= vnPayGateway.createPaymentUrl(payment,booking,clientIp,req.getReturnUrl());
        PaymentResponse response=PaymentMapper.toResponse(payment);
        response.setPaymentUrl(paymenUrl);
        return response;
    }
}
