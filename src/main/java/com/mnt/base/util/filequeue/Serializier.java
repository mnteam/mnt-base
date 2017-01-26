package com.mnt.base.util.filequeue;

public interface Serializier<T> {
	byte[] serialize(T t);
	T deserialize(byte[] bs);
}
