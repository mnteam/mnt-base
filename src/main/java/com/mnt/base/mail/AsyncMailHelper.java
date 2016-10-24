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

package com.mnt.base.mail;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.util.BaseConfiguration;
import com.mnt.base.util.CommonUtil;

/**
 * support async send email
 * @author Peng Peng
 *
 */
public class AsyncMailHelper implements Runnable {
	
	private static final Log log = LogFactory.getLog(AsyncMailHelper.class);
	
	private static class MailHolder {
		String mailType;
		String mailTos;
		Map<String, Object> infoMap;
		
		public MailHolder(String mailType, String mailTos,
				Map<String, Object> infoMap) {
			super();
			this.mailType = mailType;
			this.mailTos = mailTos;
			this.infoMap = infoMap;
		}

		@Override
		public String toString() {
			return "MailHolder [mailType=" + mailType + ", mailTos=" + mailTos
					+ ", infoMap=" + infoMap + "]";
		}
	}
	
	private static BlockingQueue<MailHolder> mailQueue; 
	
	private AsyncMailHelper() {
		mailQueue = new LinkedBlockingQueue<MailHolder>(); 
	}
	
	static {
		new Thread(new AsyncMailHelper()).start();
	}
	
	
	
	public static void sendMail(String mailType, Map<String, Object> infoMap) {
        String toArrs =BaseConfiguration.getProperty("mail_notification_receivers").trim();
        sendMail(mailType, toArrs, infoMap);
	}
	
	public static void sendMail(String mailType, String mailTos, Map<String, Object> infoMap) {
		
		mailQueue.offer(new MailHolder(mailType, mailTos, infoMap));
	}

	@Override
	public void run() {
		MailHolder mh = null;
		
		while(true) {
			try {
				mh = mailQueue.take();
			} catch (InterruptedException e) {
				log.error("fail to retrieve mailholder, the server would be shutting down...", e);
				break;
			}
			
			if(mh != null) {
				
				try {
					MailHelper.sendMail(mh.mailType, mh.mailTos, mh.infoMap);
				} catch(Exception e) {
					log.error("fail to send the email: " + mh, e);
				}
				
				CommonUtil.deepClear(mh.infoMap);
			}
		}
	}
}