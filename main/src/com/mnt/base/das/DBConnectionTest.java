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

package com.mnt.base.das;

import java.sql.Connection;

/**
 * test class for db connection retrieve
 * @author Peng Peng
 * @date 2013-2-17
 *
 *
 */
public class DBConnectionTest {

	public static void main(String[] args) throws Exception {
		
		// pool test
		int i = 0;
		while(i++ < 100){
			final Connection conn = DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).getConnection(); 
			System.out.println("new connection[" + i + "] : " + conn);
			
			new Thread(){
				public void run(){
					
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						
					}
					
					try {
						System.out.println("start to close the conneciton: " + conn);
						conn.close();
						DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).close(conn);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
		
		/*
		 * availablibity test
		 * 
		 * Connection conn = DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).getConnection(); 
		
		System.out.println(conn);
		
		DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).close(conn);
		
		Thread.sleep(5000);
		
		conn = DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).getConnection(); 
		
		System.out.println(conn);
		
		DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).close(conn);
		
		Thread.sleep(11000);
		
		conn = DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).getConnection(); 
		
		System.out.println(conn);
		
		DBFactory.getDBFactory(DBFactory.FactoryType.RELATION_DB).close(conn);*/
		
		
	}
}
