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

package com.mnt.base.util;

import java.util.Calendar;

/**
 * provide time related methods. 
 * 
 * @author Peng Peng
 * #date 2013-2-17
 *
 *
 */
public class TimeUtil {
	
	public static final long SEC_1				= 1000		* 1;
	public static final long SEC_30 			= SEC_1 	* 30;
	public static final long MINUTES_1			= SEC_1  	* 60;
	public static final long MINUTES_15 		= MINUTES_1 * 15;
	public static final long MINUTES_30			= MINUTES_1 * 30;
	public static final long MINUTES_45			= MINUTES_1 * 45;
	public static final long HOUR_1 			= MINUTES_1 * 60;
	public static final long DAY_1	 			= HOUR_1 	* 24;
	public static final long WEEK_1 			= DAY_1 	* 7;
	
	private static long dateTodayStartTs 		= 0;
	private static long dateTodayEndTs 			= 0;
	
	/**
	 * Get current time count by minutes
	 * 
	 * @return
	 */
	public static long currentTimeInMinutes(){
		return (System.currentTimeMillis() / MINUTES_1);
	}
	
	/**
	 * convert the timestamp by minutes
	 * 
	 * @param timeMillis
	 * @return
	 */
	public static long timeMillisToTimeMintues(long timeMillis){
		return timeMillis / MINUTES_1;
	}
	
	/**
	 * Get today start timestamp.
	 * 
	 * @return
	 */
	public static long getDateTodayStartTs() {
		if(System.currentTimeMillis() - DAY_1 > dateTodayStartTs) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.AM_PM, Calendar.AM);
			
			dateTodayStartTs = cal.getTimeInMillis();
		}
		
		return dateTodayStartTs;
	}
	
	/**
	 * Get today end timestamp.
	 * 
	 * @return
	 */
	public static long getDateTodayEndTs() {
		if(System.currentTimeMillis() > dateTodayEndTs) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.AM_PM, Calendar.AM);
			
			dateTodayEndTs = cal.getTimeInMillis() + DAY_1;
		}
		
		return dateTodayEndTs;
	}
	
	/**
	 * Get the current time week index.
	 * 
	 * @return
	 */
	public static int getWeekIndex() {
		return getWeekIndex(System.currentTimeMillis());
	}
	
	/**
	 * Get the specified timestamp week index.
	 * 
	 * @param timestamp
	 * @return
	 */
	public static int getWeekIndex(long timestamp) {
		return getWeekIndex(timestamp, 0);
	}
	
	/**
	 * Get the week index for specified delay timestamp.
	 * 
	 * @param dalayTs
	 * @return
	 */
	public static int getWeekIndex(int dalayTs) {
		return getWeekIndex(System.currentTimeMillis(), dalayTs);
	}
	
	/**
	 * Get the week index for specified timestamp with delay timestamp.
	 * 
	 * weekIndex means the week count with the year.
	 * e.g.: 201301 means the first week in 2013
	 * 
	 * @param timestamp
	 * @param dalayTs
	 * @return
	 */
	public static int getWeekIndex(long timestamp, int dalayTs) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timestamp + dalayTs);
		int weekIndex = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR);
		
		return weekIndex;
	}
}
