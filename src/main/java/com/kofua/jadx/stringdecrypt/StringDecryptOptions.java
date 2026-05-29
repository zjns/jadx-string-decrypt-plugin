package com.kofua.jadx.stringdecrypt;

import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class StringDecryptOptions extends BasePluginOptionsBuilder {
	public static final String DEFAULT_METHOD_SIGNATURE =
			"Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;";

	private boolean enable;
	private boolean logReplacements;
	private String targetPackage;
	private String methodSignatures;
	private DecoderKind decoder;

	@Override
	public void registerOptions() {
		boolOption(JadxStringDecryptPlugin.PLUGIN_ID + ".enable")
				.description("enable string decrypt replacement")
				.defaultValue(true)
				.setter(v -> enable = v);

		strOption(JadxStringDecryptPlugin.PLUGIN_ID + ".targetPackage")
				.description("optional package prefix used to decide if this plugin should run")
				.defaultValue("")
				.setter(v -> targetPackage = v.trim());

		strOption(JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures")
				.description("comma separated decrypt method signatures, dex form or jadx rawFullId form")
				.defaultValue(DEFAULT_METHOD_SIGNATURE)
				.setter(v -> methodSignatures = v);

		enumOption(JadxStringDecryptPlugin.PLUGIN_ID + ".decoder", DecoderKind.values(), DecoderKind::parse)
				.description("decoder implementation: method, template, identity or xor_utf8")
				.defaultValue(DecoderKind.METHOD)
				.setter(v -> decoder = v);

		boolOption(JadxStringDecryptPlugin.PLUGIN_ID + ".logReplacements")
				.description("log each successful string replacement")
				.defaultValue(true)
				.setter(v -> logReplacements = v);
	}

	public boolean isEnable() {
		return enable;
	}

	public boolean isLogReplacements() {
		return logReplacements;
	}

	public String getTargetPackage() {
		return targetPackage;
	}

	public String getMethodSignatures() {
		return methodSignatures;
	}

	public DecoderKind getDecoder() {
		return decoder;
	}

	public MethodSignatureMatcher getMatcher() {
		return MethodSignatureMatcher.fromOption(methodSignatures);
	}
}
