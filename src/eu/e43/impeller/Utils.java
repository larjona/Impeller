/* Copyright 2013 Owen Shepherd. A part of Impeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.e43.impeller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

public class Utils {

	static public String readAll(Reader r) throws IOException {
		int nRead;
		char[] buf = new char[16 * 1024];
		StringBuilder bld = new StringBuilder();
		while((nRead = r.read(buf)) != -1) {
			bld.append(buf, 0, nRead);
		}
		return bld.toString();
	}

	static public String readAll(InputStream s) throws IOException {
		return readAll(new InputStreamReader(s, "UTF-8"));
	}
	
	static public String encode(Map<String, String> params) {
		try {
			StringBuilder sb = new StringBuilder();
			for(Map.Entry<String, String> entry : params.entrySet()) {
				if(sb.length() > 0) {
					sb.append('&');
				}
				sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				sb.append('=');
				sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
			}
			
			return sb.toString();
		} catch(UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static Map<String, String> getQueryMap(String query)  {
		try {
			String[] params = query.split("&");  
			Map<String, String> map = new HashMap<String, String>();  
			for (String param : params) {
				String[] parts = param.split("=", 2);
				String name;
					name = URLDecoder.decode(parts[0], "UTF-8");
				String value = URLDecoder.decode(parts[1], "UTF-8");  
				map.put(name, value);  
		     }  
		     return map;  
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	 }

	public static byte[] sha1(String text) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(text.getBytes("utf-8"), 0, text.length());
			return md.digest();
		} catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch(UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
    }
	
	private static final char[] HEX_DIGITS = new char[] {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
		'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	public static String sha1Hex(String text) {
		byte[] sha = sha1(text);
		char[] hex = new char[sha.length * 2];
		
		for(int i = 0; i < sha.length; i++) {
			hex[2 * i + 0] = HEX_DIGITS[sha[i]        & 0x0F];
			hex[2 * i + 1] = HEX_DIGITS[(sha[1] >> 4) & 0x0F];
		}
		
		return new String(hex);
	}
	
	public static String getProxyUrl(JSONObject obj) {
		if(obj.has("pump_io")) {
			JSONObject pump_io = obj.optJSONObject("pump_io");
			String url = pump_io.optString("proxyURL", null);
			if(url == null || url.length() == 0) return null;
			return url;
		} else return null;
	}
	
	public static String getImageUrl(JSONObject img) {
		String url = getProxyUrl(img);
		if(url == null)
			url = img.optString("url");
		return url;
	}
}
