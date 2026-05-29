.class public Lcom/example/LoopCarriedBytesApp;
.super Ljava/lang/Object;

.method public static run()Ljava/lang/String;
    .registers 12
    const/4 v0, 0x1
    const/4 v1, 0x5
    const/4 v2, 0x6
    const/4 v3, 0x7
    const/4 v4, 0x0
    const-string v11, ""

    :loop
    if-ge v4, v0, :done

    const/4 v5, 0x3
    new-array v5, v5, [B
    const/16 v6, 0x67
    const/4 v7, 0x0
    aput-byte v6, v5, v7
    const/16 v6, 0x7c
    const/4 v8, 0x1
    aput-byte v6, v5, v8
    const/16 v6, 0x52
    const/4 v9, 0x2
    aput-byte v6, v5, v9

    const/16 v6, 0x8
    new-array v6, v6, [B
    aput-byte v7, v6, v7
    const/16 v10, 0x19
    aput-byte v10, v6, v8
    const/16 v8, 0x26
    aput-byte v8, v6, v9
    const/16 v8, 0x12
    const/4 v9, 0x3
    aput-byte v8, v6, v9
    const/16 v8, 0xe
    const/4 v9, 0x4
    aput-byte v8, v6, v9
    const/16 v8, 0xc
    aput-byte v8, v6, v1
    const/16 v8, 0x11
    aput-byte v8, v6, v2
    const/16 v8, 0x9
    aput-byte v8, v6, v3

    invoke-static {v5, v6}, Lcom/example/LoopCarriedBytesApp;->dec([B[B)Ljava/lang/String;
    move-result-object v11
    add-int/lit8 v4, v4, 0x1
    const/4 v1, 0x5
    const/4 v2, 0x6
    const/4 v3, 0x7
    goto :loop

    :done
    return-object v11
.end method

.method public static dec([B[B)Ljava/lang/String;
    .registers 7
    array-length v0, p0
    new-array v1, v0, [B
    const/4 v0, 0x0

    :loop
    array-length v2, p0
    if-ge v0, v2, :done
    aget-byte v2, p0, v0
    array-length v3, p1
    rem-int v3, v0, v3
    aget-byte v3, p1, v3
    xor-int/2addr v2, v3
    int-to-byte v2, v2
    aput-byte v2, v1, v0
    add-int/lit8 v0, v0, 0x1
    goto :loop

    :done
    new-instance v4, Ljava/lang/String;
    invoke-direct {v4, v1}, Ljava/lang/String;-><init>([B)V
    return-object v4
.end method
