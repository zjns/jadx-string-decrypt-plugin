package com.kofua.jadx.stringdecrypt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo;
import jadx.api.plugins.pass.types.JadxDecompilePass;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayInsn;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.InsnRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringDecryptReplacePass implements JadxDecompilePass {
	private static final Logger LOG = LoggerFactory.getLogger(StringDecryptReplacePass.class);
	private static final Object UNRESOLVED = new Object();

	private final StringDecryptOptions options;
	private final StringDecryptState state;
	private MethodSignatureMatcher matcher;
	private StringDecoder decoder;
	private MethodBodyEvaluator methodBodyEvaluator;
	private RootNode root;

	public StringDecryptReplacePass(StringDecryptOptions options, StringDecryptState state) {
		this.options = options;
		this.state = state;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new OrderedJadxPassInfo(
				"StringDecryptReplace",
				"Replace configured string decrypt calls with constants")
				.after("SSATransform")
				.before("TypeInferenceVisitor");
	}

	@Override
	public void init(RootNode root) {
		this.root = root;
		this.matcher = options.getMatcher();
		this.decoder = StringDecoder.forKind(options.getDecoder());
		this.methodBodyEvaluator = new MethodBodyEvaluator();
	}

	@Override
	public boolean visit(ClassNode cls) {
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
		if (!state.isActive()) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		if (blocks == null || blocks.isEmpty()) {
			return;
		}
		for (BlockNode block : blocks) {
			List<InsnNode> instructions = block.getInstructions();
			for (int i = 0; i < instructions.size(); i++) {
				InsnNode insn = instructions.get(i);
				if (insn instanceof BaseInvokeNode) {
					InsnNode replacement = tryReplace(mth, (BaseInvokeNode) insn);
					if (replacement != null) {
						instructions.set(i, replacement);
					}
				}
			}
		}
		InsnRemover.removeAllMarked(mth);
	}

	private InsnNode tryReplace(MethodNode mth, BaseInvokeNode invoke) {
		if (!matcher.matches(invoke.getCallMth().getRawFullId()) && !matcher.matches(invoke.getCallMth().getFullId())) {
			return null;
		}
		LOG.debug("Matched decrypt call {} in {}", invoke.getCallMth().getRawFullId(), mth);

		String decoded;
		Set<InsnNode> cleanup = new LinkedHashSet<>();
		try {
			if (options.getDecoder() == DecoderKind.METHOD) {
				MethodNode decryptMethod = root.resolveMethod(invoke.getCallMth());
				if (decryptMethod == null) {
					return null;
				}
				Object[] methodArgs = buildMethodArgs(mth, invoke, cleanup);
					if (methodArgs == null) {
						LOG.debug("Skip string decrypt call with unresolved arguments {} in {}", invoke.getCallMth().getRawFullId(), mth);
						return null;
					}
					try {
						decoded = methodBodyEvaluator.evaluateToString(decryptMethod, methodArgs);
					} catch (Exception e) {
						LOG.debug("Method evaluator failed for call {}, try byte-array fallback", invoke.getCallMth().getRawFullId(), e);
						decoded = null;
					}
					if (decoded == null) {
						decoded = decodeXorUtf8Fallback(methodArgs);
					}
				} else {
					DecodeRequest request = buildDecodeRequest(mth, invoke, cleanup);
					decoded = decoder.decode(request);
			}
		} catch (Exception e) {
			LOG.warn("String decrypt failed for call {} in {}", invoke.getCallMth().getRawFullId(), mth, e);
			return null;
		}
		if (decoded == null) {
			if (options.getDecoder() == DecoderKind.METHOD) {
				LOG.debug("Method evaluator returned null for call {} in {}", invoke.getCallMth().getRawFullId(), mth);
			}
			return null;
		}

		ConstStringNode constString = new ConstStringNode(decoded);
		constString.copyAttributesFrom(invoke);
		constString.copyLines(invoke);
		constString.setOffset(invoke.getOffset());
		constString.setResult(invoke.getResult());
		for (InsnArg arg : invoke.getArguments()) {
			InsnRemover.unbindArgUsage(mth, arg);
		}
		markConstantArgInstructionsForRemoval(mth, invoke, cleanup);
		if (options.isLogReplacements()) {
			LOG.info("Decoded string in {}: {}", mth, decoded);
		}
		return constString;
	}

	private static String decodeXorUtf8Fallback(Object[] methodArgs) {
		if (methodArgs.length < 2 || !(methodArgs[0] instanceof byte[]) || !(methodArgs[1] instanceof byte[])) {
			return null;
		}
		return StringDecoder.forKind(DecoderKind.XOR_UTF8)
				.decode(new DecodeRequest(null, new byte[][] {(byte[]) methodArgs[0], (byte[]) methodArgs[1]}));
	}

	private static Object[] buildMethodArgs(MethodNode mth, BaseInvokeNode invoke, Set<InsnNode> cleanup) {
		int firstArgOffset = invoke.getFirstArgOffset();
		Object[] args = new Object[invoke.getArgsCount() - firstArgOffset];
		for (int i = firstArgOffset; i < invoke.getArgsCount(); i++) {
			Object value = extractValue(mth, invoke, invoke.getArg(i), cleanup);
			if (value == UNRESOLVED) {
				return null;
			}
			args[i - firstArgOffset] = value;
		}
		return args;
	}

	private static Object extractValue(MethodNode mth, BaseInvokeNode invoke, InsnArg arg, Set<InsnNode> cleanup) {
		String str = extractString(arg);
		if (str != null) {
			return str;
		}
		byte[] bytes = extractByteArray(mth, invoke, arg, cleanup);
		if (bytes != null) {
			return bytes;
		}
		if (arg instanceof LiteralArg) {
			return (int) ((LiteralArg) arg).getLiteral();
		}
		return UNRESOLVED;
	}

	private static DecodeRequest buildDecodeRequest(MethodNode mth, BaseInvokeNode invoke, Set<InsnNode> cleanup) {
		String stringArg = null;
		List<byte[]> byteArrayArgs = new ArrayList<>();
		int firstArgOffset = invoke.getFirstArgOffset();
		for (int i = firstArgOffset; i < invoke.getArgsCount(); i++) {
			InsnArg arg = invoke.getArg(i);
			if (stringArg == null) {
				stringArg = extractString(arg);
			}
			byte[] bytes = extractByteArray(mth, invoke, arg, cleanup);
			if (bytes != null) {
				byteArrayArgs.add(bytes);
			}
		}
		return new DecodeRequest(stringArg, byteArrayArgs.toArray(new byte[0][]));
	}

	private static String extractString(InsnArg arg) {
		InsnNode insn = unwrapArgInsn(arg);
		if (insn instanceof ConstStringNode) {
			return ((ConstStringNode) insn).getString();
		}
		return null;
	}

	private static byte[] extractByteArray(MethodNode mth, BaseInvokeNode invoke, InsnArg arg, Set<InsnNode> cleanup) {
		InsnNode insn = unwrapArgInsn(arg);
		if (insn == null) {
			return null;
		}
		if (insn.getType() == InsnType.ONE_ARG && insn.getArgsCount() == 1) {
			return extractByteArray(mth, invoke, insn.getArg(0), cleanup);
		}
		if (insn instanceof FilledNewArrayNode) {
			byte[] data = extractFilledNewArray((FilledNewArrayNode) insn);
			if (data != null && arg instanceof RegisterArg) {
				cleanup.add(insn);
			}
			if (data != null) {
				return data;
			}
			return extractByteArrayByTrace(mth, invoke, arg, cleanup);
		}
		if (arg instanceof RegisterArg) {
			byte[] fillArrayData = extractFillArrayData(mth, invoke, (RegisterArg) arg, insn, cleanup);
			if (fillArrayData != null) {
				return fillArrayData;
			}
			byte[] aputInitializedArray = extractAputInitializedArray(mth, invoke, (RegisterArg) arg, insn, cleanup);
			if (aputInitializedArray != null) {
				return aputInitializedArray;
			}
			return extractByteArrayByTrace(mth, invoke, arg, cleanup);
		}
		return extractByteArrayByTrace(mth, invoke, arg, cleanup);
	}

	private static byte[] extractFilledNewArray(FilledNewArrayNode insn) {
		int count = insn.getArgsCount();
		byte[] data = new byte[count];
		for (int i = 0; i < count; i++) {
			InsnArg item = insn.getArg(i);
			if (!(item instanceof LiteralArg)) {
				return null;
			}
			data[i] = (byte) ((LiteralArg) item).getLiteral();
		}
		return data;
	}

	private static byte[] extractFillArrayData(
			MethodNode mth,
			BaseInvokeNode invoke,
			RegisterArg arrayArg,
			InsnNode assignInsn,
			Set<InsnNode> cleanup) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn == invoke) {
					return null;
				}
				if (insn instanceof FillArrayInsn && insn.getArgsCount() == 1 && sameSVar(arrayArg, insn.getArg(0))) {
					List<LiteralArg> literals = ((FillArrayInsn) insn).getLiteralArgs(ArgType.BYTE);
					byte[] data = new byte[literals.size()];
					for (int i = 0; i < literals.size(); i++) {
						data[i] = (byte) literals.get(i).getLiteral();
					}
					cleanup.add(assignInsn);
					cleanup.add(insn);
					return data;
				}
			}
		}
		return null;
	}

	private static byte[] extractAputInitializedArray(
			MethodNode mth,
			BaseInvokeNode invoke,
			RegisterArg arrayArg,
			InsnNode assignInsn,
			Set<InsnNode> cleanup) {
		if (!(assignInsn instanceof NewArrayNode)) {
			return null;
		}
		NewArrayNode newArray = (NewArrayNode) assignInsn;
		if (!ArgType.array(ArgType.BYTE).equals(newArray.getArrayType())) {
			return null;
		}
		Integer length = extractInt(newArray.getArg(0));
		if (length == null || length < 0) {
			return null;
		}
		byte[] data = new byte[length];
		boolean wrote = false;
		List<InsnNode> arrayWrites = new ArrayList<>();
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn == invoke) {
					if (wrote) {
						cleanup.add(assignInsn);
						cleanup.addAll(arrayWrites);
					}
					return wrote ? data : null;
				}
				if (insn.getType() == InsnType.APUT && insn.getArgsCount() == 3 && sameSVar(arrayArg, insn.getArg(0))) {
					Integer index = extractInt(insn.getArg(1));
					Integer value = extractInt(insn.getArg(2));
					if (index == null || value == null || index < 0 || index >= data.length) {
						return null;
					}
					data[index] = (byte) value.intValue();
					arrayWrites.add(insn);
					wrote = true;
				}
			}
		}
		return wrote ? data : null;
	}

	private static byte[] extractByteArrayByTrace(MethodNode mth, BaseInvokeNode invoke, InsnArg arg, Set<InsnNode> cleanup) {
		ConstTrace trace = new ConstTrace();
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn == invoke) {
					Object value = trace.valueOf(arg);
					if (value instanceof TrackedByteArray) {
						TrackedByteArray tracked = (TrackedByteArray) value;
						cleanup.addAll(tracked.cleanup);
						return Arrays.copyOf(tracked.data, tracked.data.length);
					}
					if (value instanceof byte[]) {
						return Arrays.copyOf((byte[]) value, ((byte[]) value).length);
					}
					return null;
				}
				trace.execute(insn);
			}
		}
		return null;
	}

	private static void markConstantArgInstructionsForRemoval(MethodNode mth, BaseInvokeNode invoke, Set<InsnNode> cleanup) {
		if (cleanup.isEmpty()) {
			return;
		}
		Set<InsnNode> expanded = new LinkedHashSet<>(cleanup);
		expandCleanupAssignments(expanded);
		if (!isCleanupSafe(expanded, invoke)) {
			LOG.debug("Skip hiding string decrypt argument setup with external uses for {}", invoke.getCallMth().getRawFullId());
			return;
		}
		InsnRemover.unbindInsns(mth, new ArrayList<>(expanded));
	}

	private static void expandCleanupAssignments(Set<InsnNode> cleanup) {
		boolean changed;
		do {
			changed = false;
			List<InsnNode> snapshot = new ArrayList<>(cleanup);
			for (InsnNode insn : snapshot) {
				for (InsnArg arg : insn.getArguments()) {
					InsnNode assignInsn = assignInsnOf(arg);
					if (assignInsn != null && !cleanup.contains(assignInsn) && isResultUsedOnlyBy(assignInsn, cleanup, null)) {
						cleanup.add(assignInsn);
						changed = true;
					}
				}
			}
		} while (changed);
	}

	private static boolean isCleanupSafe(Set<InsnNode> cleanup, BaseInvokeNode invoke) {
		for (InsnNode insn : cleanup) {
			if (!isResultUsedOnlyBy(insn, cleanup, invoke)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isResultUsedOnlyBy(InsnNode assignInsn, Set<InsnNode> cleanup, BaseInvokeNode invoke) {
		RegisterArg result = assignInsn.getResult();
		if (result == null) {
			return true;
		}
		if (result.getSVar() == null) {
			return false;
		}
		SSAVar ssaVar = result.getSVar();
		for (RegisterArg use : ssaVar.getUseList()) {
			InsnNode parentInsn = use.getParentInsn();
			if (parentInsn == invoke) {
				continue;
			}
			if (!cleanup.contains(parentInsn)) {
				return false;
			}
		}
		return true;
	}

	private static InsnNode assignInsnOf(InsnArg arg) {
		if (arg instanceof RegisterArg) {
			return ((RegisterArg) arg).getAssignInsn();
		}
		if (arg instanceof InsnWrapArg) {
			return ((InsnWrapArg) arg).getWrapInsn();
		}
		return null;
	}

	private static final class ConstTrace {
		private final Map<Integer, Object> regs = new HashMap<>();

		void execute(InsnNode insn) {
			switch (insn.getType()) {
				case CONST:
				case CONST_STR:
				case MOVE:
				case ONE_ARG:
				case CAST:
				case ARITH:
				case ARRAY_LENGTH:
				case NEW_ARRAY:
				case FILLED_NEW_ARRAY:
				case AGET:
					setResult(insn, valueOfInsn(insn));
					return;

				case FILL_ARRAY:
					executeFillArray((FillArrayInsn) insn);
					return;

				case APUT:
					executeArrayPut(insn);
					return;

				default:
					if (insn.getResult() != null) {
						regs.remove(insn.getResult().getRegNum());
					}
			}
		}

		Object valueOf(InsnArg arg) {
			if (arg instanceof LiteralArg) {
				return (int) ((LiteralArg) arg).getLiteral();
			}
			if (arg instanceof RegisterArg) {
				Object value = regs.get(((RegisterArg) arg).getRegNum());
				return value != null ? value : UNRESOLVED;
			}
			if (arg instanceof InsnWrapArg) {
				return valueOfInsn(((InsnWrapArg) arg).getWrapInsn());
			}
			return UNRESOLVED;
		}

		private Object valueOfInsn(InsnNode insn) {
			switch (insn.getType()) {
				case CONST:
					return valueOf(insn.getArg(0));

				case CONST_STR:
					return ((ConstStringNode) insn).getString();

				case MOVE:
				case ONE_ARG:
				case CAST:
					return insn.getArgsCount() == 1 ? valueOf(insn.getArg(0)) : UNRESOLVED;

				case ARITH:
					return arithValue((ArithNode) insn);

				case ARRAY_LENGTH:
					Object array = valueOf(insn.getArg(0));
					if (array instanceof TrackedByteArray) {
						return ((TrackedByteArray) array).data.length;
					}
					if (array instanceof byte[]) {
						return ((byte[]) array).length;
					}
					return UNRESOLVED;

				case NEW_ARRAY:
					return newArrayValue((NewArrayNode) insn);

				case FILLED_NEW_ARRAY:
					return filledNewArrayValue((FilledNewArrayNode) insn);

				case AGET:
					return arrayGetValue(insn);

				default:
					return UNRESOLVED;
			}
		}

		private Object arithValue(ArithNode insn) {
			Integer a = asInteger(valueOf(insn.getArg(0)));
			Integer b = asInteger(valueOf(insn.getArg(1)));
			if (a == null || b == null) {
				return UNRESOLVED;
			}
			switch (insn.getOp()) {
				case ADD:
					return a + b;
				case SUB:
					return a - b;
				case MUL:
					return a * b;
				case DIV:
					return b == 0 ? UNRESOLVED : a / b;
				case REM:
					return b == 0 ? UNRESOLVED : a % b;
				case AND:
					return a & b;
				case OR:
					return a | b;
				case XOR:
					return a ^ b;
				case SHL:
					return a << b;
				case SHR:
					return a >> b;
				case USHR:
					return a >>> b;
				default:
					return UNRESOLVED;
			}
		}

		private Object newArrayValue(NewArrayNode insn) {
			Integer length = asInteger(valueOf(insn.getArg(0)));
			if (length == null || length < 0 || !ArgType.array(ArgType.BYTE).equals(insn.getArrayType())) {
				return UNRESOLVED;
			}
			TrackedByteArray array = new TrackedByteArray(length);
			array.cleanup.add(insn);
			return array;
		}

		private Object filledNewArrayValue(FilledNewArrayNode insn) {
			if (insn.getElemType() != ArgType.BYTE) {
				return UNRESOLVED;
			}
			TrackedByteArray array = new TrackedByteArray(insn.getArgsCount());
			array.cleanup.add(insn);
			for (int i = 0; i < insn.getArgsCount(); i++) {
				Integer value = asInteger(valueOf(insn.getArg(i)));
				if (value == null) {
					return UNRESOLVED;
				}
				array.data[i] = (byte) value.intValue();
			}
			return array;
		}

		private Object arrayGetValue(InsnNode insn) {
			Object array = valueOf(insn.getArg(0));
			Integer index = asInteger(valueOf(insn.getArg(1)));
			if (index == null) {
				return UNRESOLVED;
			}
			byte[] data = arrayData(array);
			if (data == null || index < 0 || index >= data.length) {
				return UNRESOLVED;
			}
			return (int) data[index];
		}

		private void executeFillArray(FillArrayInsn insn) {
			Object value = valueOf(insn.getArg(0));
			if (!(value instanceof TrackedByteArray)) {
				return;
			}
			TrackedByteArray array = (TrackedByteArray) value;
			List<LiteralArg> literals = insn.getLiteralArgs(ArgType.BYTE);
			if (literals.size() > array.data.length) {
				return;
			}
			for (int i = 0; i < literals.size(); i++) {
				array.data[i] = (byte) literals.get(i).getLiteral();
			}
			array.cleanup.add(insn);
		}

		private void executeArrayPut(InsnNode insn) {
			Object value = valueOf(insn.getArg(0));
			if (!(value instanceof TrackedByteArray)) {
				return;
			}
			Integer index = asInteger(valueOf(insn.getArg(1)));
			Integer item = asInteger(valueOf(insn.getArg(2)));
			TrackedByteArray array = (TrackedByteArray) value;
			if (index == null || item == null || index < 0 || index >= array.data.length) {
				return;
			}
			array.data[index] = (byte) item.intValue();
			array.cleanup.add(insn);
		}

		private void setResult(InsnNode insn, Object value) {
			RegisterArg result = insn.getResult();
			if (result == null) {
				return;
			}
			if (value == UNRESOLVED) {
				regs.remove(result.getRegNum());
			} else {
				regs.put(result.getRegNum(), value);
			}
		}

		private static byte[] arrayData(Object value) {
			if (value instanceof TrackedByteArray) {
				return ((TrackedByteArray) value).data;
			}
			if (value instanceof byte[]) {
				return (byte[]) value;
			}
			return null;
		}

		private static Integer asInteger(Object value) {
			return value instanceof Number ? ((Number) value).intValue() : null;
		}
	}

	private static final class TrackedByteArray {
		private final byte[] data;
		private final Set<InsnNode> cleanup = new LinkedHashSet<>();

		TrackedByteArray(int length) {
			this.data = new byte[length];
		}
	}

	private static boolean sameSVar(RegisterArg first, InsnArg second) {
		if (!(second instanceof RegisterArg)) {
			return false;
		}
		RegisterArg secondReg = (RegisterArg) second;
		if (first.getSVar() != null && first.getSVar() == secondReg.getSVar()) {
			return true;
		}
		return first.getSVar() == null && secondReg.getSVar() == null && first.getRegNum() == secondReg.getRegNum();
	}

	private static Integer extractInt(InsnArg arg) {
		if (arg instanceof LiteralArg) {
			return (int) ((LiteralArg) arg).getLiteral();
		}
		InsnNode insn = unwrapArgInsn(arg);
		if (insn == null) {
			return null;
		}
		switch (insn.getType()) {
			case CONST:
				return extractInt(insn.getArg(0));

			case MOVE:
			case ONE_ARG:
			case CAST:
				return insn.getArgsCount() == 1 ? extractInt(insn.getArg(0)) : null;

			default:
				return null;
		}
	}

	private static InsnNode unwrapArgInsn(InsnArg arg) {
		if (arg instanceof InsnWrapArg) {
			return ((InsnWrapArg) arg).getWrapInsn();
		}
		if (arg instanceof RegisterArg) {
			return ((RegisterArg) arg).getAssignInsn();
		}
		return null;
	}
}
