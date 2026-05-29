package com.kofua.jadx.stringdecrypt;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayInsn;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;

public class MethodBodyEvaluator {
	private static final int MAX_STEPS = 10_000;

	public String evaluateToString(MethodNode method, Object[] args) {
		Object result = evaluate(method, args);
		return result instanceof String ? (String) result : null;
	}

	public Object evaluate(MethodNode method, Object[] args) {
		List<BlockNode> blocks = method.getBasicBlocks();
		if (blocks == null || blocks.isEmpty()) {
			return null;
		}
		EvalState state = new EvalState();
		List<RegisterArg> argRegs = method.getArgRegs();
		if (args.length < argRegs.size()) {
			return null;
		}
		for (int i = 0; i < argRegs.size(); i++) {
			state.set(argRegs.get(i), copyValue(args[i]));
		}

		BlockNode block = method.getEnterBlock() != null ? method.getEnterBlock() : blocks.get(0);
		for (int steps = 0; block != null && steps < MAX_STEPS; steps++) {
			StepResult result = executeBlock(method, state, block);
			if (result.returned) {
				return result.value;
			}
			block = result.nextBlock;
		}
		return null;
	}

	private StepResult executeBlock(MethodNode method, EvalState state, BlockNode block) {
		List<InsnNode> instructions = new ArrayList<>(block.getInstructions());
		for (InsnNode insn : instructions) {
			switch (insn.getType()) {
				case CONST:
					writeResult(state, insn, valueOf(state, insn.getArg(0)));
					break;

					case CONST_STR:
						writeResult(state, insn, ((ConstStringNode) insn).getString());
						break;

					case SGET:
						writeResult(state, insn, valueOfStaticField((IndexInsnNode) insn));
						break;

					case MOVE:
					case ONE_ARG:
					case CAST:
					writeResult(state, insn, castIfNeeded(insn, valueOf(state, insn.getArg(0))));
					break;

				case ARITH:
					executeArith(state, (ArithNode) insn);
					break;

				case ARRAY_LENGTH:
					writeResult(state, insn, arrayLength(valueOf(state, insn.getArg(0))));
					break;

				case NEW_ARRAY:
					writeResult(state, insn, newArray((NewArrayNode) insn, asInt(valueOf(state, insn.getArg(0)))));
					break;

				case FILL_ARRAY:
					executeFillArray(state, (FillArrayInsn) insn);
					break;

				case FILLED_NEW_ARRAY:
					writeResult(state, insn, filledNewArray(state, (FilledNewArrayNode) insn));
					break;

				case AGET:
					writeResult(state, insn, arrayGet(valueOf(state, insn.getArg(0)), asInt(valueOf(state, insn.getArg(1)))));
					break;

				case APUT:
					arrayPut(valueOf(state, insn.getArg(0)), asInt(valueOf(state, insn.getArg(1))), valueOf(state, insn.getArg(2)));
					break;

				case NEW_INSTANCE:
					writeResult(state, insn, new ObjectValue(((IndexInsnNode) insn).getIndex().toString()));
					break;

				case CONSTRUCTOR:
					writeResult(state, insn, executeConstructor(state, (ConstructorInsn) insn));
					break;

				case INVOKE:
					executeInvoke(state, (BaseInvokeNode) insn);
					break;

				case IF:
					return StepResult.next(evaluateIf(state, (IfNode) insn) ? ((IfNode) insn).getThenBlock() : ((IfNode) insn).getElseBlock());

				case GOTO:
					return StepResult.next(BlockUtils.getBlockByOffset(((GotoNode) insn).getTarget(), method.getBasicBlocks()));

				case RETURN:
					return StepResult.returnValue(insn.getArgsCount() == 0 ? null : valueOf(state, insn.getArg(0)));

				case NOP:
				case MOVE_RESULT:
					break;

				default:
					throw new UnsupportedOperationException("Unsupported instruction in method evaluator: " + insn);
			}
		}
		List<BlockNode> successors = block.getSuccessors();
		return StepResult.next(successors.isEmpty() ? null : successors.get(0));
	}

	private static void executeArith(EvalState state, ArithNode insn) {
		int value = arithValue(state, insn);
		RegisterArg result = insn.getResult();
		if (result != null) {
			state.set(result, value);
		} else if (insn.getArg(0) instanceof RegisterArg) {
			state.set((RegisterArg) insn.getArg(0), value);
		}
	}

	private static int arithValue(EvalState state, ArithNode insn) {
		int a = asInt(valueOf(state, insn.getArg(0)));
		int b = asInt(valueOf(state, insn.getArg(1)));
		switch (insn.getOp()) {
			case ADD:
				return a + b;
			case SUB:
				return a - b;
			case MUL:
				return a * b;
			case DIV:
				return a / b;
			case REM:
				return a % b;
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
				throw new UnsupportedOperationException("Unsupported arithmetic op: " + insn.getOp());
		}
	}

