package me.ksyz.accountmanager.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.OAuthHandler;
import me.ksyz.accountmanager.auth.OAuthServer;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;

public class GuiMicrosoftFaiths extends GuiScreen {
    private static final String CLIENT_ID = "0add8caf-2cc6-4546-b798-c3d171217dd9";
    private static final String REDIRECT_URI = "http://localhost:21919/login";
    private static final String SCOPE = "XboxLive.signin%20offline_access";
    private static final String XBOX_AUTH_URL = "https://login.live.com/oauth20_token.srf";
    private static final String XBOX_REFRESH_DATA = "client_id=<client_id>&redirect_uri=<redirect_uri>&grant_type=authorization_code&code=";
    private static final String XBOX_REFRESH_TOKEN_DATA = "client_id=<client_id>&redirect_uri=<redirect_uri>&grant_type=refresh_token&refresh_token=";
    private static final ExecutorService SHARED_EXECUTOR = Executors.newSingleThreadExecutor();
    private final GuiScreen previousScreen;
    private GuiButton cancelButton;
    private ExecutorService executor;
    private CompletableFuture<Void> task;
    private String status;
    private boolean success;
    private OAuthServer oAuthServer;

    public GuiMicrosoftFaiths(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        this.field_146292_n
            .add(
                this.cancelButton = new GuiButton(
                    0, this.field_146294_l / 2 - 50, this.field_146295_m / 2 + 60, 100, 20, "Cancel"
                )
            );
        if (this.task == null) {
            this.status = "&fPreparing Microsoft login...&r";
            if (this.executor == null) {
                this.executor = Executors.newSingleThreadExecutor();
            }

            this.startOAuthFlow();
        }
    }

    private void startOAuthFlow() {
        try {
            this.oAuthServer = new OAuthServer(
                new OAuthHandler() {
                    @Override
                    public void openUrl(String url) {
                        Toolkit toolkit = Toolkit.getDefaultToolkit();
                        StringSelection strSel = new StringSelection(url);
                        toolkit.getSystemClipboard().setContents(strSel, null);
                        GuiMicrosoftFaiths.this.status = "&fLogin link copied to clipboard! Open your browser and paste it.&r";
                    }

                    @Override
                    public void authResult(String code, String clientId, String scope) {
                        GuiMicrosoftFaiths.this.status = "&fAuth code received, logging in...&r";
                        GuiMicrosoftFaiths.this.loginWithCode(code, clientId, scope);
                    }

                    @Override
                    public void authError(String error) {
                        GuiMicrosoftFaiths.this.status = "&cError: " + error + "&r";
                    }
                },
                "0add8caf-2cc6-4546-b798-c3d171217dd9",
                "http://localhost:21919/login",
                "XboxLive.signin%20offline_access"
            );
            this.oAuthServer.start();
        } catch (Exception e) {
            this.status = "&cFailed to start authentication server: " + e.getMessage() + "&r";
        }
    }

