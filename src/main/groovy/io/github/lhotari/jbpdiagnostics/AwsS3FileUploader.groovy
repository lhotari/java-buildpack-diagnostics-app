package io.github.lhotari.jbpdiagnostics

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.CountingOutputStream
import org.apache.commons.io.output.NullOutputStream
import org.joda.time.LocalDateTime
import org.mule.util.compression.GZIPCompressorInputStream

/**
 * Utility class for uploading files to AWS S3
 *
 * Created by lari on 03/03/15.
 */
@CompileStatic
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

    String gzipAndUploadFile(File file, boolean noTempFile = true) {
        String name =  "${file.name}.gz"
        if(noTempFile) {
            long contentLength = calculateGzippedLength(file)
            def metadata = new ObjectMetadata()
            metadata.setContentLength(contentLength)
            createGzipCompressingInputStream(file).withStream { InputStream input ->
                s3Client.putObject(new PutObjectRequest(bucketName, name, input, metadata))
            }
        } else {
            File tempFile = new File(file.getParentFile(), name)
            try {
                tempFile.withOutputStream { OutputStream output ->
                    createGzipCompressingInputStream(file).withStream { InputStream input ->
                        IOUtils.copy(input, output)
                    }
                }
                s3Client.putObject(new PutObjectRequest(bucketName, name, tempFile))
            } finally {
                tempFile.delete()
            }
        }
        return name
    }

    long calculateGzippedLength(File file) {
        CountingOutputStream countingOutput = new CountingOutputStream(new NullOutputStream())
        createGzipCompressingInputStream(file).withStream { InputStream input ->
            IOUtils.copy(input, countingOutput)
        }
        countingOutput.getByteCount()
    }

    protected GZIPCompressorInputStream createGzipCompressingInputStream(File file) {
        new GZIPCompressorInputStream(file.newInputStream())
    }

    String generatePresignedUrl(String objectKey) {
        def request = new GeneratePresignedUrlRequest(bucketName, objectKey)
        request.setExpiration(LocalDateTime.now().plusHours(expiresInHours).toDate())
        s3Client.generatePresignedUrl(request)
    }

}
