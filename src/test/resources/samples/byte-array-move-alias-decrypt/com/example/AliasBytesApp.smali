.class public Lcom/example/AliasBytesApp;
.super Ljava/lang/Object;

.method public static run()Ljava/lang/String;
    .registers 4
    const/4 v0, 0x5
    new-array v0, v0, [B
    fill-array-data v0, :cipher
    move-object v2, v0

    const/4 v1, 0x2
    new-array v1, v1, [B
    fill-array-data v1, :key
    move-object v3, v1

    invoke-static {v2, v3}, Lcom/example/AliasBytesApp;->dec([B[B)Ljava/lang/String;
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
    .registers 2
    const/4 v0, 0x0
    return-object v0
.end method