	private static void executeFillArray(EvalState state, FillArrayInsn insn) {
		Object array = valueOf(state, insn.getArg(0));
		List<LiteralArg> literals = insn.getLiteralArgs(ArgType.BYTE);
		if (array instanceof byte[]) {
			byte[] bytes = (byte[]) array;
			for (int i = 0; i < literals.size(); i++) {
				bytes[i] = (byte) literals.get(i).getLiteral();
			}
			return;
		}
		throw new UnsupportedOperationException("Unsupported fill-array target: " + array);
	}

	private static Object executeConstructor(EvalState state, ConstructorInsn insn) {
		String callId = insn.getCallMth().getRawFullId();
		if (callId.equals("java.lang.String.<init>([B)V") && insn.getArgsCount() == 1) {
			return new String((byte[]) valueOf(state, insn.getArg(0)), StandardCharsets.UTF_8);
		}
		if (callId.equals("java.lang.String.<init>([BLjava/nio/charset/Charset;)V") && insn.getArgsCount() == 2) {
			return new String((byte[]) valueOf(state, insn.getArg(0)), asCharset(valueOf(state, insn.getArg(1))));
		}
		throw new UnsupportedOperationException("Unsupported constructor: " + callId);
	}

	private static void executeInvoke(EvalState state, BaseInvokeNode insn) {
		String callId = insn.getCallMth().getRawFullId();
		if (callId.equals("java.lang.String.<init>([B)V") && insn.getArgsCount() >= 2) {
			Object instance = rawValueOf(state, insn.getArg(0));
			if (instance instanceof ObjectValue) {
				((ObjectValue) instance).value = new String((byte[]) valueOf(state, insn.getArg(1)), StandardCharsets.UTF_8);
				}
				return;
			}
		if (callId.equals("java.lang.String.<init>([BLjava/nio/charset/Charset;)V") && insn.getArgsCount() >= 3) {
			Object instance = rawValueOf(state, insn.getArg(0));
			if (instance instanceof ObjectValue) {
				((ObjectValue) instance).value = new String(
						(byte[]) valueOf(state, insn.getArg(1)),
						asCharset(valueOf(state, insn.getArg(2))));
			}
			return;
		}
		throw new UnsupportedOperationException("Unsupported invoke: " + callId);
	}

	private static boolean evaluateIf(EvalState state, IfNode insn) {
		int a = asInt(valueOf(state, insn.getArg(0)));
		int b = asInt(valueOf(state, insn.getArg(1)));
		IfOp op = insn.getOp();
		switch (op) {
			case EQ:
				return a == b;
			case NE:
				return a != b;
			case LT:
				return a < b;
			case LE:
				return a <= b;
			case GT:
				return a > b;
			case GE:
				return a >= b;
			default:
				throw new UnsupportedOperationException("Unsupported if op: " + op);
		}
	}

	private static Object castIfNeeded(InsnNode insn, Object value) {
		if (insn instanceof IndexInsnNode && ((IndexInsnNode) insn).getIndex() == ArgType.BYTE) {
			return (int) (byte) asInt(value);
		}
		return value;
	}

	private static Object newArray(NewArrayNode insn, int length) {
		ArgType arrayType = insn.getArrayType();
		if (arrayType.equals(ArgType.array(ArgType.BYTE))) {
			return new byte[length];
		}
		if (arrayType.equals(ArgType.array(ArgType.INT))) {
			return new int[length];
		}
		return new Object[length];
	}

	private static Object filledNewArray(EvalState state, FilledNewArrayNode insn) {
		if (insn.getElemType() == ArgType.BYTE) {
			byte[] data = new byte[insn.getArgsCount()];
			for (int i = 0; i < insn.getArgsCount(); i++) {
				data[i] = (byte) asInt(valueOf(state, insn.getArg(i)));
			}
			return data;
		}
		Object[] data = new Object[insn.getArgsCount()];
		for (int i = 0; i < insn.getArgsCount(); i++) {
			data[i] = valueOf(state, insn.getArg(i));
		}
		return data;
	}

	private static int arrayLength(Object array) {
		if (array instanceof byte[]) {
			return ((byte[]) array).length;
		}
		if (array instanceof int[]) {
			return ((int[]) array).length;
		}
		if (array == null) {
			throw new UnsupportedOperationException("Unsupported array-length target: null");
		}
		return ((Object[]) array).length;
	}

	private static Object arrayGet(Object array, int index) {
		if (array instanceof byte[]) {
			return (int) ((byte[]) array)[index];
		}
		if (array instanceof int[]) {
			return ((int[]) array)[index];
		}
		if (array == null) {
			throw new UnsupportedOperationException("Unsupported array-get target: null");
		}
		return ((Object[]) array)[index];
	}

	private static void arrayPut(Object array, int index, Object value) {
		if (array instanceof byte[]) {
			((byte[]) array)[index] = (byte) asInt(value);
			return;
		}
		if (array instanceof int[]) {
			((int[]) array)[index] = asInt(value);
			return;
		}
		if (array == null) {
			throw new UnsupportedOperationException("Unsupported array-put target: null");
		}
		((Object[]) array)[index] = value;
	}

