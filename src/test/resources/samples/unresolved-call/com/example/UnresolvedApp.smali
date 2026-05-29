.class public Lcom/example/UnresolvedApp;
.super Ljava/lang/Object;

.method public static run([B[B)Ljava/lang/String;
    .registers 2
    invoke-static {p0, p1}, Lcom/example/UnresolvedApp;->dec([B[B)Ljava/lang/String;
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
