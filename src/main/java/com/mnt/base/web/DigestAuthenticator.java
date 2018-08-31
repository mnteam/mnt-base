/**
 * $Revision: 1.0
 * $Date: 2013-5-21
 *
 * Copyright (C) 2013-2020 MNT. All rights reserved.
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
 * 
 */

package com.mnt.base.web;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.BeanContext;
import com.mnt.base.util.CommonUtil;




/**
 * Provide the method to verify the http digest authentication
 *  
 * refer the doc: http://en.wikipedia.org/wiki/Digest_access_authentication
 * 
 * 
 * HA1 = MD5(A1) = MD5(username:realm:password) 
 * 
 * If the qop directive's value is "auth" or is unspecified, then HA2 is
 * HA2 = MD5(A2) = MD5(method:digestURI)
 * If the qop directive's value is "auth-int", then HA2 is
 * HA2 = MD5(A2) = MD5(method:digestURI:MD5(entityBody))
 * If the qop directive's value is "auth" or "auth-int", then compute the response as follows:
 * response = MD5(HA1:nonce:nonceCount:clientNonce:qop:HA2)
 * If the qop directive is unspecified, then compute the response as follows:
 * response = MD5(HA1:nonce:HA2)
 * 
 * @author Peng Peng
 * #date 2012-10-19
 *
 *
 */
public class DigestAuthenticator {
	
	private static final Log log = LogFactory.getLog(DigestAuthenticator.class);
	
	public static String AUTHENTICATED_FLAG_KEY	= "__authPassed";
	private static String AUTH_INFO_MAP			= "__authInfoMap";
	
	private static AuthenticatorProvider authProvider;
	
	private static ThreadLocal<String> authUser 	= new ThreadLocal<String>();
	
	public static boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
		
		boolean result = false;
		
		HttpSession session = req.getSession();
		
		if(session != null) {
			
			result = session.getAttribute(AUTHENTICATED_FLAG_KEY) != null;
			
			if(!result) {
				
				session.setMaxInactiveInterval(60);
				
				Map<String, Object> authInfoMap = CommonUtil.uncheckedMapCast(session.getAttribute(AUTH_INFO_MAP));
				
				if(authInfoMap == null) {
					authInfoMap = new HashMap<String, Object>();
					session.setAttribute(AUTH_INFO_MAP, authInfoMap);
				}
				
				String authentication = req.getHeader("Authorization");
				
				if(CommonUtil.isEmpty(authentication) || !authentication.startsWith("Digest ")) {
					
					postAuthRequired(req, resp, authInfoMap);
					
				} else {
					result = authenticate(req.getMethod(), authentication, authInfoMap);
					
					if(result) {
						
						if(authProvider != null) {
							try {
								authProvider.authenticated(authUser.get(), true);
							} catch(Exception e) {
								log.error("error while invoke the authProvider.authenticated: " + authUser.get(), e);
							}
						}
						session.setAttribute(AUTHENTICATED_FLAG_KEY, true);
						session.removeAttribute(AUTH_INFO_MAP);
						authInfoMap.clear();
						authInfoMap = null;
						
						session.setMaxInactiveInterval(1800);
					} else {
						authProvider.authenticated(authUser.get(), false);
						authInfoMap.clear();
						postAuthRequired(req, resp, authInfoMap);
					}
				}
			}
		} else {
			System.err.println("Just support session available authentication.");
		}
		
