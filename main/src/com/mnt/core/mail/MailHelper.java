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

package com.mnt.core.mail;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.mnt.core.util.BaseConfiguration;
import com.mnt.core.util.CommonUtil;
import com.sun.mail.util.MailSSLSocketFactory;

public class MailHelper {
	
	private static final Log log = LogFactory.getLog(MailHelper.class);
	
	private static final Session smtpSession;
	
	static {
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.debug", "false");
		props.put("mail.smtp.host", BaseConfiguration.getProperty("mail_server_host"));
		props.put("mail.smtp.port", BaseConfiguration.getIntProperty("mail_server_port", 25));
		
		String authType = BaseConfiguration.getProperty("mail_auth_type");
		
		if("ssl".equals(authType)) {
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
			props.put("mail.smtp.ssl.trust", false);
			props.put("mail.smtps.auth", "true");
			props.put("mail.transport.protocol", "smtps"); 
		} else if("tls".equals(authType)) {
			props.put("mail.smtp.starttls.enable", "true");
			
			MailSSLSocketFactory sf = null;
			try {
				sf = new MailSSLSocketFactory();
			} catch (GeneralSecurityException e1) {
				log.error("fail to construct MailSSLSocketFactory", e1);
			}
	        sf.setTrustAllHosts(true);
	        props.put("mail.smtp.ssl.socketFactory", sf);  //*
		}
		
		String username = BaseConfiguration.getProperty("mail_server_username");
		if(CommonUtil.isEmpty(username)) {
			username = BaseConfiguration.getProperty("mail_server_email");
		}
		
		smtpSession = Session.getDefaultInstance(props,
                new PasswordAuthenticator(username, BaseConfiguration.getProperty("mail_server_password")));
	}

	
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void sendMail(String mailType, Map<String, Object> infoMap) {
        String toArrs =BaseConfiguration.getProperty("mail_notification_receivers").trim();
        sendMail(mailType, toArrs, infoMap);
	}
	
	public static void sendMail(String mailType, String mailTos, Map<String, Object> infoMap) {
		
		String[] mailtemplate = loadMailTemplate(mailType);
		
		if(mailtemplate != null && mailtemplate.length == 2) {
			String from = BaseConfiguration.getProperty("mail_server_email");
	        String subject = buildMailContent(mailtemplate[0], infoMap, false);
	        String mailContent = buildMailContent(mailtemplate[1], infoMap, true);
	        
	        
	        Message msg = new MimeMessage(smtpSession);
	        try {
				msg.setFrom(new InternetAddress(from));
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailTos, false));
		        msg.setSubject(MimeUtility.encodeText(subject,"UTF-8","B"));
		        msg.setContent(mailContent,"text/html;charset=UTF-8");
		        msg.setSentDate(new Date());
		        
		        Transport.send(msg);
			} catch (Exception e) {
				log.error("fail to send the mail: " + msg, e);
			}
		}
	}
	
	private static final String BLOCK_PREFIX 		= "block:";
	private static final int BLOCK_PREFIX_LEN 	= BLOCK_PREFIX.length();

	private static String buildMailContent(String source, Map<String, Object> infoMap, boolean isContent) {
		
		for(String key : infoMap.keySet()) {
			
			if(key.startsWith(BLOCK_PREFIX)) {
				if(isContent) {
					//
					
					String regex = "\\$\\{" + key.substring(BLOCK_PREFIX_LEN) + "\\}\\[#[^#]+#\\]";
					
					Pattern pattern = Pattern.compile(regex);
					Matcher matcher = pattern.matcher(source);
					
					if(matcher.find()) {
						
						String subSourceBase = matcher.group();
						
						String newSubSourceBase = subSourceBase.substring(("${" + key.substring(BLOCK_PREFIX_LEN) + "}[#").length(), subSourceBase.length() - 2);
						
						List<Map<String, Object>> subInfoList = CommonUtil.uncheckedListCast(infoMap.get(key));
						
						StringBuilder sb = new StringBuilder();
						
						if(subInfoList != null) {
							for(Map<String, Object > subInfoMap : subInfoList) {
								sb.append(buildMailContent(newSubSourceBase, subInfoMap, true));// do not support next level block
							}
						} else {
							sb.append("	none<br />");
						}
						
						source = source.replace(subSourceBase, sb);
					}/* else {
						// skip it
					}*/
				}
			} else {
				
				Object value = infoMap.get(key);
				
				source = source.replace("${" + key + "}", value == null ? "-" : value.toString());
			}
		}
		
		source = source.replace("${now}", dateFormatter.format(new Date()));
		
		return source;
	}
	
	private static String[] loadMailTemplate(String templateName) {
		
		String[] titleContents = new String[2]; 
		
		InputStream in = BaseConfiguration.getRelativeFileStream("mail.template.xml");
		
		if(in != null) {
			try {           
				SAXReader reader = new SAXReader(); 
				Document document = reader.read(in);
				Element rootElt = document.getRootElement(); // 
				
				Element mailInfo = (Element)rootElt.selectSingleNode("//templates/mail[@id='" + templateName + "']");
				
				if(mailInfo != null) {
					titleContents[0] = mailInfo.element("title").getText();
					titleContents[1] = mailInfo.element("content").getText();
				}
			}catch (Exception e) {
				log.error("fail to load the mail template: " + templateName, e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					log.error("fail to close the mail template file: " + templateName, e);
				}
			}
		} else {
			log.error("no corresponding mail template file be found: " + templateName);
		}
		
		return titleContents;
	}
}

class PasswordAuthenticator extends Authenticator {

    private String username;
    private String password;

    public PasswordAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
    }
}