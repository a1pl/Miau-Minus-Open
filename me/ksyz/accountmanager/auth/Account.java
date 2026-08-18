package me.ksyz.accountmanager.auth;

public class Account {
    public static final String TYPE_PREMIUM = "premium";
    public static final String TYPE_CRACKED = "cracked";
    public static final String TYPE_COOKIE = "cookie";
    private String refreshToken;
    private String accessToken;
    private String username;
    private long unban;
    private String clientId;
    private String scope;
    private String type;

    public Account(String refreshToken, String accessToken, String username, String clientId, String scope) {
        this(refreshToken, accessToken, username, 0L, clientId, scope, "premium");
    }

    public Account(String refreshToken, String accessToken, String username, long unban, String clientId, String scope) {
        this(refreshToken, accessToken, username, unban, clientId, scope, "premium");
    }

    public Account(
        String refreshToken,
        String accessToken,
        String username,
        long unban,
        String clientId,
        String scope,
        String type
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.unban = unban;
        this.clientId = clientId;
        this.scope = scope;
        this.type = type != null && !type.isEmpty() ? type : "premium";
    }

    public static Account cracked(String username) {
        return new Account("", "", username, 0L, "", "", "cracked");
    }

    public String getClientId() {
        return this.clientId;
    }

    public String getScope() {
        return this.scope;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getUsername() {
        return this.username;
    }

    public long getUnban() {
        return this.unban;
    }

    public String getType() {
        return this.type;
    }

    public boolean isCracked() {
        return "cracked".equals(this.type);
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUnban(long unban) {
        this.unban = unban;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setType(String type) {
        this.type = type != null && !type.isEmpty() ? type : "premium";
    }
}