    private void loginWithCode(String code, String clientId, String scope) {
        AtomicReference<String> refreshToken = new AtomicReference<>("");
        this.task = CompletableFuture.<Object[]>supplyAsync(
                () -> {
                    try {
                        String body = "client_id=<client_id>&redirect_uri=<redirect_uri>&grant_type=authorization_code&code="
                                .replace("<client_id>", clientId)
                                .replace("<redirect_uri>", "http://localhost:21919/login")
                            + code;
                        URL url = new URL("https://login.live.com/oauth20_token.srf");
                        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        conn.setDoOutput(true);
                        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                        conn.connect();
                        Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                        String response = scanner.hasNext() ? scanner.next() : "";
                        conn.disconnect();
                        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
                        String token = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
                        String access = json.has("access_token") ? json.get("access_token").getAsString() : null;
                        if (token == null) {
                            throw new Exception("Failed to get refresh token");
                        }

                        refreshToken.set(token);
                        String mcToken = this.microsoftToMinecraft(access, clientId);
                        Session session = this.fetchProfile(mcToken);
                        return new Object[]{session, token};
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                this.executor
            )
            .thenAccept(
                result -> {
                    Session session = (Session)result[0];
                    String token = (String)result[1];
                    Account acc = new Account(
                        token,
                        session.func_148254_d(),
                        session.func_111285_a(),
                        "0add8caf-2cc6-4546-b798-c3d171217dd9",
                        "XboxLive.signin%20offline_access"
                    );

                    for (Account a : AccountManager.accounts) {
                        if (acc.getUsername().equals(a.getUsername())) {
                            acc.setUnban(a.getUnban());
                            break;
                        }
                    }

                    AccountManager.accounts.add(acc);
                    AccountManager.save();
                    SessionManager.set(session);
                    this.success = true;
                }
            )
            .exceptionally(error -> {
                this.status = "&c" + error.getCause().getMessage() + "&r";
                return null;
            });
    }

    public static String refreshToken(String refreshToken, String clientId) throws Exception {
        String body = "client_id=<client_id>&redirect_uri=<redirect_uri>&grant_type=refresh_token&refresh_token="
                .replace("<client_id>", clientId)
                .replace("<redirect_uri>", "http://localhost:21919/login")
            + refreshToken;
        URL url = new URL("https://login.live.com/oauth20_token.srf");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        conn.connect();
        Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        conn.disconnect();
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        return !json.has("refresh_token") ? null : json.get("refresh_token").getAsString();
    }

    public static void attemptAutoLogin() {
        AccountManager.load();
        if (!AccountManager.accounts.isEmpty()) {
            Account last = AccountManager.accounts.get(AccountManager.accounts.size() - 1);
            if (last != null && !last.getRefreshToken().isEmpty() && !last.getUsername().isEmpty()) {
                try {
                    String newRefresh = refreshToken(last.getRefreshToken(), last.getClientId());
                    if (newRefresh == null) {
                        return;
                    }

                    String body = "client_id=<client_id>&redirect_uri=<redirect_uri>&grant_type=refresh_token&refresh_token="
                            .replace("<client_id>", last.getClientId())
                            .replace("<redirect_uri>", "http://localhost:21919/login")
                        + newRefresh;
                    URL url = new URL("https://login.live.com/oauth20_token.srf");
                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                    conn.connect();
                    Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                    String resp = s.hasNext() ? s.next() : "";
                    conn.disconnect();
                    JsonObject json = new JsonParser().parse(resp).getAsJsonObject();
                    if (!json.has("access_token")) {
                        return;
                    }

                    String accessToken = json.get("access_token").getAsString();
                    String mcToken = new GuiMicrosoftFaiths(null).microsoftToMinecraft(accessToken, last.getClientId());
                    Session session = new GuiMicrosoftFaiths(null).fetchProfile(mcToken);
                    SessionManager.set(session);
                    int idx = AccountManager.accounts.size() - 1;
                    AccountManager.accounts
                        .set(
                            idx,
                            new Account(
                                newRefresh,
                                session.func_148254_d(),
                                session.func_111285_a(),
                                last.getUnban(),
                                last.getClientId(),
                                last.getScope()
                            )
                        );
                    AccountManager.save();
                } catch (Exception var12) {
                }
            }
        }
    }

    private String microsoftToMinecraft(String msAccessToken, String clientId) throws Exception {
        String rpsRule = "d=" + msAccessToken;
        URL url = new URL("https://user.auth.xboxlive.com/user/authenticate");
        String xblBody = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"<rps_ticket>\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}"
            .replace("<rps_ticket>", rpsRule);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(xblBody.getBytes(StandardCharsets.UTF_8));
        conn.connect();
        Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String xblResp = s.hasNext() ? s.next() : "";
        conn.disconnect();
        JsonObject xblJson = new JsonParser().parse(xblResp).getAsJsonObject();
        String xblToken = xblJson.get("Token").getAsString();
        String userHash = xblJson.get("DisplayClaims")
            .getAsJsonObject()
            .get("xui")
            .getAsJsonArray()
            .get(0)
            .getAsJsonObject()
            .get("uhs")
            .getAsString();
        URL xstsUrl = new URL("https://xsts.auth.xboxlive.com/xsts/authorize");
        String xstsBody = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"<xbl_token>\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}"
            .replace("<xbl_token>", xblToken);
        conn = (HttpURLConnection)xstsUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(xstsBody.getBytes(StandardCharsets.UTF_8));
        conn.connect();
        s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String xstsResp = s.hasNext() ? s.next() : "";
        conn.disconnect();
        JsonObject xstsJson = new JsonParser().parse(xstsResp).getAsJsonObject();
        String xstsToken = xstsJson.get("Token").getAsString();
        URL mcUrl = new URL("https://api.minecraftservices.com/authentication/login_with_xbox");
        String mcBody = "{\"identityToken\":\"XBL3.0 x=<userhash>;<xsts_token>\"}"
            .replace("<userhash>", userHash)
            .replace("<xsts_token>", xstsToken);
        conn = (HttpURLConnection)mcUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.getOutputStream().write(mcBody.getBytes(StandardCharsets.UTF_8));
        conn.connect();
        s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String mcResp = s.hasNext() ? s.next() : "";
        conn.disconnect();
        JsonObject mcJson = new JsonParser().parse(mcResp).getAsJsonObject();
        return mcJson.get("access_token").getAsString();
    }

    private Session fetchProfile(String mcToken) throws Exception {
        URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + mcToken);
        conn.connect();
        Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        String resp = s.hasNext() ? s.next() : "";
        conn.disconnect();
        JsonObject json = new JsonParser().parse(resp).getAsJsonObject();
        if (!json.has("id")) {
            throw new Exception("No Minecraft profile found");
        } else {
            return new Session(json.get("name").getAsString(), json.get("id").getAsString(), mcToken, "mojang");
        }
    }

    public void func_146281_b() {
        if (this.oAuthServer != null) {
            this.oAuthServer.stop(true);
        }

        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            this.executor.shutdownNow();
        }
    }

    public void func_73876_c() {
        if (this.success) {
            this.field_146297_k
                .func_147108_a(
                    new GuiAccountManager(
                        this.previousScreen,
                        new Notification(
                            TextFormatting.translate(
                                String.format("&aSuccessful login! (%s)&r", SessionManager.get().func_111285_a())
                            ),
                            5000L
                        )
                    )
                );
            this.success = false;
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.func_146276_q_();
        super.func_73863_a(mouseX, mouseY, partialTicks);
        this.func_73732_a(
            this.field_146289_q,
            "Microsoft Authentication",
            this.field_146294_l / 2,
            this.field_146295_m / 2 - 50,
            11184810
        );
        if (this.status != null) {
            this.func_73732_a(
                this.field_146289_q,
                TextFormatting.translate(this.status),
                this.field_146294_l / 2,
                this.field_146295_m / 2 - 10,
                -1
            );
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.func_146284_a(this.cancelButton);
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button != null && button.field_146124_l) {
            if (button.field_146127_k == 0) {
                if (this.oAuthServer != null) {
                    this.oAuthServer.stop(true);
                }

                this.field_146297_k.func_147108_a(new GuiAccountManager(this.previousScreen));
            }
        }
    }
}
