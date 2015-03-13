package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse

/**
 * Servlet that shows memory smaps from /proc/self/smaps on Linux and returns it as a response
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/memsmap")
class MemorySmapServlet extends GenericServlet {
    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "memsmap")) {
            doMemSmap(out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doMemSmap(PrintWriter out) {
        def smaps = new File("/proc/self/smaps").text
        out << smaps
    }
}

