/**
 * 
 */
package org.seasr.central.ws.restlets;

import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.ws.SC;

/** This servlet implements a base abstract restlet.
 * 
 * @author xavier
 *
 */
public abstract class BaseAbstractRestlet implements RestServlet {

	/** The parent SC object */
	protected SC sc;
	
	/** The back end storage link */
	protected BackendStorageLink bsl;
	
	@Override
	public void setSCParent(SC sc) {
		this.sc = sc;
		this.bsl = sc.getBackendStorageLink();
	}

	@Override
	public void setBackendStoreLink(BackendStorageLink bsl) {
		this.bsl = bsl;
	}

}
