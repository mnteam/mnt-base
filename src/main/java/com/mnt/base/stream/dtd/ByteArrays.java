package com.mnt.base.stream.dtd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteArrays {

	private List<ByteArray> byteArrays;
	private int totalLength;
	
	public ByteArrays() {
		byteArrays = new ArrayList<ByteArray>();
		totalLength = 0;
	}
	
	public ByteArrays(byte... bytes) {
		this(bytes, 0, bytes.length);
	}
	
	public ByteArrays(byte[] bytes, int position, int length) {
		byteArrays = new ArrayList<ByteArray>();
		add(bytes, position, length);
	}
	
	public byte[] toByteArray() {
		if(byteArrays.size() == 1) {
			return byteArrays.get(0).toByteArray();
		} else {
			byte[] ba = new byte[totalLength];
			int position = 0;
			for(ByteArray byteArray : byteArrays) {
				System.arraycopy(byteArray.bytes, byteArray.position, ba, position, byteArray.length);
				position += byteArray.length;
			}
			
			return ba;
		}
	}
	
	public void add(byte... bytes) {
		add(bytes, 0, bytes.length);
	}
	
	public void add(byte[] bytes, int position, int length) {
		byteArrays.add(new ByteArray(bytes, position, length));
	}
	
	public void add(ByteArray byteArray) {
		byteArrays.add(byteArray);
	}
	
	public void insert(int index, byte... bytes) {
		insert(index, bytes, 0, bytes.length);
	}
	
	public void insert(int index, byte[] bytes, int position, int length) {
		byteArrays.add(index, new ByteArray(bytes, position, length));
	}
	
	public void insert(int index, ByteArray byteArray) {
		byteArrays.add(index, byteArray);
	}
	
	public List<ByteArray> getByteArrays() {
		return byteArrays;
	}

	public void setByteArrays(List<ByteArray> byteArrays) {
		this.byteArrays = byteArrays;
	}

	public int getTotalLength() {
		return totalLength;
	}

	public void setTotalLength(int totalLength) {
		this.totalLength = totalLength;
	}

	public byte indexOf(int position) {
		if(position < 0 || position >= totalLength) {
			throw new IndexOutOfBoundsException("position " + position + " exceed the array range: " + totalLength);
		}
		
		for(ByteArray ba : byteArrays) {
			if(position < ba.length) {
				return ba.indexOf(position);
			} else {
				position -= ba.length;
			}
		}
		
		throw new IndexOutOfBoundsException("position " + position + " exceed the array range: " + totalLength);
	}

	public class ByteArray {
		private byte[] bytes;
		
		private int position;
		
		private int length;
		
		public ByteArray() {
			
		}

		public ByteArray(byte... bytes) {
			this(bytes, 0, bytes.length);
		}
		
		public ByteArray(byte[] bytes, int position, int length) {
			super();
			this.bytes = bytes;
			this.position = position;
			this.length = length;
			ByteArrays.this.totalLength += length;
		}

		public byte[] toByteArray() {
			if(position == 0 && length == bytes.length) {
				return bytes;
			} else {
				return Arrays.copyOfRange(bytes, position, position + length);
			}
		}
		
		public void update(byte[] bytes, int position, int length) {
			this.bytes = bytes;
			this.position = position;
			setLength(length);
		}
		
		public byte indexOf(int position) {
			if(position < 0 || position >= length) {
				throw new IndexOutOfBoundsException("position " + position + " exceed the array range: " + this.position);
			}
			
			return bytes[this.position + position];
		}

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

		public int getPosition() {
			return position;
		}

		public void setPosition(int position) {
			this.position = position;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			ByteArrays.this.totalLength -= this.length;
			this.length = length;
			ByteArrays.this.totalLength += length;
		}

		@Override
		public String toString() {
			return "ByteArray [bytes=" + Arrays.toString(bytes) + ", position="
					+ position + ", length=" + length + "]";
		}
	}

	@Override
	public String toString() {
		return "ByteArrays [byteArrays=" + byteArrays + ", totalLength="
				+ totalLength + "]";
	}
}


