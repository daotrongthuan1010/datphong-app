package com.vivu.booking.config;

import com.vivu.booking.utils.AppProperties;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MinioConfig {
    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);
    private static MinioClient client;
    private static String bucket;

    private MinioConfig() {
    }

    public static synchronized MinioClient getClient() {
        if (client == null) {
            String endpoint = AppProperties.get("minio.endpoint", "http://103.216.117.40:9000");
            String accessKey = AppProperties.get("minio.accessKey", "vephim");
            String secretKey = AppProperties.get("minio.secretKey", "hoclaptrinh@2026");
            bucket = AppProperties.get("minio.bucket", "vivu-bucket");
            client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
            log.info("MinIO client initialized endpoint={} bucket={}", endpoint, bucket);
        }
        return client;
    }

    public static String getBucket() {
        if (bucket == null) getClient();
        return bucket;
    }
}
