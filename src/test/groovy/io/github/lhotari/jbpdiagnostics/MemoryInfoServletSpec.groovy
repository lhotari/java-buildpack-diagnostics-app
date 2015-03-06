package io.github.lhotari.jbpdiagnostics

import spock.lang.Specification

/**
 * Created by lari on 06/03/15.
 */
class MemoryInfoServletSpec extends Specification {

    def "should parse ps output in CF env"(){
        given:
        MemoryInfoServlet servlet = new MemoryInfoServlet()
        StringWriter stringWriter = new StringWriter()
        when:
        servlet.printMemSummary(new PrintWriter(stringWriter, true),
                '''  PID  PPID   RSS    VSZ %MEM CPU     TIME COMMAND
   31     1 352376 2385056  1.0 - 00:00:12 java
   59    31  1388   6908  0.0   - 00:00:00 ps
    1     0   828   1132  0.0   - 00:00:00 wshd''' ,8, "31" )
        println stringWriter
        then:

        stringWriter.toString() == '''OS memory report

Memory usage of current process (pid 31):
Resident set size (rss): 344M
Virtual size (vsz): 2329M

Total RSS of all listed processes: 346M - 354592K

'''


    }
}
