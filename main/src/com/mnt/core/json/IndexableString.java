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

package com.mnt.core.json;

/**
 * index marked string for json process.
 * @author Peng Peng
 *
 */
class IndexableString {
	private StringBuilder src;
	private int indexNum;
	
	IndexableString() {
		this(0 , null);
	}
	
	IndexableString(String str) {
		this(0 , str);
	}
	
	IndexableString(int index, String str) {
		this.src = new StringBuilder(str);
		this.indexNum = index;
	}
	
	StringBuilder source() {
		return this.src;
	}
	
	int moveIndex() {
		return moveIndex(1);
	}
	
	int moveIndex(int increment) {
		return indexNum += increment ;
	}
	
	int moveAndTrimStart() {
		return moveAndTrimStart(1);
	}
	
	int moveAndTrimStart(int increment) {
		indexNum += increment;
		trimStart();
		return indexNum;
	}
	
	int findNearestIndex(char... chs) {
		int i = indexNum;
		while(i < src.length()) {
			for(char ch : chs) {
				if(ch == src.charAt(i)) return i - indexNum;
			}
			
			i++;
		}
		
		return -1;
	}
	
	int indexOf(String str) {
		int idx = this.src.indexOf(str, indexNum) - indexNum;
		
		if(idx < 0) {
			idx = -1;
		}
		
		return idx;
	}
	
	boolean startsWith(String str) {
		return str.length() <= length() ? valueBy(str.length()).equals(str) : false; 
	}
	
	int currentIndex() {
		return indexNum;
	}
	
	public int length() {
		return src.length() - indexNum;
	}
	
	public boolean moreChars() {
		return src.length() > indexNum;
	}
	
	char currentChar() {
		return src.charAt(indexNum);
	}
	
	char charAt(int index) {
		return src.charAt(indexNum + index);
	}
	
	public boolean charEquals(int index, char ch) {
		return src.charAt(indexNum + index) == ch;
	}
	
	public String valueBy(int end) {
		return src.substring(indexNum, indexNum + end);
	}
	
	public String valueBy(int space, int end) {
		return src.substring(indexNum + space, indexNum + end);
	}
	
	/**
	 * Trim the IndexableString start characters, refer the method: String.trim()
	 * 
	 * @param source
	 * @return
	 */
	private static int TRIMABLE_CHAR = (' ' + 1);
	IndexableString trimStart(){
		if(currentChar() < TRIMABLE_CHAR) {
			indexNum ++;
			trimStart();
		}
		return this;
	}
	
	void clear() {
		src.setLength(0);
		src = null;
		indexNum = 0;
	}

	@Override
	public String toString() {
		return "IndexableString [sb=" + src + ", indexNum=" + indexNum + "]";
	}
}
