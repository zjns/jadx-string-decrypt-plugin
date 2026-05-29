.class public Lcom/example/BytesApp;
.super Ljava/lang/Object;

.method public static run()Ljava/lang/String;
    .registers 2
    const/4 v0, 0x5
    new-array v0, v0, [B
    fill-array-data v0, :cipher
    const/4 v1, 0x2
    new-array v1, v1, [B
    fill-array-data v1, :key
    invoke-static {v0, v1}, Lcom/example/BytesApp;->dec([B[B)Ljava/lang/String;
    move-result-object v0
    return-object v0

    :cipher
    .array-data 1
        0x23t
        0x0ct
        0x27t
        0x05t
        0x24t
    .end array-data

    :key
    .array-data 1
        0x4bt
        0x69t
    .end array-data
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
