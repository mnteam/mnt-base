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

package com.mnt.base.stream.comm;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mnt.base.util.TimeUtil;

/**
 * <pre>
 * Net Traffic Tool for printout the network datagram.
 * 
 * It use a file existing or not to switch on|off the traffic monitor, the flag file name is "ntflag" with no subfix.
 * 
 * Specify the path by key NT_FLAG_FILE in system properties or env, otherwise the flag file should be created under the application running path.
 * 
 * 
 * </pre>
 * 
 * @author Peng Peng
 *
 */
public class NetTraffic {
	
	private static final Log log = LogFactory.getLog(NetTraffic.class);
	
	private static boolean logFlag = false;
	
	private static String ntFlagPath;
	
	public static void log(Object... data) {
		
		if(logFlag && data != null) {
			
			StringBuilder sb = new StringBuilder("[=NT=]");
			for(Object o : data) {
				sb.append(o);
			}
			
			log.info(sb.toString());
			sb.setLength(0);
		}
	}
	
	static {
		ntFlagPath = System.getProperty("NT_FLAG_FILE");
		if(ntFlagPath == null) {
			ntFlagPath = System.getenv("NT_FLAG_FILE"); 
		}
		
		if(ntFlagPath == null) {
			ntFlagPath = "";
		}
		
		if (!ntFlagPath.endsWith("/") && !ntFlagPath.endsWith("\\")) {
			ntFlagPath += File.separator;
		}
		
		if(ntFlagPath == null) {
			ntFlagPath = ""; // default ntFlagFile path
		}
		
		ntFlagPath += "ntflag";
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				File f = new File(ntFlagPath);
				logFlag = f.exists();
			}
		}, TimeUtil.MINUTES_1, TimeUtil.MINUTES_1);
	}
}
