package com.kofua.jadx.stringdecrypt;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxAfterLoadPass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringDecryptAfterLoadPass implements JadxAfterLoadPass {
	private static final Logger LOG = LoggerFactory.getLogger(StringDecryptAfterLoadPass.class);

	private final StringDecryptOptions options;
	private final StringDecryptState state;

	public StringDecryptAfterLoadPass(StringDecryptOptions options, StringDecryptState state) {
		this.options = options;
		this.state = state;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new SimpleJadxPassInfo("StringDecryptAfterLoad", "Enable string decrypt pass after class loading");
	}

	@Override
	public void init(JadxDecompiler decompiler) {
		String targetPackage = options.getTargetPackage();
		if (targetPackage == null || targetPackage.isEmpty() || hasClassInPackage(decompiler, targetPackage)) {
			state.setActive(true);
			LOG.info("Enabled jadx string decrypt replacement for signatures: {}", options.getMethodSignatures());
		} else {
			state.setActive(false);
			LOG.info("Skip jadx string decrypt replacement, no class matched package prefix: {}", targetPackage);
		}
	}

	private static boolean hasClassInPackage(JadxDecompiler decompiler, String packagePrefix) {
		for (JavaClass cls : decompiler.getClassesWithInners()) {
			if (cls.getFullName().startsWith(packagePrefix)) {
				return true;
			}
		}
		return false;
	}
}
