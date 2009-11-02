/**
 * 
 */
package org.seasr.central.ws.restlets.user;

import static org.seasr.central.ws.restlets.Tools.errorExpectationFail;
import static org.seasr.central.ws.restlets.Tools.exceptionToText;
import static org.seasr.central.ws.restlets.Tools.extractTextPayloads;
import static org.seasr.central.ws.restlets.Tools.log;
import static org.seasr.central.ws.restlets.Tools.sendContent;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.ws.restlets.BaseAbstractRestlet;

/** This servlet implements add user functionality.
 * 
 * @author xavier
 *
 */
public class DeleteUserRestlet extends BaseAbstractRestlet {

	/* (non-Javadoc)
	 * @see org.seasr.central.ws.servlets.RestServlet#getRestRegularExpression()
	 */
	@Override
	public String getRestRegularExpression() {
		return "/services/user/delete/format\\.(txt|json|xml|html)";
	}

	/* (non-Javadoc)
	 * @see org.seasr.central.ws.servlets.RestServlet#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean process(HttpServletRequest request,
			HttpServletResponse response, String method, String... values) {

		Map<String,String[]> map = extractTextPayloads(request);

		if ( map.containsKey("screen_name") || map.containsKey("uuid") ) {

			String [] sna = map.get("screen_name");
			String [] uua = map.get("uuid");

			boolean bOK = true;
			JSONArray ja = new JSONArray();
			
			if ( sna!=null ) {
				for ( int i=0, iMax=sna.length ; bOK && i<iMax ; i++ ) {
					try {
						JSONObject jo = new JSONObject();
						String sUUID = bsl.getUserUUID(sna[i]).toString();
						if ( bsl.removeUser(sna[i])) {
							// User deleted successfully	
							jo.put("uuid", sUUID);
							jo.put("screen_name", sna[i]);
						}
						else {
							// Could not add the user
							JSONObject error = new JSONObject();
							error.put("text", "User screen name "+sna[i]+" could not be deleted");
							error.put("uuid", sUUID);
							error.put("screen_name", sna[i]);
							jo.put("error_msg", error);
						}
						ja.put(jo);
					} catch (JSONException e) {
						bOK = false;
					}
				}
			}

			if ( uua!=null ) {
				for ( int i=0, iMax=uua.length ; bOK && i<iMax ; i++ ) {
					try {
						JSONObject jo = new JSONObject();
						UUID uuid = UUID.fromString(uua[i]);
						String screenName = bsl.getUserScreenName(uuid);
						if ( bsl.removeUser(uuid)) {
							// User deleted successfully	
							jo.put("uuid", uuid.toString());
							jo.put("screen_name", screenName);
						}
						else {
							// Could not add the user
							JSONObject error = new JSONObject();
							error.put("text", "User with UUID "+uua[i]+" could not be deleted");
							error.put("uuid", uuid.toString());
							error.put("screen_name", bsl.getUserScreenName(uuid));
							jo.put("error_msg", error);
						}
						ja.put(jo);
					} catch (JSONException e) {
						bOK = false;
					}
				}
			}

			try {
				if ( bOK )
					sendContent(response, ja, values[0]);
			} catch (IOException e) {
				log.warning(exceptionToText(e));
				return false;
			}
			return true;

		} else
			try {
				errorExpectationFail(response);
				return true;
			} catch (IOException e) {
				log.warning(exceptionToText(e));
				return false;
			}
	}

}
