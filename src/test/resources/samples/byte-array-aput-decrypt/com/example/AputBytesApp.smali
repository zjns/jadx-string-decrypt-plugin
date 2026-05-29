.class public Lcom/example/AputBytesApp;
.super Ljava/lang/Object;

.method public static run()Ljava/lang/String;
    .registers 8
    const/4 v1, 0x3
    new-array v2, v1, [B

    const/16 v3, 0x67
    const/4 v4, 0x0
    aput-byte v3, v2, v4

    const/16 v3, 0x7c
    const/4 v5, 0x1
    aput-byte v3, v2, v5

    const/16 v3, 0x52
    const/4 v6, 0x2
    aput-byte v3, v2, v6

    const/16 v3, 0x8
    new-array v3, v3, [B

    aput-byte v4, v3, v4

    const/16 v7, 0x19
    aput-byte v7, v3, v5

    const/16 v5, 0x26
    aput-byte v5, v3, v6

    const/16 v5, 0x12
    aput-byte v5, v3, v1

    const/4 v1, 0x4
    const/16 v5, 0xe
    aput-byte v5, v3, v1

    const/4 v1, 0x5
    const/16 v5, 0xc
    aput-byte v5, v3, v1

    const/4 v1, 0x6
    const/16 v5, 0x11
    aput-byte v5, v3, v1

    const/4 v1, 0x7
    const/16 v5, 0x9
    aput-byte v5, v3, v1

    invoke-static {v2, v3}, Lcom/example/AputBytesApp;->dec([B[B)Ljava/lang/String;
    move-result-object v0
    return-object v0
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
