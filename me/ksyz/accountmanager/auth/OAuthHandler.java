package me.ksyz.accountmanager.auth;

public interface OAuthHandler {
    void openUrl(String var1);

    void authResult(String var1, String var2, String var3);

    void authError(String var1);
}
