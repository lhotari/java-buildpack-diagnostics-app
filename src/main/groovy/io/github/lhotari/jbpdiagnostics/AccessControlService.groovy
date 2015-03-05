package io.github.lhotari.jbpdiagnostics

import groovy.transform.CompileStatic

import javax.servlet.ServletRequest

/**
 * Created by lari on 04/03/15.
 */
@CompileStatic
class AccessControlService {
    static AccessControlService instance = new AccessControlService()
    Set<String> secretKeys

    private AccessControlService() {
        secretKeys = ((System.getenv("JBPDIAG_TOKEN") ?: UUID.randomUUID().toString()).split(/,/)).findAll { it } as Set
    }

    boolean isOperationAllowed(ServletRequest request, String operation) {
        def accessToken = request.getParameter("TOKEN")
        return (accessToken && accessToken in secretKeys)
    }

}
