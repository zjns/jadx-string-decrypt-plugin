package com.kofua.jadx.stringdecrypt;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class JadxStringDecryptPluginIntegrationTest {

	@Test
	void replacesConfiguredDecryptCallWithDecodedStringConstant() throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleDir("identity-decrypt/com/example/App.smali"));
		args.setPluginOptions(Map.of(
				JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
				"Lcom/example/App;->dec(Ljava/lang/String;)Ljava/lang/String;"));

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.registerPlugin(new JadxStringDecryptPlugin());
			jadx.load();

			JavaClass appClass = jadx.searchJavaClassByOrigFullName("com.example.App");
			assertThat(appClass).isNotNull();

			String code = appClass.getCode();
			assertThat(code).contains("return \"cipher\";");
			assertThat(code).doesNotContain("dec(\"cipher\")");
		}
	}

	@Test
	void replacesConfiguredByteArrayDecryptCallWithDecodedStringConstant() throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleDir("byte-array-decrypt/com/example/BytesApp.smali"));
		args.setPluginOptions(Map.of(
				JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
				"Lcom/example/BytesApp;->dec([B[B)Ljava/lang/String;"));

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.registerPlugin(new JadxStringDecryptPlugin());
			jadx.load();

			JavaClass appClass = jadx.searchJavaClassByOrigFullName("com.example.BytesApp");
			assertThat(appClass).isNotNull();

			String code = appClass.getCode();
			assertThat(code).contains("return \"hello\";");
			assertThat(code).doesNotContain("dec(new byte");
			assertThat(code).contains("public static String run() {\n        return \"hello\";\n    }");
		}
	}

	@Test
	void replacesConfiguredByteArrayDecryptCallUsingCharsetConstructor() throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleDir("byte-array-charset-decrypt/com/example/CharsetBytesApp.smali"));
		args.setPluginOptions(Map.of(
				JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
				"Lcom/example/CharsetBytesApp;->dec([B[B)Ljava/lang/String;"));

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.registerPlugin(new JadxStringDecryptPlugin());
			jadx.load();

			JavaClass appClass = jadx.searchJavaClassByOrigFullName("com.example.CharsetBytesApp");
			assertThat(appClass).isNotNull();

			String code = appClass.getCode();
			assertThat(code).contains("return \"hello\";");
			assertThat(code).doesNotContain("dec(new byte");
			assertThat(code).contains("public static String run() {\n        return \"hello\";\n    }");
		}
	}

	@Test
	void replacesConfiguredAputInitializedByteArrayDecryptCall() throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleDir("byte-array-aput-decrypt/com/example/AputBytesApp.smali"));
		args.setPluginOptions(Map.of(
				JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
				"Lcom/example/AputBytesApp;->dec([B[B)Ljava/lang/String;"));

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.registerPlugin(new JadxStringDecryptPlugin());
			jadx.load();

			JavaClass appClass = jadx.searchJavaClassByOrigFullName("com.example.AputBytesApp");
			assertThat(appClass).isNotNull();

			String code = appClass.getCode();
			assertThat(code).contains("return \"get\";");
			assertThat(code).doesNotContain("dec(new byte");
			assertThat(code).contains("public static String run() {\n        return \"get\";\n    }");
		}
	}

	@Test
	void replacesLoopCarriedIndexByteArrayDecryptCall() throws Exception {
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(getSampleDir("byte-array-loop-carried-decrypt/com/example/LoopCarriedBytesApp.smali"));
		args.setPluginOptions(Map.of(
				JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
				"Lcom/example/LoopCarriedBytesApp;->dec([B[B)Ljava/lang/String;"));

		try (JadxDecompiler jadx = new JadxDecompiler(args)) {
			jadx.registerPlugin(new JadxStringDecryptPlugin());
			jadx.load();

			JavaClass appClass = jadx.searchJavaClassByOrigFullName("com.example.LoopCarriedBytesApp");
			assertThat(appClass).isNotNull();

			String code = appClass.getCode();
			assertThat(code).contains("\"get\"");
			assertThat(code).doesNotContain("str = dec(");
		}
	}

	@Test
	void evaluatesNestedWrappedByteExpressionUsedInArrayPut() throws Exception {
		Class<?> stateClass = Class.forName(MethodBodyEvaluator.class.getName() + "$EvalState");
		Constructor<?> stateConstructor = stateClass.getDeclaredConstructor();
		stateConstructor.setAccessible(true);
		Object state = stateConstructor.newInstance();

		Method set = stateClass.getDeclaredMethod("set", RegisterArg.class, Object.class);
		set.setAccessible(true);
		set.invoke(state, InsnArg.reg(8, ArgType.array(ArgType.BYTE)), new byte[] {1, 2});
		set.invoke(state, InsnArg.reg(9, ArgType.array(ArgType.BYTE)), new byte[] {3, 4});
		set.invoke(state, InsnArg.reg(4, ArgType.INT), 1);
		set.invoke(state, InsnArg.reg(5, ArgType.INT), 0);

		InsnNode cipherGet = new InsnNode(InsnType.AGET, 2);
		cipherGet.setResult(InsnArg.reg(10, ArgType.BYTE));
		cipherGet.addArg(InsnArg.reg(8, ArgType.array(ArgType.BYTE)));
		cipherGet.addArg(InsnArg.reg(4, ArgType.INT));

		InsnNode keyGet = new InsnNode(InsnType.AGET, 2);
		keyGet.setResult(InsnArg.reg(11, ArgType.BYTE));
		keyGet.addArg(InsnArg.reg(9, ArgType.array(ArgType.BYTE)));
		keyGet.addArg(InsnArg.reg(5, ArgType.INT));

		ArithNode xor = new ArithNode(
				ArithOp.XOR,
				InsnArg.reg(12, ArgType.INT),
				InsnArg.wrapArg(cipherGet),
				InsnArg.wrapArg(keyGet));
		IndexInsnNode cast = new IndexInsnNode(InsnType.CAST, ArgType.BYTE, 1);
		cast.addArg(InsnArg.wrapArg(xor));

		Method valueOf = MethodBodyEvaluator.class.getDeclaredMethod("valueOf", stateClass, InsnArg.class);
		valueOf.setAccessible(true);

		assertThatCode(() -> assertThat(valueOf.invoke(null, state, InsnArg.wrapArg(cast))).isEqualTo(1))
				.doesNotThrowAnyException();
	}

	@Test
	void skipsUnresolvedMethodArgumentsWithoutWarning() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(StringDecryptReplacePass.class);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			JadxArgs args = new JadxArgs();
			args.getInputFiles().add(getSampleDir("unresolved-call/com/example/UnresolvedApp.smali"));
			args.setPluginOptions(Map.of(
					JadxStringDecryptPlugin.PLUGIN_ID + ".methodSignatures",
					"Lcom/example/UnresolvedApp;->dec([B[B)Ljava/lang/String;"));

			try (JadxDecompiler jadx = new JadxDecompiler(args)) {
				jadx.registerPlugin(new JadxStringDecryptPlugin());
				jadx.load();

				JavaClass appClass = jadx.searchJavaClassByOrigFullName("com.example.UnresolvedApp");
				assertThat(appClass).isNotNull();
				assertThat(appClass.getCode()).contains("dec(");
			}

			assertThat(appender.list)
					.noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN));
		} finally {
			logger.detachAppender(appender);
		}
	}

	private File getSampleDir(String fileName) throws URISyntaxException {
		URL file = getClass().getClassLoader().getResource("samples/" + fileName);
		assertThat(file).isNotNull();
		return new File(file.toURI()).getParentFile().getParentFile().getParentFile();
	}
}
