package com.kofua.jadx.stringdecrypt;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

public class JadxStringDecryptPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "string-decrypt";

	private final StringDecryptOptions options = new StringDecryptOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("String decrypt")
				.description("Replace configured decrypt method calls with decoded string constants")
				.homepage("https://github.com/skylot/jadx/discussions/2742")
				.requiredJadxVersion("1.5.3, r2504")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		if (options.isEnable()) {
			String targetPackage = options.getTargetPackage();
			StringDecryptState state = new StringDecryptState(targetPackage == null || targetPackage.isEmpty());
			context.addPass(new StringDecryptAfterLoadPass(options, state));
			context.addPass(new StringDecryptReplacePass(options, state));
		}
	}
}
