package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse

/**
 * Servlet for accessing netstat
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/netstat")
class NetstatServlet extends GenericServlet {
    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "netstat")) {
            doNetstat(req, out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doNetstat(ServletRequest req, PrintWriter out) {
        def params = DiagUtils.macOs ? "-an" : "-anp"
        if(req.getParameter("stat")) {
            params = "-s"
        }
        out << "netstat ${params}".execute().text
    }
}

