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
public class AddUserRestlet extends BaseAbstractRestlet {

	/* (non-Javadoc)
	 * @see org.seasr.central.ws.servlets.RestServlet#getRestRegularExpression()
	 */
	@Override
	public String getRestRegularExpression() {
		return "/services/user/add/format\\.(txt|json|xml|html)";
	}

	/* (non-Javadoc)
	 * @see org.seasr.central.ws.servlets.RestServlet#process(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean process(HttpServletRequest request,
			HttpServletResponse response, String method, String... values) {

		Map<String,String[]> map = extractTextPayloads(request);

		if ( map.containsKey("screen_name") && map.containsKey("password") && map.containsKey("profile") && 
				map.get("screen_name").length==map.get("password").length && 
				map.get("password").length==map.get("profile").length ) {

			String [] sna = map.get("screen_name");
			String [] pwa = map.get("password");
			String [] pfa = map.get("profile");
			UUID uuid;
			JSONArray ja = new JSONArray();
			boolean bOK = true;
			for ( int i=0, iMax=sna.length ; bOK && i<iMax ; i++ ) {
				try {
					JSONObject joProfile = new JSONObject(pfa[i]);
					uuid = bsl.addUser(sna[i], pwa[i], joProfile);
					JSONObject jo = new JSONObject();
					if ( uuid==null ) {
						// Could not add the user
						JSONObject error = new JSONObject();
						UUID oldUuid = bsl.getUserUUID(sna[i]);
						error.put("text", "User screen name "+sna[i]+" already exist");
						error.put("uuid", oldUuid);
						error.put("created_at", bsl.getUserCreationTime(oldUuid));
						error.put("profile", bsl.getUserProfile(sna[i]));
						jo.put("error_msg", error);
					}
					else {
						// User added successfully	
						jo.put("uuid", uuid.toString());
						jo.put("screen_name", sna[i]);
						jo.put("created_at", bsl.getUserCreationTime(uuid));
						jo.put("profile", joProfile);
					}
					ja.put(jo);
				} catch (JSONException e) {
					bOK = false;
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
