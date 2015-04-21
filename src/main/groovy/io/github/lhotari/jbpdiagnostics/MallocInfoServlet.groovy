package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse

/**
 * Servlet that shows malloc_info on Linux and returns it as a response
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/mallocinfo")
class MallocInfoServlet extends GenericServlet {
    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "mallocinfo")) {
            ((HttpServletResponse)res).setHeader("Content-Type", "text/xml");
            doMallocInfo(out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doMallocInfo(PrintWriter out) {
        def mallocInfoFile = File.createTempFile("mallocinfo",".xml")
        MallocInfo.writeMallocInfo(mallocInfoFile)
        out << mallocInfoFile.text
        mallocInfoFile.delete()
    }
}

