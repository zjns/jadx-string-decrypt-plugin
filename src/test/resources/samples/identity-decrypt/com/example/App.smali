.class public Lcom/example/App;
.super Ljava/lang/Object;

.method public static run()Ljava/lang/String;
    .registers 1
    const-string v0, "cipher"
    invoke-static {v0}, Lcom/example/App;->dec(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    return-object v0
.end method

.method public static dec(Ljava/lang/String;)Ljava/lang/String;
    .registers 1
    return-object p0
.end method
