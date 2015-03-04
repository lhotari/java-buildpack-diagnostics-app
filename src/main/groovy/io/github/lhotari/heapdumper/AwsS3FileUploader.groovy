package io.github.lhotari.heapdumper

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.joda.time.LocalDateTime
import org.mule.util.compression.GZIPCompressorInputStream

/**
 * Utility class for uploading files to AWS S3
 *
 * Created by lari on 03/03/15.
 */
class AwsS3FileUploader {
    AmazonS3Client s3Client
    String bucketName
    String s3endpoint
    int expiresInHours = 48

    AwsS3FileUploader(String accessKey, String secretKey, String bucketName, String s3endpoint = null) {
        s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey))
        if(s3endpoint) {
            s3Client.setRegion(RegionUtils.getRegionByEndpoint(s3endpoint))
        }
        this.bucketName = bucketName
    }

    String uploadFileAndPresignUrl(File file) {
        generatePresignedUrl(uploadFile(file))
    }

    String uploadFile(File file) {
        s3Client.putObject(new PutObjectRequest(bucketName, file.name, file))
        return file.name
    }

    String gzipAndUploadFile(File file) {
        String name =  "${file.name}.gz"
        s3Client.putObject(new PutObjectRequest(bucketName, name, new GZIPCompressorInputStream(file.newInputStream()), new ObjectMetadata()))
        return name
    }

    String generatePresignedUrl(String objectKey) {
        def request = new GeneratePresignedUrlRequest(bucketName, objectKey)
        request.setExpiration(LocalDateTime.now().plusHours(expiresInHours).toDate())
        s3Client.generatePresignedUrl(request)
    }

}
