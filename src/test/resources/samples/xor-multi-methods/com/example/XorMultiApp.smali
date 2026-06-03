.class public Lcom/example/XorMultiApp;
.super Ljava/lang/Object;

.method public static run()Ljava/lang/String;
    .registers 6
    const/4 v0, 0x5
    new-array v0, v0, [B
    fill-array-data v0, :cipher_a

    const/4 v1, 0x2
    new-array v1, v1, [B
    fill-array-data v1, :key_a

    invoke-static {v0, v1}, Lcom/example/XorMultiApp;->decA([B[B)Ljava/lang/String;
    move-result-object v0

    const/4 v1, 0x3
    new-array v1, v1, [B
    fill-array-data v1, :cipher_b

    const/4 v2, 0x1
    new-array v2, v2, [B
    fill-array-data v2, :key_b

    invoke-static {v1, v2}, Lcom/example/XorMultiApp;->decB([B[B)Ljava/lang/String;
    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    return-object v0

    :cipher_a
    .array-data 1
        0x23t
        0x0ct
        0x27t
        0x05t
        0x24t
    .end array-data

    :key_a
    .array-data 1
        0x4bt
        0x69t
    .end array-data

    :cipher_b
    .array-data 1
        0x67t
        0x6et
        0x6et
    .end array-data

    :key_b
    .array-data 1
        0x01t
    .end array-data
.end method

.method public static decA([B[B)Ljava/lang/String;
    .registers 2
    const/4 v0, 0x0
    return-object v0
.end method

.method public static decB([B[B)Ljava/lang/String;
    .registers 2
    const/4 v0, 0x0
    return-object v0
.end method
