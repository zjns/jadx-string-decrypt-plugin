package com.kofua.jadx.stringdecrypt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringDecoderTest {

	@Test
	void identityDecoderReturnsSingleStringArgument() {
		StringDecoder decoder = StringDecoder.forKind(DecoderKind.IDENTITY);

		assertThat(decoder.decode(new DecodeRequest("cipher", new byte[0][]))).isEqualTo("cipher");
	}

	@Test
	void xorUtf8DecoderUsesSecondByteArrayAsRepeatingKey() {
		StringDecoder decoder = StringDecoder.forKind(DecoderKind.XOR_UTF8);

		byte[] cipher = new byte[] { 0x23, 0x0c, 0x27, 0x05, 0x24 };
		byte[] key = new byte[] { 0x4b, 0x69 };

		assertThat(decoder.decode(new DecodeRequest(null, new byte[][] { cipher, key }))).isEqualTo("hello");
	}

	@Test
	void templateDecoderReturnsNullUntilAlgorithmIsImplemented() {
		StringDecoder decoder = StringDecoder.forKind(DecoderKind.TEMPLATE);

		assertThat(decoder.decode(new DecodeRequest("cipher", new byte[0][]))).isNull();
	}

	@Test
	void methodDecoderIsResolvedByReplacePass() {
		assertThat(StringDecoder.forKind(DecoderKind.METHOD)).isNull();
	}
}
