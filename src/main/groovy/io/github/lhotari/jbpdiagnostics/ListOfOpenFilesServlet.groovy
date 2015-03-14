package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse

/**
 * Servlet for accessing lsof
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/lsof")
class ListOfOpenFilesServlet extends GenericServlet {
    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "lsof")) {
            doLsof(out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doLsof(PrintWriter out) {
        def lsofOutput = "lsof -n -p ${DiagUtils.currentProcessId}".execute().text
        out << lsofOutput
    }
}

