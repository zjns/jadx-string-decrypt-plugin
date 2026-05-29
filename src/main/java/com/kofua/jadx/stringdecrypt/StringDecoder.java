package com.kofua.jadx.stringdecrypt;

import java.nio.charset.StandardCharsets;

public interface StringDecoder {

	String decode(DecodeRequest request);

	static StringDecoder forKind(DecoderKind kind) {
		switch (kind) {
			case IDENTITY:
				return request -> request.getStringArg();

			case XOR_UTF8:
				return StringDecoder::decodeXorUtf8;

			case METHOD:
				return null;

			case TEMPLATE:
			default:
				return StringDecoder::decodeTemplate;
		}
	}

	private static String decodeTemplate(DecodeRequest request) {
		// Replace this method with the target app's real decrypt algorithm.
		return null;
	}

	private static String decodeXorUtf8(DecodeRequest request) {
		byte[][] args = request.getByteArrayArgs();
		if (args.length < 2 || args[0] == null || args[1] == null || args[1].length == 0) {
			return null;
		}
		byte[] cipher = args[0];
		byte[] key = args[1];
		byte[] out = new byte[cipher.length];
		for (int i = 0; i < cipher.length; i++) {
			out[i] = (byte) (cipher[i] ^ key[i % key.length]);
		}
		return new String(out, StandardCharsets.UTF_8);
	}
}
