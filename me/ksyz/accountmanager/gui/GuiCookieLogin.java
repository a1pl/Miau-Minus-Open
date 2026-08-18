package me.ksyz.accountmanager.gui;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class GuiCookieLogin extends GuiScreen {
    private static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    private static final String SCOPE = "XboxLive.signin XboxLive.offline_access";
    private final GuiScreen previousScreen;
    private GuiButton chooseButton = null;
    private String status = null;
    private String cause = null;
    private String selectedFile = null;
    private volatile boolean chooserOpen = false;
    private ExecutorService executor = null;
    private volatile CompletableFuture<Void> task = null;
    private volatile boolean success = false;

    public GuiCookieLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        Keyboard.enableRepeatEvents(true);
        if (this.executor == null) {
            this.executor = Executors.newSingleThreadExecutor();
        }

        this.field_146292_n
            .add(
                this.chooseButton = new GuiButton(
                    998, this.field_146294_l / 2 - 100, this.field_146295_m / 2, 200, 20, "Choose Cookie File..."
                )
            );
    }

    public void func_146281_b() {
        Keyboard.enableRepeatEvents(false);
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
        }

        if (this.executor != null) {
            this.executor.shutdownNow();
        }
    }

    public void func_73876_c() {
        if (this.chooseButton != null) {
            this.chooseButton.field_146124_l = !this.chooserOpen && (this.task == null || this.task.isDone());
        }

        if (this.success) {
            this.success = false;
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
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.func_146276_q_();
        super.func_73863_a(mouseX, mouseY, partialTicks);
        this.func_73732_a(
            this.field_146289_q,
            "Cookie Login",
            this.field_146294_l / 2,
            this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - this.field_146289_q.field_78288_b * 3 - 6,
            11184810
        );
        String info = this.status != null
            ? this.status
            : "&7Choose an exported cookies file (Netscape .txt or JSON).&r";
        this.func_73732_a(
            this.field_146289_q,
            TextFormatting.translate(info),
            this.field_146294_l / 2,
            this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - this.field_146289_q.field_78288_b - 8,
            -1
        );
        if (this.selectedFile != null) {
            this.func_73732_a(
                this.field_146289_q,
                TextFormatting.translate(String.format("&8File: &7%s&r", this.selectedFile)),
                this.field_146294_l / 2,
                this.field_146295_m / 2 + 26,
                -1
            );
        }

        if (this.cause != null) {
            String causeText = TextFormatting.translate(this.cause);
            Gui.func_73734_a(
                0,
                this.field_146295_m - 2 - this.field_146289_q.field_78288_b - 3,
                3 + this.field_146297_k.field_71466_p.func_78256_a(causeText) + 3,
                this.field_146295_m,
                1677721600
            );
            this.func_73731_b(
                this.field_146289_q, causeText, 3, this.field_146295_m - 2 - this.field_146289_q.field_78288_b, -1
            );
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) {
        if (keyCode == 1 && !this.chooserOpen && (this.task == null || this.task.isDone())) {
            this.field_146297_k.func_147108_a(this.previousScreen);
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button != null && button.field_146124_l) {
            if (button.field_146127_k == 998) {
                this.openFileChooser();
            }
        }
    }

    private void openFileChooser() {
        if (!this.chooserOpen && (this.task == null || this.task.isDone())) {
            this.chooserOpen = true;
            this.cause = null;
            this.status = "&7Opening file chooser...&r";
            Thread thread = new Thread(() -> {
                File chosen = null;

                try {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception var9) {
                    }

                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Select your exported cookies file");
                    chooser.setFileSelectionMode(0);
                    chooser.setFileFilter(new FileNameExtensionFilter("Cookie files (*.txt, *.json)", "txt", "json"));
                    chooser.setAcceptAllFileFilterUsed(true);
                    JFrame parent = new JFrame();
                    parent.setAlwaysOnTop(true);
                    parent.setLocationRelativeTo(null);
                    int result = chooser.showOpenDialog(parent);
                    parent.dispose();
                    if (result == 0) {
                        chosen = chooser.getSelectedFile();
                    }
                } catch (Throwable t) {
                    this.status = String.format("&cFile chooser failed: %s&r", t.getMessage());
                } finally {
                    this.chooserOpen = false;
                }

                if (chosen != null) {
                    this.startLogin(chosen);
                } else if (this.status != null && this.status.contains("Opening")) {
                    this.status = "&7No file selected.&r";
                }
            }, "Cookie File Chooser");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void startLogin(File file) {
        if (this.task == null || this.task.isDone()) {
            String[] cookieLines;
            try {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                cookieLines = content.split("\\r?\\n");
            } catch (Exception e) {
                this.status = String.format("&cCould not read file: %s&r", e.getMessage());
                return;
            }

            if (cookieLines.length == 0) {
                this.status = "&cNo cookies were found in that file.&r";
            } else {
                this.selectedFile = file.getName();
                this.cause = null;
                CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
                SCOPE = "XboxLive.signin XboxLive.offline_access";
                AtomicReference<String> refreshToken = new AtomicReference<>("");
                AtomicReference<String> accessToken = new AtomicReference<>("");
                this.status = "&fAuthenticating with cookies (Sisu)&r";
                this.task = CompletableFuture.<Map<String, String>>supplyAsync(
                        () -> {
                            try {
                                StringBuilder cookies = new StringBuilder();
                                List<String> cooki = new ArrayList<>();

                                for (String cookie : cookieLines) {
                                    String[] parts = cookie.split("\t");
                                    if (parts.length >= 7
                                        && parts[0].endsWith("login.live.com")
                                        && !cooki.contains(parts[5])) {
                                        cookies.append(parts[5]).append("=").append(parts[6]).append("; ");
                                        cooki.add(parts[5]);
                                    }
                                }

                                if (cookies.length() > 2) {
                                    cookies = new StringBuilder(cookies.substring(0, cookies.length() - 2));
                                    this.status = "&fAcquiring Xbox token (Sisu)&r";
                                    HttpsURLConnection connection = (HttpsURLConnection)new URL(
                                            "https://sisu.xboxlive.com/connect/XboxLive/?state=login&cobrandId=8058f65d-ce06-4c30-9559-473c9275a65d&tid=896928775&ru=https%3A%2F%2Fwww.minecraft.net%2Fen-us%2Flogin&aid=1142970254"
                                        )
                                        .openConnection();
                                    connection.setRequestMethod("GET");
                                    connection.setRequestProperty(
                                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                                    );
                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                                    connection.setInstanceFollowRedirects(false);
                                    connection.connect();
                                    String location = connection.getHeaderField("Location");
                                    if (location == null) {
                                        throw new Exception("No Location header in first redirect");
                                    }

                                    location = location.replaceAll(" ", "%20");
                                    connection = (HttpsURLConnection)new URL(location).openConnection();
                                    connection.setRequestMethod("GET");
                                    connection.setRequestProperty(
                                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                                    );
                                    connection.setRequestProperty("Cookie", cookies.toString());
                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                                    connection.setInstanceFollowRedirects(false);
                                    connection.connect();
                                    String location2 = connection.getHeaderField("Location");
                                    if (location2 == null) {
                                        throw new Exception("No Location header in second redirect");
                                    }

                                    connection = (HttpsURLConnection)new URL(location2).openConnection();
                                    connection.setRequestMethod("GET");
                                    connection.setRequestProperty(
                                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                                    );
                                    connection.setRequestProperty("Cookie", cookies.toString());
                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                                    connection.setInstanceFollowRedirects(false);
                                    connection.connect();
                                    String location3 = connection.getHeaderField("Location");
                                    if (location3 == null) {
                                        throw new Exception(
                                            "No Location header in third redirect (Cookie invalid or expired)"
                                        );
                                    }

                                    String accTokenStr = location3.split("accessToken=")[1];
                                    if (accTokenStr.contains("&")) {
                                        accTokenStr = accTokenStr.split("&")[0];
                                    }

                                    String decoded = new String(
                                            Base64.getDecoder().decode(accTokenStr), StandardCharsets.UTF_8
                                        )
                                        .split("\"rp://api.minecraftservices.com/\",")[1];
                                    String token = decoded.split("\"Token\":\"")[1].split("\"")[0];
                                    String uhs = decoded.split(
                                            Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\"")
                                        )[1]
                                        .split("\"")[0];
                                    Map<String, String> result = new HashMap<>();
                                    result.put("Token", token);
                                    result.put("uhs", uhs);
                                    return result;
                                } else {
                                    throw new Exception("No login.live.com cookies found!");
                                }
                            } catch (Exception e) {
                                throw new CompletionException("Failed Sisu Xbox authentication! " + e.getMessage(), e);
                            }
                        },
                        this.executor
                    )
                    .thenComposeAsync(
                        xboxXstsData -> {
                            this.status = "&fAcquiring Minecraft access token&r";
                            return MicrosoftAuth.acquireMCAccessToken(
                                xboxXstsData.get("Token"), xboxXstsData.get("uhs"), this.executor
                            );
                        }
                    )
                    .thenComposeAsync(mcToken -> {
                        this.status = "&fFetching your Minecraft profile&r";
                        accessToken.set(mcToken);
                        return MicrosoftAuth.login(mcToken, this.executor);
                    })
                    .thenAccept(
                        session -> {
                            this.status = null;
                            Account acc = new Account(
                                refreshToken.get(),
                                accessToken.get(),
                                session.func_111285_a(),
                                0L,
                                "42a60a84-599d-44b2-a7c6-b00cdef1d6a2",
                                "XboxLive.signin XboxLive.offline_access",
                                "cookie"
                            );

                            for (Account account : AccountManager.accounts) {
                                if (acc.getUsername().equals(account.getUsername())) {
                                    acc.setUnban(account.getUnban());
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
                        this.status = String.format("&c%s&r", error.getMessage());
                        if (error.getCause() != null) {
                            this.cause = String.format("&c%s&r", error.getCause().getMessage());
                        }

                        this.task = null;
                        return null;
                    });
            }
        }
    }
}
