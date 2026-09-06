package com.vivu.booking.payment.gateway.vnpay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
public final class VnPayUtil {
    private VnPayUtil() {}
    public static String hmacSHA512(String key,String data){
        try{
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));}
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo chữ ký VNPay", e);
        }
    }

    public static String buildPaymentUrl(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue == null || fieldValue.isBlank()) {continue;}
            String encodedValue = URLEncoder.encode(
                            fieldValue,
                            StandardCharsets.US_ASCII
                    );

            if (!hashData.isEmpty()) {hashData.append("&");query.append("&");}
            hashData
                    .append(fieldName)
                    .append("=")
                    .append(encodedValue);
            query
                    .append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII))
                    .append("=")
                    .append(encodedValue);
        }
        String secureHash = hmacSHA512(VnPayConfig.HASH_SECRET, hashData.toString());
        return VnPayConfig.PAY_URL
                + "?"
                + query
                + "&vnp_SecureHash="
                + secureHash;
    }
}