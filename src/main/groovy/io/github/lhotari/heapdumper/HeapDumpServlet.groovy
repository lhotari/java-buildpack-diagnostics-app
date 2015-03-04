package io.github.lhotari.heapdumper

import com.sun.management.HotSpotDiagnosticMXBean
import org.springframework.cloud.Cloud
import org.springframework.cloud.CloudFactory

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory

/**
 * Servlet that does a heapdump and uploads the file to S3
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/heapdump")
class HeapDumpServlet extends GenericServlet {
    HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean
    Set<String> secretKeys = ((System.getenv("HEAPDUMP_TOKEN") ?: UUID.randomUUID().toString()).split(/,/)).findAll{ it } as Set
    AwsS3FileUploader s3Uploader
    Cloud cloud
    String fileNameBase = "heapdump"

    @Override
    void init() throws ServletException {
        hotSpotDiagnosticMXBean = ManagementFactory.getPlatformMXBeans(HotSpotDiagnosticMXBean.class).get(0);
        println "HeapDumpServlet initializing. allowed TOKENs: ${secretKeys.join(', ')}"
        if(System.getenv("HEAPDUMP_AWS_ACCESS_KEY")) {
            s3Uploader = new AwsS3FileUploader(System.getenv("HEAPDUMP_AWS_ACCESS_KEY"), System.getenv("HEAPDUMP_AWS_SECRET_KEY"), System.getenv("HEAPDUMP_AWS_BUCKET"))
            println "Uploading to $s3Uploader.bucketName"
        }
        try {
            cloud = CloudFactory.newInstance().cloud
        } catch(e) {
        }
        if(cloud) {
            def appInfo=cloud.getApplicationInstanceInfo()
            fileNameBase = "heapdump-${appInfo.appId}-${appInfo.instanceId}"
        } else {
            fileNameBase = "heapdump-${InetAddress.getLocalHost().getHostName()}"
        }
    }

    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if((req.getParameter("TOKEN")?:'') in secretKeys) {
            out.println "Dumping..."
            out.flush()
            File dumpFile=File.createTempFile("$fileNameBase-${new Date().format('yyyy-MM-dd-HH-mm')}", ".bin")
            dumpFile.delete()
            hotSpotDiagnosticMXBean.dumpHeap(dumpFile.getAbsolutePath(), true)
            out.println "Dumped to $dumpFile"
            out.flush()
            if(s3Uploader) {
                println "Uploading to AWS."
                String link = s3Uploader.uploadFileAndPresignUrl(dumpFile)
                out.println "Dump uploaded to S3. Download from $link"
            }
        } else {
            out << "NOT OK"
        }
        out.close()
    }
}
