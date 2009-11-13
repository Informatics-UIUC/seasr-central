/**
 * 
 */
package org.seasr.central.ws.restlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.ws.SC;


/** This interface provides the basic interface to implement a REST based service.
 * 
 * @author xavier
 *
 */
public interface RestServlet {

	/** Set the SC central.
	 * 
	 * @param sc SEASR Central reference
	 */
	public void setSCParent ( SC sc );
	
	/** Set the back end store link.
	 * 
	 * @param bsl The back end store link reference
	 */
	public void setBackendStoreLink ( BackendStorageLink bsl );
	
	/** Return the regular expression used for the restful service.
	 * 
	 * @return The string containing the regular expression
	 */
	public String getRestContextRegexp ();
	
	/** Process a matching restfull request.
	 * 
	 * @param request The request object
	 * @param response The response object
	 * @param method The HTTP method used 
	 * @param values The extracted values from the rest request
	 * @return True is the request is processed and no further attempts should be ma
	 */
	public boolean process ( HttpServletRequest request, HttpServletResponse response, String method, String...values ) ;

}