		return result;
	}
	
	/**
	 * WWW-Authenticate: Digest realm="testrealm@host.com",
     *                   qop="auth,auth-int",
     *                   nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
     *                   opaque="5ccc069c403ebaf9f0171e9517f40e41"
	 * @param req 
	 *
	 * @param resp
	 * @param authInfoMap
	 */
	private static void postAuthRequired(HttpServletRequest req, HttpServletResponse resp,
			Map<String, Object> authInfoMap) {
		
		resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		
		StringBuilder sb = new StringBuilder();
		String siteHost;
		siteHost = req.getHeader("Host");
		if(siteHost == null) {
			siteHost = "www.mntplay.com";
		}
		
		sb.append("Digest realm=\"" + siteHost + "\",algorithm=\"md5\",");
		sb.append("qop=\"auth,auth-int\",");
		
		String nonce = (String)authInfoMap.get("nonce");
		
		if(nonce == null) {
			nonce = UUID.randomUUID().toString();
		}
		
		String opaque = UUID.randomUUID().toString();
		
		sb.append("nonce=\"" + nonce + "\",");
		sb.append("opaque=\"" + opaque + "\"");
		
		authInfoMap.put("nonce", nonce);
		resp.setHeader("WWW-Authenticate", sb.toString());
		
		try {
			resp.flushBuffer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Authorization: Digest username="Mufasa",
     *                realm="testrealm@host.com",
     *                nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093",
     *                uri="/dir/index.html",
     *                qop=auth,
     *                nc=00000001,
     *                cnonce="0a4f113b",
     *                response="6629fae49393a05397450978507c4ef1",
     *                opaque="5ccc069c403ebaf9f0171e9517f40e41"
	 *
	 * HA1 = MD5(A1) = MD5(username:realm:password) 
	 * 
	 * If the qop directive's value is "auth" or is unspecified, then HA2 is
	 * HA2 = MD5(A2) = MD5(method:digestURI)
	 * If the qop directive's value is "auth-int", then HA2 is
	 * HA2 = MD5(A2) = MD5(method:digestURI:MD5(entityBody))
	 * 
	 * 
	 * If the qop directive's value is "auth" or "auth-int", then compute the response as follows:
	 * response = MD5(HA1:nonce:nonceCount:clientNonce:qop:HA2)
	 * If the qop directive is unspecified, then compute the response as follows:
	 * response = MD5(HA1:nonce:HA2)
	 * 
	 * @param authentication
	 * @param authInfoMap
	 * @return
	 */
	private static boolean authenticate(String method, String authentication,
			Map<String, Object> authInfoMap) {
		
		boolean result = false;
		
		String authStr = authentication.substring(7);//remove "Digest " 
		
		Map<String, String> keyVal = new HashMap<String, String>();
		
		StringBuilder key = new StringBuilder();
		StringBuilder val = new StringBuilder();
		
		boolean keyFlag = true;
		boolean quotFlag = false;
		
		char[] chs = authStr.toCharArray();
		for(char ch : chs) {
			
			if('=' == ch && !quotFlag) {
				keyFlag = false;
			} else if(',' == ch && !quotFlag) {
				keyFlag = true;
				keyVal.put(key.toString().trim(), val.toString().trim());
				
				key.setLength(0);
				val.setLength(0);
				
			} else if('"' == ch) {
				quotFlag = !quotFlag;
			} else {
				if(keyFlag) {
					key.append(ch);
				} else {
					val.append(ch);
				}
			}
		}
		
		keyVal.put(key.toString().trim(), val.toString().trim());
		
		String validNonce = (String)authInfoMap.remove("nonce");
		
		//String nonce = keyVal.get("nonce");
		
		//System.out.println("nonce: [" + keyVal.get("nonce") + "]");
		//System.out.println("validNonce: [" + validNonce + "]");
		
		if(validNonce != null && validNonce.equals(keyVal.get("nonce"))) {
			String username = keyVal.get("username");
			authUser.set(username);
			String HA1 = md5(username, ":", keyVal.get("realm"), ":", getPasswordByUsername(username));
			String digestURI = keyVal.get("uri");
			
			/*
			 * by unknown reason I replaced this question mark, but it block the request including ?
			 * here I commented these sentences, it works well. Not sure if there is any other issue occur in the future.
			 * Just make a note.
			 * 
			 * if(digestURI.contains("?")) {
				digestURI = digestURI.replace("?", "");
			}*/
			
			String HA2 = ("auth-int".equals(keyVal.get("qop"))) ? md5(method, ":", digestURI, ":", md5(authentication)) : md5(method, ":", digestURI);
			
			if(keyVal.get("qop") != null) {
				result = md5(HA1, ":", validNonce, ":", keyVal.get("nc"), ":", keyVal.get("cnonce"), ":" , keyVal.get("qop"), ":", HA2).equals(keyVal.get("response"));
			} else {
				result = md5(HA1, ":", validNonce, ":", HA2).equals(keyVal.get("response"));
			}
		}
		
		return result;
	}
	
	private static final char charArray[] = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
	};
	
	static public String md5(String sb, char[] passwd) {
		
		byte[] unencodedPassword = sb.toString().getBytes();
	
		MessageDigest md = null;
	
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			return sb.toString();
		}
	
		md.reset();
		md.update(unencodedPassword);
		
		for(int i = 0; i < passwd.length; i++) {
			md.update((byte)passwd[i]);
		}
		
		byte[] digest = md.digest();
		//StringBuffer buf = new StringBuffer();
	
		/*char j;
		for (int i = 0; i < encodedPassword.length; i++) {
			
			j = (char)((encodedPassword[i] >> 4) & 0xf);
			
			if(j <= 9) {
				buf.append((char)(j + '0'));
			} else {
				buf.append((char)(j + 'a' - 10));
			}
			
			j = (char)((encodedPassword[i]) & 0xf);
			
			if(j <= 9) {
				buf.append((char)(j + '0'));
			} else {
				buf.append((char)(j + 'a' - 10));
			}
		}*/
		
		StringBuffer res = new StringBuffer(digest.length * 2);
			for (int i = 0; i < digest.length; i++) {
			int hashchar = ((digest[i] >>> 4) & 0xf);
			res.append(charArray[hashchar]);
			hashchar = (digest[i] & 0xf);
			res.append(charArray[hashchar]);
		 }
	
		return res.toString();
	}
	
	static public String md5(String... ss) {
		StringBuilder sb = new StringBuilder();
		if(ss != null) {
			for(String si : ss) {
				sb.append(si);
			}
		}
		
		byte[] unencodedPassword = sb.toString().getBytes();
	
		MessageDigest md = null;
	
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			return sb.toString();
		}
	
		md.reset();
		md.update(unencodedPassword);
		byte[] digest = md.digest();
		//StringBuffer buf = new StringBuffer();
	
		/*char j;
		for (int i = 0; i < encodedPassword.length; i++) {
			
			j = (char)((encodedPassword[i] >> 4) & 0xf);
			
			if(j <= 9) {
				buf.append((char)(j + '0'));
			} else {
				buf.append((char)(j + 'a' - 10));
			}
			
			j = (char)((encodedPassword[i]) & 0xf);
			
			if(j <= 9) {
				buf.append((char)(j + '0'));
			} else {
				buf.append((char)(j + 'a' - 10));
			}
		}*/
		
		StringBuffer res = new StringBuffer(digest.length * 2);
			for (int i = 0; i < digest.length; i++) {
			int hashchar = ((digest[i] >>> 4) & 0xf);
			res.append(charArray[hashchar]);
			hashchar = (digest[i] & 0xf);
			res.append(charArray[hashchar]);
		 }
	
		return res.toString();
	}
	
	public static String getPasswordByUsername(String username) {
		
		//System.err.println(username + " :: " + userMap);
		String password = null;
		if(authProvider != null) {
			try {
				password = authProvider.getPasswordByUsername(username);
			} catch(Exception e) {
				log.error("error while invoke authProvider.getPasswordByUsername: " + username, e);
			}
			
		}
		
		if(password == null) {
			password = userMap.get(username);
		}
		
		return password;
	}
	
	private static Map<String, String> userMap = new HashMap<String, String>();
	
	static {
		
		try {
			authProvider = BeanContext.getInstance().getBean("authenticatorProvider", AuthenticatorProvider.class);
		} catch(Exception e){
			// no config, skip it
		}
		
		String ups = BaseConfiguration.getAdminUps();
		
		if(!CommonUtil.isEmpty(ups)) {
			String[] users = ups.split(";");
			
			if(users != null) {
				
				for(String user : users) {
					String[] keyval = user.split(":");
					
					if(keyval.length == 2){
						userMap.put(keyval[0], keyval[1]);
					}
				}
			}
		}
	}
	
	//http://www.javadocexamples.com/java_source/sun/net/www/protocol/http/DigestAuthentication.java.html
	/*public static void main(String[] args){

		// 
		// keyval: {response=4b0e199f300a0420630e146d3294e4f7, cnonce=3b63996689cc9a85, username=admin, nc=00000001, qop=auth, nonce=3dcdefef-c019-4636-bc8f-65238d494422, realm=www.mntplay.com, opaque=f825ef87-c100-4587-b13f-9fea4fbba74f, uri=/monitor/, algorithm=MD5-sess}

		String authStr = "username=\"admin\", realm=\"www.mntplay.com\", nonce=\"3dcdefef-c019-4636-bc8f-65238d494422\", uri=\"/monitor/\", algorithm=MD5-sess, response=\"4b0e199f300a0420630e146d3294e4f7\", opaque=\"f825ef87-c100-4587-b13f-9fea4fbba74f\", qop=auth, nc=00000001, cnonce=\"3b63996689cc9a85\"";
		
		String authentication = "Digest " + authStr;
		
		Map<String, String> keyVal = new HashMap<String, String>();
		
		StringBuilder key = new StringBuilder();
		StringBuilder val = new StringBuilder();
		
		boolean quotFlag = false;
		boolean keyFlag = true;
		
		char[] chs = authStr.toCharArray();
		for(char ch : chs) {
			
			if('=' == ch) {
				keyFlag = false;
			} else if(',' == ch && !quotFlag) {
				keyFlag = true;
				keyVal.put(key.toString().trim(), val.toString().trim());
				
				key.setLength(0);
				val.setLength(0);
				
			} else if('"' == ch) {
				quotFlag = !quotFlag;
			} else {
				if(keyFlag) {
					key.append(ch);
				} else {
					val.append(ch);
				}
			}
		}
		
		keyVal.put(key.toString().trim(), val.toString().trim());
		
		
		String username = keyVal.get("username");
		String HA1 = md5(username + ":" + keyVal.get("realm") + ":", getPasswordByUsername(username).toCharArray());
		String digestURI = "http://localhost:9090/monitor/"; //keyVal.get("uri"); //
		
		String validNonce = keyVal.get("nonce");
		boolean result = false;
		
		String method = "GET";
		
		String HA2 = ("auth-int".equals(keyVal.get("qop"))) ? md5(method, ":", digestURI, ":", md5(authentication)) : md5(method, ":", digestURI);
		
		if(keyVal.get("qop") != null) {
			
			result = md5(HA1, ":", validNonce, ":", keyVal.get("nc"), ":", keyVal.get("cnonce"), ":auth:", HA2).equals(keyVal.get("response"));
		} else {
			result = md5(HA1, ":", validNonce, ":", HA2).equals(keyVal.get("response"));
		}
		
		System.out.println(keyVal);
		
		System.out.println(md5(HA1, ":", validNonce, ":", keyVal.get("nc"), ":", keyVal.get("cnonce"), ":", keyVal.get("qop"), ":", HA2));

		System.out.println(result);
	}*/
}
