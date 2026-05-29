package com.kofua.jadx.stringdecrypt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MethodSignatureMatcherTest {

	@Test
	void normalizesDexMethodSignatureToJadxRawFullId() {
		MethodSignatureMatcher matcher = MethodSignatureMatcher.fromOption(
				"Lcom/rx/patch/IiiI1i;->IiiI11i([B[B)Ljava/lang/String;");

		assertThat(matcher.matches("com.rx.patch.IiiI1i.IiiI11i([B[B)Ljava/lang/String;")).isTrue();
		assertThat(matcher.matches("Lcom/rx/patch/IiiI1i;->IiiI11i([B[B)Ljava/lang/String;")).isTrue();
	}

	@Test
	void supportsCommaSeparatedSignatures() {
		MethodSignatureMatcher matcher = MethodSignatureMatcher.fromOption(
				"Lcom/a/A;->d(Ljava/lang/String;)Ljava/lang/String;, com.b.B.x(I)Ljava/lang/String;");

		assertThat(matcher.matches("com.a.A.d(Ljava/lang/String;)Ljava/lang/String;")).isTrue();
		assertThat(matcher.matches("com.b.B.x(I)Ljava/lang/String;")).isTrue();
		assertThat(matcher.matches("com.b.B.y(I)Ljava/lang/String;")).isFalse();
	}
}
