package com.vivu.booking.service.impl;

import com.vivu.booking.common.PageResponse;
import com.vivu.booking.dao.VoucherDao;
import com.vivu.booking.dto.request.VoucherCreateRequest;
import com.vivu.booking.dto.request.VoucherUpdateRequest;
import com.vivu.booking.dto.response.VoucherResponse;
import com.vivu.booking.entity.Voucher;
import com.vivu.booking.entity.VoucherUsage;
import com.vivu.booking.enums.VoucherOwnerType;
import com.vivu.booking.exception.BusinessException;
import com.vivu.booking.exception.ResourceNotFoundException;
import com.vivu.booking.mapper.VoucherMapper;
import com.vivu.booking.service.VoucherService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoucherServiceImpl implements VoucherService {
 private static final Logger log= LoggerFactory.getLogger(VoucherServiceImpl.class);
private final  VoucherDao voucherDao;

    public VoucherServiceImpl(VoucherDao voucherDao) {
        this.voucherDao = voucherDao;
    }

    @Override
    public VoucherResponse create(VoucherCreateRequest req) {
        if (voucherDao.existByCode(req.getCode())){
            throw new BusinessException(409, "Voucher code already exists: "+req.getCode());
        }
        Voucher entity = VoucherMapper.toEntity(req);
        voucherDao.save(entity);
        log.info("Voucher created id={} code={}",entity.getId(), entity.getCode());
        return VoucherMapper.toResponse(entity);
    }

    @Override
    public VoucherResponse getById(Long id) {
        Voucher v = voucherDao.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Voucher not found: "+id));
        return VoucherMapper.toResponse(v);
    }

    @Override
    public PageResponse<VoucherResponse> list(VoucherOwnerType type, VoucherUsage usage, String keyword, int page, int size) {
        return null;
    }

    @Override
    public VoucherResponse update(Long id, VoucherUpdateRequest req) {
        Voucher v = voucherDao.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Voucher not found: "+id));
        if (req.getUser() != null) v.setOwner(req.getUser());
        if (req.getType() != null) v.setOwnerType(req.getType());
        if (req.getDiscountType() != null) v.setDiscountType(req.getDiscountType());
        if (req.getDiscountValue() != null) v.setDiscountValue(req.getDiscountValue());
        if (req.getMinNights() != null) v.setMinNights(req.getMinNights());
        if (req.getMinOrderValue() != null) v.setMinOrderValue(req.getMinOrderValue());
        if (req.getTargetRank() != null) v.setTargetRank(req.getTargetRank());
        if (req.getValidFrom() != null) v.setValidFrom(req.getValidFrom());
        if (req.getValidTo() != null) v.setValidTo(req.getValidTo());
        if (req.getUsageLimitTotal() != null) v.setUsageLimitTotal(req.getUsageLimitTotal());
        if (req.getUsageLimitPerUser() != null) v.setUsageLimitPerUser(req.getUsageLimitPerUser());
        Voucher merged = voucherDao.update(v);
        return VoucherMapper.toResponse(merged);
    }

    @Override
    public void delete(Long id) {
        Voucher v = voucherDao.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Voucher not found "+id));
        voucherDao.deleteById(id);
        voucherDao.update(v);
    }
}
