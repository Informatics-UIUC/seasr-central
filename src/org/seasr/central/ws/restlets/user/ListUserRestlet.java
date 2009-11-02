/**
 * 
 */
package org.seasr.central.ws.restlets.user;

import static org.seasr.central.ws.restlets.Tools.exceptionToText;
import static org.seasr.central.ws.restlets.Tools.log;
import static org.seasr.central.ws.restlets.Tools.sendContent;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.seasr.central.ws.restlets.BaseAbstractRestlet;

/** This servlet implements list user functionality.
 * 
 * @author xavier
 *
 */
public class ListUserRestlet extends BaseAbstractRestlet {

	
	/* (non-Javadoc)
	 * @see org.seasr.central.ws.servlets.RestServlet#getRestRegularExpression()
	 */
	@Override
	public String getRestRegularExpression() {
		return "/services/user/list/format\\.(txt|json|xml|html)";
	}

	/* (non-Javadoc)
	 * @see org.seasr.central.ws.servlets.RestServlet#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean process(HttpServletRequest request,
			HttpServletResponse response, String method, String... values) {
		String format = values[0];
		
		long offset = 0;
		long count  = Long.MAX_VALUE;

		String sCount = request.getParameter("count");
		String sOffset = request.getParameter("offset");

		if ( sCount!=null ) count = Long.parseLong(sCount);
		if ( sOffset!=null ) count = Long.parseLong(sOffset);
		
		JSONArray ja = bsl.listUsers(count, offset);
		
		try {
			sendContent(response, ja, format);
			return true;
		} catch (IOException e) {
			log.warning(exceptionToText(e));
			return false;
		}
	}

	

}
