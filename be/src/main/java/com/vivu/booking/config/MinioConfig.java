package com.vivu.booking.config;

import com.vivu.booking.utils.AppProperties;
import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    // Tạo bucket nếu chưa tồn tại
    public static void createBucket(String bucketName)
            throws Exception {

        MinioClient minioClient = getClient();

        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!exists) {

            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info(
                    "MinIO bucket created: {}",
                    bucketName
            );
        }
    }

    // Upload file
    public static void upload(
            String bucketName,
            String objectName,
            InputStream inputStream,
            long size,
            String contentType
    ) throws Exception {

        getClient().putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(
                                inputStream,
                                size,
                                -1
                        )
                        .contentType(contentType)
                        .build()
        );

        log.info(
                "Upload success: {}/{}",
                bucketName,
                objectName
        );
    }

    // Xoá object khỏi MinIO (khi admin xoá ảnh/video của phòng)
    public static void removeObject(
            String bucketName,
            String objectName
    ) throws Exception {

        getClient().removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );

        log.info(
                "Remove object success: {}/{}",
                bucketName,
                objectName
        );
    }

    // Tạo URL
    public static String getObjectUrl(
            String bucketName,
            String objectName
    ) {

        String endpoint = AppProperties.get(
                "minio.endpoint",
                "http://103.216.117.40:9000"
        );

        return endpoint
                + "/"
                + bucketName
                + "/"
                + objectName;
    }

    // URL tạm thời nếu bucket private
    public static String getPresignedUrl(
            String bucketName,
            String objectName
    ) throws Exception {

        return getClient().getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(
                                1,
                                TimeUnit.HOURS
                        )
                        .build()
        );
    }
    public static void setPublic(String bucketName) throws Exception {

        String policy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": [
                    "s3:GetObject"
                  ],
                  "Resource": [
                    "arn:aws:s3:::%s/*"
                  ]
                }
              ]
            }
            """.formatted(bucketName);

        getClient().setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build()
        );

        log.info("MinIO bucket set to PUBLIC: {}", bucketName);
    }
    public static String uploadExcel(
            byte[] data,
            String folder,
            String fileName
    ) throws Exception {

        String bucketName = getBucket();

        createBucket(bucketName);

        String objectName =
                folder
                        + "/"
                        + UUID.randomUUID()
                        + "_"
                        + fileName;

        try (InputStream inputStream =
                     new ByteArrayInputStream(data)) {

            upload(
                    bucketName,
                    objectName,
                    inputStream,
                    data.length,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );
        }

        log.info(
                "Upload Excel success: {}/{}",
                bucketName,
                objectName
        );

        return getObjectUrl(
                bucketName,
                objectName
        );
    }
}

