package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse

/**
 * Servlet for launching tmate ssh access
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/tmatessh")
class TmateSshServlet extends GenericServlet {
    static final String TMATE_SERVER_SCRIPT = "tmate-server.sh"
    int counter = 0

    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "tmatessh")) {
            if(System.getenv("VCAP_APPLICATION")) {
                doTmateSsh(req, out)
            } else {
                out << "Not running in CloudFoundry."
            }
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doTmateSsh(ServletRequest req, PrintWriter out) {
        def homeDir = new File(System.getProperty("user.home"))
        def tmateDir = new File(homeDir, "tmate")
        if(!tmateDir.isDirectory()) {
            installTmate(tmateDir)
        }
        def action = "start"
        if(req.getParameter("action") in ['start', 'stop', 'status']) {
            action = req.getParameter("action")
        }
        def command = "bash ${new File(tmateDir, TMATE_SERVER_SCRIPT).absolutePath} ${action}"
        out << command.execute().text
    }

    protected installTmate(File tmateDir) {
        def classLoader = this.getClass().getClassLoader()
        classLoader.getResourceAsStream('tmate.tar.gz').withStream { InputStream input ->
            ProcessBuilder pb = new ProcessBuilder()
            pb.directory(tmateDir.getParentFile())
            pb.command("tar", "zxf", "-")
            def process = pb.start()
            process.withOutputStream { OutputStream output ->
                output << input
            }
            process.waitFor()
        }
        classLoader.getResourceAsStream(TMATE_SERVER_SCRIPT).withStream { InputStream input ->
            def tmateServerScript = new File(tmateDir, TMATE_SERVER_SCRIPT)
            tmateServerScript.withOutputStream { OutputStream output ->
                output << input
            }
        }
    }
}
