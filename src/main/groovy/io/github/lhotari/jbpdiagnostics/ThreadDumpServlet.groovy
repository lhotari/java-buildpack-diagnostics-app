package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse

/**
 * Servlet that does a threaddump and returns it as a response
 *
 * JBPDIAG_TOKEN - secret token to request the threaddump via /threaddump?TOKEN=thesecret_token
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/threaddump")
class ThreadDumpServlet extends GenericServlet {
    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "threaddump")) {
            doThreadDump(out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doThreadDump(PrintWriter out) {
        try {
            out.write(DiagnosticCommandMBeanHelper.threadPrint())
        } catch (Exception e) {
            throw new RuntimeException("Problem doing threaddump with DiagnosticCommandMBean.threadPrint JMX invocation", out)
        }
    }
}