	private static void writeResult(EvalState state, InsnNode insn, Object value) {
		RegisterArg result = insn.getResult();
		if (result != null) {
			state.set(result, value);
		}
	}

	private static Object valueOf(EvalState state, InsnArg arg) {
		Object value = rawValueOf(state, arg);
		return value instanceof ObjectValue ? ((ObjectValue) value).value : value;
	}

	private static Object rawValueOf(EvalState state, InsnArg arg) {
		if (arg instanceof RegisterArg) {
			return state.get((RegisterArg) arg);
		}
		if (arg instanceof LiteralArg) {
			return (int) ((LiteralArg) arg).getLiteral();
		}
		if (arg instanceof InsnWrapArg) {
			return valueOfInsn(state, ((InsnWrapArg) arg).getWrapInsn());
		}
		throw new UnsupportedOperationException("Unsupported argument: " + arg);
	}

	private static Object valueOfInsn(EvalState state, InsnNode insn) {
		switch (insn.getType()) {
			case CONST:
				return valueOf(state, insn.getArg(0));

				case CONST_STR:
					return ((ConstStringNode) insn).getString();

				case SGET:
					return valueOfStaticField((IndexInsnNode) insn);

				case MOVE:
				case ONE_ARG:
				case CAST:
				return castIfNeeded(insn, valueOf(state, insn.getArg(0)));

			case ARITH:
				return arithValue(state, (ArithNode) insn);

			case ARRAY_LENGTH:
				return arrayLength(valueOf(state, insn.getArg(0)));

			case NEW_ARRAY:
				return newArray((NewArrayNode) insn, asInt(valueOf(state, insn.getArg(0))));

			case FILLED_NEW_ARRAY:
				return filledNewArray(state, (FilledNewArrayNode) insn);

			case AGET:
				return arrayGet(valueOf(state, insn.getArg(0)), asInt(valueOf(state, insn.getArg(1))));

			case NEW_INSTANCE:
				return new ObjectValue(((IndexInsnNode) insn).getIndex().toString());

			case CONSTRUCTOR:
				return executeConstructor(state, (ConstructorInsn) insn);

			default:
				throw new UnsupportedOperationException("Unsupported wrapped instruction in method evaluator: " + insn);
		}
	}

	private static Object valueOfStaticField(IndexInsnNode insn) {
		Object index = insn.getIndex();
		if (index instanceof FieldInfo) {
			String fieldId = ((FieldInfo) index).getRawFullId();
			switch (fieldId) {
				case "java.nio.charset.StandardCharsets.US_ASCII:Ljava/nio/charset/Charset;":
					return StandardCharsets.US_ASCII;
				case "java.nio.charset.StandardCharsets.ISO_8859_1:Ljava/nio/charset/Charset;":
					return StandardCharsets.ISO_8859_1;
				case "java.nio.charset.StandardCharsets.UTF_8:Ljava/nio/charset/Charset;":
					return StandardCharsets.UTF_8;
				case "java.nio.charset.StandardCharsets.UTF_16BE:Ljava/nio/charset/Charset;":
					return StandardCharsets.UTF_16BE;
				case "java.nio.charset.StandardCharsets.UTF_16LE:Ljava/nio/charset/Charset;":
					return StandardCharsets.UTF_16LE;
				case "java.nio.charset.StandardCharsets.UTF_16:Ljava/nio/charset/Charset;":
					return StandardCharsets.UTF_16;
				default:
					break;
			}
		}
		throw new UnsupportedOperationException("Unsupported static field: " + index);
	}

	private static Charset asCharset(Object value) {
		if (value instanceof Charset) {
			return (Charset) value;
		}
		if (value instanceof String) {
			return Charset.forName((String) value);
		}
		throw new UnsupportedOperationException("Unsupported charset value: " + value);
	}

	private static int asInt(Object value) {
		return ((Number) value).intValue();
	}

	private static Object copyValue(Object value) {
		if (value instanceof byte[]) {
			return Arrays.copyOf((byte[]) value, ((byte[]) value).length);
		}
		if (value instanceof int[]) {
			return Arrays.copyOf((int[]) value, ((int[]) value).length);
		}
		return value;
	}

	private static final class EvalState {
		private final Map<Integer, Object> regs = new HashMap<>();

		void set(RegisterArg arg, Object value) {
			regs.put(arg.getRegNum(), value);
		}

		Object get(RegisterArg arg) {
			return regs.get(arg.getRegNum());
		}
	}

	private static final class ObjectValue {
		private final String type;
		private Object value;

		ObjectValue(String type) {
			this.type = type;
		}

		@Override
		public String toString() {
			return type + ':' + value;
		}
	}

	private static final class StepResult {
		private final boolean returned;
		private final Object value;
		private final BlockNode nextBlock;

		private StepResult(boolean returned, Object value, BlockNode nextBlock) {
			this.returned = returned;
			this.value = value;
			this.nextBlock = nextBlock;
		}

		static StepResult returnValue(Object value) {
			return new StepResult(true, value, null);
		}

		static StepResult next(BlockNode block) {
			return new StepResult(false, null, block);
		}
	}
}
