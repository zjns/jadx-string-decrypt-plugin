package com.kofua.jadx.stringdecrypt;

public final class DecodeRequest {
	private final String stringArg;
	private final byte[][] byteArrayArgs;

	public DecodeRequest(String stringArg, byte[][] byteArrayArgs) {
		this.stringArg = stringArg;
		this.byteArrayArgs = byteArrayArgs == null ? new byte[0][] : byteArrayArgs;
	}

	public String getStringArg() {
		return stringArg;
	}

	public byte[][] getByteArrayArgs() {
		return byteArrayArgs;
	}
}
