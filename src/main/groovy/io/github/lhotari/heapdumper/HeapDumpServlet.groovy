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
 * Meant to be used for CloudFoundry applications
 *
 * expects that you have these environment variables specified in your CloudFoundry application instance
 * JBPDIAG_AWS_BUCKET - the AWS S3 bucket to use
 * JBPDIAG_AWS_ACCESS_KEY - the AWS access key id that has access to the S3 bucket
 * JBPDIAG_AWS_SECRET_KEY - the AWS secret key for the previous
 * JBPDIAG_TOKEN - secret token to request the heapdump via /heapdumper?TOKEN=thesecret_token
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/heapdump")
class HeapDumpServlet extends GenericServlet {
    HotSpotDiagnosticMXBean hotSpotDiagnosticMXBean
    Set<String> secretKeys
    AwsS3FileUploader s3Uploader
    Cloud cloud
    String fileNameBase = "heapdump"
    boolean logInfoToFiles = false

    @Override
    void init() throws ServletException {
        try {
            cloud = CloudFactory.newInstance().cloud
        } catch(e) {
        }
        logInfoToFiles = cloud != null

        hotSpotDiagnosticMXBean = ManagementFactory.getPlatformMXBeans(HotSpotDiagnosticMXBean.class).get(0);

        secretKeys = ((System.getenv("JBPDIAG_TOKEN") ?: UUID.randomUUID().toString()).split(/,/)).findAll{ it } as Set
        println "HeapDumpServlet initializing. allowed TOKENs: ${secretKeys.join(', ')}"
        if(logInfoToFiles) new File(System.getProperty("user.home"), ".heapdumpservlet.tokens").text = secretKeys.join(',')

        if(System.getenv("JBPDIAG_AWS_ACCESS_KEY")) {
            s3Uploader = new AwsS3FileUploader(System.getenv("JBPDIAG_AWS_ACCESS_KEY"), System.getenv("JBPDIAG_AWS_SECRET_KEY"), System.getenv("JBPDIAG_AWS_BUCKET"))
            println "Uploading to $s3Uploader.bucketName"
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
        def accessToken = req.getParameter("TOKEN")
        if(accessToken && accessToken in secretKeys) {
            doHeapDump(out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doHeapDump(PrintWriter out) {
        out.println "Dumping..."
        out.flush()
        File dumpFile = File.createTempFile("$fileNameBase-${new Date().format('yyyy-MM-dd-HH-mm')}-", ".bin")
        dumpFile.delete()
        hotSpotDiagnosticMXBean.dumpHeap(dumpFile.getAbsolutePath(), true)
        out.println "Dumped to $dumpFile"
        out.flush()
        if (s3Uploader) {
            println "Uploading to AWS."
            String filename = s3Uploader.gzipAndUploadFile(dumpFile)
            String link = s3Uploader.generatePresignedUrl(filename)
            out.println "Dump gzipped and uploaded to S3. Download from $link"
            if(logInfoToFiles) {
                new File(System.getProperty("user.home"), ".heapdumpservlet.dumps").withWriterAppend {
                    it.write(link)
                    it.write('\n')
                }
            }
            dumpFile.delete()
        }
    }
}
