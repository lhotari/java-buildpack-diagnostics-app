package io.github.lhotari.jbpdiagnostics;

import com.sun.management.DiagnosticCommandMBean;
import sun.management.ManagementFactoryHelper;

import javax.management.MBeanException;
import javax.management.ReflectionException;

/**
 * Created by lari on 05/03/15.
 */
public class DiagnosticCommandMBeanHelper {

    public static String threadPrint() throws ReflectionException, MBeanException {
        return callDiagnosticsMethod("threadPrint");
    }

    private static String callDiagnosticsMethod(String actionName) throws MBeanException, ReflectionException {
        String[] emptyStringArgs = {};
        Object[] dcmdArgs = {emptyStringArgs};
        String[] signature = {String[].class.getName()};
        DiagnosticCommandMBean dcmd = ManagementFactoryHelper.getDiagnosticCommandMBean();
        return (String)dcmd.invoke(actionName, dcmdArgs, signature);
    }
}
