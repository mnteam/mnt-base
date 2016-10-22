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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

public class NetworkUtil {

	public static String getHostName() {
		String hostName = null;

		try {
			InetAddress a = InetAddress.getLocalHost();
			hostName = a.getHostName();
		} catch (UnknownHostException e) {
			// ignore
		}

		if (hostName == null) {
			boolean bFindIP = false;
			Enumeration<NetworkInterface> netInterfaces = null;
			try {
				netInterfaces = (Enumeration<NetworkInterface>) NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e) {
				// ignore
			}
			
			if(netInterfaces != null) {
				while (netInterfaces.hasMoreElements()) {
					if (bFindIP) {
						break;
					}

					NetworkInterface ni = (NetworkInterface) netInterfaces.nextElement();
					Enumeration<InetAddress> ips = ni.getInetAddresses();
					while (ips.hasMoreElements()) {
						InetAddress ip = (InetAddress) ips.nextElement();
						if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
								&& ip.getHostAddress().indexOf(":") == -1) {
							bFindIP = true;
							
							hostName = ip.getHostName();
							break;
						}
					}
				}
			}
		}

		if (hostName == null) {
			hostName = "unknown";
		}

		return hostName;
	}
}
