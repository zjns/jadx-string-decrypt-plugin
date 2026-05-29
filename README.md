# String Decrypt Plugin

Generic JADX plugin template for replacing known string decrypt method calls with decoded string constants during decompilation.

It follows the approach described in jadx discussion #2742:

- register an after-load pass to decide whether the plugin should run for the loaded app
- register a decompile pass that scans invoke instructions
- when the configured decrypt method is called with constant arguments, evaluate that method body and replace the invoke with a `ConstStringNode`

## Build

```bash
./gradlew clean test dist
```

The plugin jar is written to:

```text
build/dist/jadx-string-decrypt-plugin-dev.jar
```

Install it:

```bash
jadx plugins --install-jar build/dist/jadx-string-decrypt-plugin-dev.jar
```

## Options

Default target method:

```text
Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;
```

Configure one or more decrypt methods:

```bash
jadx app.apk \
  -Pstring-decrypt.methodSignatures='Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;,Lcom/example/T;->decode([B[B)Ljava/lang/String;'
```

Supported signature forms:

```text
Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;
com.example.S.dec(Ljava/lang/String;)Ljava/lang/String;
```

Optional package gate:

```bash
jadx app.apk -Pstring-decrypt.targetPackage='com.example.'
```

## Relationship Between `methodSignatures` And `decoder`

`string-decrypt.methodSignatures` identifies the decrypt method calls to replace. By default, that same method body is treated as the decrypt algorithm:

```text
methodSignatures -> find invoke -> extract constant args -> evaluate matched method body -> replace invoke with string
```

So for the default signature:

```text
Lcom/example/S;->dec(Ljava/lang/String;)Ljava/lang/String;
```

the plugin will try to execute `dec(String)` with the constant string found at the call site.

`string-decrypt.decoder` is optional. The default is `method`:

```bash
jadx app.apk -Pstring-decrypt.decoder=method
jadx app.apk -Pstring-decrypt.decoder=template
jadx app.apk -Pstring-decrypt.decoder=identity
jadx app.apk -Pstring-decrypt.decoder=xor_utf8
```

Use `template`, `identity`, or `xor_utf8` only when you want to bypass method-body evaluation and force a plugin-side decoder.

## What It Handles

- `String -> String` decrypt calls when the argument is a constant string
- `[B, [B -> String` style calls when byte arrays are constants from `fill-array-data`
- `filled-new-array` byte literals if JADX has already wrapped them
- simple decrypt method bodies with constants, moves, arithmetic, byte arrays, array access, loops, branches, and `new String(byte[])`

If an argument is not constant, the method body uses unsupported instructions, or evaluation returns `null`, the original invoke is left unchanged.
