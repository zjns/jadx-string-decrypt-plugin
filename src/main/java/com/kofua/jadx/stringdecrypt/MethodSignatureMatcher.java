package com.kofua.jadx.stringdecrypt;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MethodSignatureMatcher {
	private final Set<String> signatures;

	private MethodSignatureMatcher(Set<String> signatures) {
		this.signatures = signatures;
	}

	public static MethodSignatureMatcher fromOption(String optionValue) {
		if (optionValue == null || optionValue.trim().isEmpty()) {
			return new MethodSignatureMatcher(Collections.emptySet());
		}
		Set<String> signatures = new LinkedHashSet<>();
		for (String value : optionValue.split(",")) {
			String signature = value.trim();
			if (!signature.isEmpty()) {
				signatures.add(signature);
				signatures.add(normalizeDexSignature(signature));
			}
		}
		return new MethodSignatureMatcher(signatures);
	}

	public boolean matches(String signature) {
		return signatures.contains(signature) || signatures.contains(normalizeDexSignature(signature));
	}

	private static String normalizeDexSignature(String signature) {
		String value = signature.trim();
		int arrowPos = value.indexOf("->");
		if (value.startsWith("L") && arrowPos > 1) {
			String cls = value.substring(1, arrowPos);
			if (cls.endsWith(";")) {
				cls = cls.substring(0, cls.length() - 1);
			}
			String method = value.substring(arrowPos + 2);
			return cls.replace('/', '.') + "." + method;
		}
		return value.replace('/', '.');
	}
}
