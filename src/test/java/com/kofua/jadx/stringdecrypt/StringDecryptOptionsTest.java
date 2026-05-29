package com.kofua.jadx.stringdecrypt;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringDecryptOptionsTest {

	@Test
	void defaultsToExampleStringSignature() {
		StringDecryptOptions options = new StringDecryptOptions();
		options.setOptions(Collections.emptyMap());

		assertThat(options.isEnable()).isTrue();
		assertThat(options.getMethodSignatures())
				.isEqualTo("Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;");
		assertThat(options.getDecoder()).isEqualTo(DecoderKind.METHOD);
	}

	@Test
	void acceptsCustomMethodSignatures() {
		StringDecryptOptions options = new StringDecryptOptions();
		options.setOptions(Map.of(
				JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
				"Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;"));

		assertThat(options.getMatcher().matches("com.example.S.dec(Ljava/lang/String;)Ljava/lang/String;")).isTrue();
	}
}
