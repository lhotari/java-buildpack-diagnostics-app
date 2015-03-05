package io.github.lhotari.jbpdiagnostics

import javax.servlet.GenericServlet
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.MemoryPoolMXBean
import java.lang.management.MemoryUsage

/**
 * Servlet that shows memory usage information and returns it as a response
 *
 * Created by lari on 03/03/15.
 */
@WebServlet("/meminfo")
class MemoryInfoServlet extends GenericServlet {
    static int BYTES_PER_MB = 1024 * 1000;

    @Override
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        def out=((HttpServletResponse)res).getWriter()
        if(AccessControlService.instance.isOperationAllowed(req, "meminfo")) {
            doMemInfo(out)
        } else {
            out << "NOT OK"
        }
        out.close()
    }

    protected synchronized void doMemInfo(PrintWriter out) {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean()

        out << "JVM memory usage\n"
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        out << formatMemoryUsage("Heap", memoryMXBean.getHeapMemoryUsage()) << "\n";
        out << formatMemoryUsage("Non-Heap", memoryMXBean.getNonHeapMemoryUsage())  << "\n";
        out << "\nMemory pools\n"
        def memPoolBeans = ManagementFactory.getMemoryPoolMXBeans().sort { it.type }
        for(MemoryPoolMXBean memoryPoolMXBean in memPoolBeans) {
            out << formatMemoryUsage(memoryPoolMXBean.name, memoryPoolMXBean.usage) << "\n"
            out << formatMemoryUsage(memoryPoolMXBean.name + " peak", memoryPoolMXBean.peakUsage) << "\n"
        }
    }

    String formatMemoryUsage(String name, MemoryUsage memoryUsage) {
        String.format("%-35s used: %4dM committed: %4dM max: %4dM", name, toMB(memoryUsage.getUsed()), toMB(memoryUsage.getCommitted()), toMB(memoryUsage.getMax()));
    }

    int toMB(Number number) {
        (number / BYTES_PER_MB) as Integer
    }
}

