package com.kofua.jadx.stringdecrypt;

import java.util.Locale;

public enum DecoderKind {
	METHOD,
	TEMPLATE,
	IDENTITY,
	XOR_UTF8;

	public static DecoderKind parse(String value) {
		String normalized = value.trim()
				.replace('-', '_')
				.toUpperCase(Locale.ROOT);
		return DecoderKind.valueOf(normalized);
	}
}
