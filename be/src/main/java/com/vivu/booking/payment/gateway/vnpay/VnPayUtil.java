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

    /**
     * Xac minh chu ky cua VNPay khi redirect ve (return URL).
     *
     * <p>VNPay ky tren chuoi query da <b>decode</b> (khong encode lai), loai {@code vnp_SecureHash}
     * va {@code vnp_SecureHashType}, sap xep key theo thu tu tu dien. Vi vay o day phai dung gia tri
     * goc tu {@code request.getParameter} (container da decode) — khong duoc encode lai nhu khi build.
     *
     * @return true neu chu ky khop
     */
    public static boolean verifyReturnParams(Map<String, String> params, String secureHash) {
        if (secureHash == null || secureHash.isBlank()) return false;
        List<String> fields = new ArrayList<>(params.keySet());
        fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        Collections.sort(fields);
        StringBuilder hashData = new StringBuilder();
        for (String f : fields) {
            String v = params.get(f);
            if (v == null || v.isBlank()) continue;
            if (!hashData.isEmpty()) hashData.append("&");
            hashData.append(f).append("=").append(v);
        }
        String expected = hmacSHA512(VnPayConfig.HASH_SECRET, hashData.toString());
        return expected.equalsIgnoreCase(secureHash);
    }
}