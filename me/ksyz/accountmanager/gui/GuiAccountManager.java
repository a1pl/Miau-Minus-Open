package me.ksyz.accountmanager.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;

public class GuiAccountManager extends GuiScreen {
    private final GuiScreen previousScreen;
    private GuiButton loginButton = null;
    private GuiButton deleteButton = null;
    private GuiButton cancelButton = null;
    private GuiAccountManager.GuiAccountList guiAccountList = null;
    private Notification notification = null;
    private int selectedAccount = -1;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;

    public GuiAccountManager(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public GuiAccountManager(GuiScreen previousScreen, Notification notification) {
        this.previousScreen = previousScreen;
        this.notification = notification;
    }

    public void func_73866_w_() {
        AccountManager.load();
        Keyboard.enableRepeatEvents(true);
        int bw = 72;
        int gap = 3;
        int x0 = this.field_146294_l / 2 - 148;
        int c0 = x0;
        int c1 = x0 + 75;
        int c2 = x0 + 150;
        int c3 = x0 + 225;
        int row1 = this.field_146295_m - 52;
        int row2 = this.field_146295_m - 28;
        this.field_146292_n.add(this.loginButton = new GuiButton(0, c0, row1, 72, 20, "Login"));
        this.field_146292_n.add(new GuiButton(1, c1, row1, 72, 20, "Add"));
        this.field_146292_n.add(new GuiButton(5, c2, row1, 72, 20, "Add Token"));
        this.field_146292_n.add(new GuiButton(4, c3, row1, 72, 20, "Session"));
        this.field_146292_n.add(this.deleteButton = new GuiButton(2, c0, row2, 72, 20, "Delete"));
        this.field_146292_n.add(new GuiButton(6, c1, row2, 72, 20, "Cracked"));
        this.field_146292_n.add(new GuiButton(7, c2, row2, 72, 20, "Cookie"));
        this.field_146292_n.add(this.cancelButton = new GuiButton(3, c3, row2, 72, 20, "Cancel"));
        this.guiAccountList = new GuiAccountManager.GuiAccountList(this.field_146297_k);
        this.guiAccountList.func_148134_d(11, 12);
        this.func_73876_c();
    }

    public void func_146281_b() {
        Keyboard.enableRepeatEvents(false);
        if (this.task != null && !this.task.isDone()) {
            this.task.cancel(true);
            this.executor.shutdownNow();
        }
    }

    public void func_73876_c() {
        if (this.loginButton != null && this.deleteButton != null) {
            this.loginButton.field_146124_l = this.deleteButton.field_146124_l = this.selectedAccount >= 0;
            if (this.task != null && !this.task.isDone()) {
                this.loginButton.field_146124_l = false;
            }
        }
    }

    public void func_73863_a(int mouseX, int mouseY, float renderPartialTicks) {
        if (this.guiAccountList != null) {
            this.guiAccountList.func_148128_a(mouseX, mouseY, renderPartialTicks);
        }

        super.func_73863_a(mouseX, mouseY, renderPartialTicks);
        this.func_73732_a(
            this.field_146289_q,
            TextFormatting.translate(String.format("&rAccount Manager &8(&7%s&8)&r", AccountManager.accounts.size())),
            this.field_146294_l / 2,
            20,
            -1
        );
        String text = TextFormatting.translate(
            String.format("&7Username: &3%s&r", SessionManager.get().func_111285_a())
        );
        this.field_146297_k.field_71462_r.func_73731_b(this.field_146297_k.field_71466_p, text, 3, 3, -1);
        if (this.notification != null && !this.notification.isExpired()) {
            String notificationText = this.notification.getMessage();
            Gui.func_73734_a(
                this.field_146297_k.field_71462_r.field_146294_l / 2
                    - this.field_146297_k.field_71466_p.func_78256_a(notificationText) / 2
                    - 3,
                4,
                this.field_146297_k.field_71462_r.field_146294_l / 2
                    + this.field_146297_k.field_71466_p.func_78256_a(notificationText) / 2
                    + 3,
                7 + this.field_146297_k.field_71466_p.field_78288_b + 2,
                1677721600
            );
            this.field_146297_k
                .field_71462_r
                .func_73732_a(
                    this.field_146297_k.field_71466_p,
                    this.notification.getMessage(),
                    this.field_146297_k.field_71462_r.field_146294_l / 2,
                    7,
                    -1
                );
        }
    }

    public void func_146274_d() throws IOException {
        if (this.guiAccountList != null) {
            this.guiAccountList.func_178039_p();
        }

        super.func_146274_d();
    }

    protected void func_73869_a(char typedChar, int keyCode) {
        switch (keyCode) {
            case 1:
                this.func_146284_a(this.cancelButton);
                break;
            case 28:
                this.func_146284_a(this.loginButton);
                break;
            case 200:
                if (this.selectedAccount > 0) {
                    this.selectedAccount--;
                    if (func_146271_m()) {
                        Collections.swap(AccountManager.accounts, this.selectedAccount, this.selectedAccount + 1);
                        AccountManager.save();
                    }
                }
                break;
            case 208:
                if (this.selectedAccount < AccountManager.accounts.size() - 1) {
                    this.selectedAccount++;
                    if (func_146271_m()) {
                        Collections.swap(AccountManager.accounts, this.selectedAccount, this.selectedAccount - 1);
                        AccountManager.save();
                    }
                }
                break;
            case 211:
                this.func_146284_a(this.deleteButton);
        }

        if (func_175280_f(keyCode) && this.selectedAccount >= 0) {
            func_146275_d(AccountManager.accounts.get(this.selectedAccount).getUsername());
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button != null) {
            if (button.field_146124_l) {
                switch (button.field_146127_k) {
                    case 0:
                        if (this.task == null || this.task.isDone()) {
                            Account account = AccountManager.accounts.get(this.selectedAccount);
                            if (account.isCracked()) {
                                SessionManager.set(SessionManager.offline(account.getUsername()));
                                this.notification = new Notification(
                                    TextFormatting.translate(
                                        String.format("&aSuccessful login! (%s)&r", account.getUsername())
                                    ),
                                    5000L
                                );
                            } else {
                                if (this.executor == null) {
                                    this.executor = Executors.newSingleThreadExecutor();
                                }

                                String username = StringUtils.isBlank(account.getUsername())
                                    ? "???"
                                    : account.getUsername();
                                AtomicReference<String> refreshToken = new AtomicReference<>("");
                                AtomicReference<String> accessToken = new AtomicReference<>("");
                                this.notification = new Notification(
                                    TextFormatting.translate(
                                        String.format("&7Fetching your Minecraft profile... (%s)&r", username)
                                    ),
                                    -1L
                                );
                                MicrosoftAuth.CLIENT_ID = account.getClientId();
                                MicrosoftAuth.SCOPE = account.getScope();
                                this.task = MicrosoftAuth.login(account.getAccessToken(), this.executor)
                                    .handle(
                                        (session, error) -> {
                                            if (session != null) {
                                                account.setUsername(session.func_111285_a());
                                                AccountManager.save();
                                                SessionManager.set(session);
                                                this.notification = new Notification(
                                                    TextFormatting.translate(
                                                        String.format(
                                                            "&aSuccessful login! (%s)&r", account.getUsername()
                                                        )
                                                    ),
                                                    5000L
                                                );
                                                return true;
                                            } else {
                                                return false;
                                            }
                                        }
                                    )
                                    .thenComposeAsync(
                                        completed -> {
                                            if (completed) {
                                                throw new NoSuchElementException();
                                            }

                                            this.notification = new Notification(
                                                TextFormatting.translate(
                                                    String.format(
                                                        "&7Refreshing Microsoft access tokens... (%s)&r", username
                                                    )
                                                ),
                                                -1L
                                            );
                                            return MicrosoftAuth.refreshMSAccessTokens(
                                                account.getRefreshToken(), this.executor
                                            );
                                        }
                                    )
                                    .thenComposeAsync(
                                        msAccessTokens -> {
                                            this.notification = new Notification(
                                                TextFormatting.translate(
                                                    String.format("&7Acquiring Xbox access token... (%s)&r", username)
                                                ),
                                                -1L
                                            );
                                            refreshToken.set(msAccessTokens.get("refresh_token"));
                                            return MicrosoftAuth.acquireXboxAccessToken(
                                                msAccessTokens.get("access_token"), this.executor
                                            );
                                        }
                                    )
                                    .thenComposeAsync(
                                        xboxAccessToken -> {
                                            this.notification = new Notification(
                                                TextFormatting.translate(
                                                    String.format("&7Acquiring Xbox XSTS token... (%s)&r", username)
                                                ),
                                                -1L
                                            );
                                            return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, this.executor);
                                        }
                                    )
                                    .thenComposeAsync(
                                        xboxXstsData -> {
                                            this.notification = new Notification(
                                                TextFormatting.translate(
                                                    String.format(
                                                        "&7Acquiring Minecraft access token... (%s)&r", username
                                                    )
                                                ),
                                                -1L
                                            );
                                            return MicrosoftAuth.acquireMCAccessToken(
                                                xboxXstsData.get("Token"), xboxXstsData.get("uhs"), this.executor
                                            );
                                        }
                                    )
                                    .thenComposeAsync(
                                        mcToken -> {
                                            this.notification = new Notification(
                                                TextFormatting.translate(
                                                    String.format(
                                                        "&7Fetching your Minecraft profile... (%s)&r", username
                                                    )
                                                ),
                                                -1L
                                            );
                                            accessToken.set(mcToken);
                                            return MicrosoftAuth.login(mcToken, this.executor);
                                        }
                                    )
                                    .thenAccept(
                                        session -> {
                                            account.setRefreshToken(refreshToken.get());
                                            account.setAccessToken(accessToken.get());
                                            account.setUsername(session.func_111285_a());
                                            AccountManager.save();
                                            SessionManager.set(session);
                                            this.notification = new Notification(
                                                TextFormatting.translate(
                                                    String.format("&aSuccessful login! (%s)&r", account.getUsername())
                                                ),
                                                5000L
                                            );
                                        }
                                    )
                                    .exceptionally(
                                        error -> {
                                            if (!(error.getCause() instanceof NoSuchElementException)) {
                                                this.notification = new Notification(
                                                    TextFormatting.translate(
                                                        String.format("&c%s (%s)&r", error.getMessage(), username)
                                                    ),
                                                    5000L
                                                );
                                            }

                                            return null;
                                        }
                                    );
                            }
                        }
                        break;
                    case 1:
                        this.field_146297_k.func_147108_a(new GuiMicrosoftFaiths(this));
                        break;
                    case 2:
                        if (this.selectedAccount > -1 && this.selectedAccount < AccountManager.accounts.size()) {
                            AccountManager.accounts.remove(this.selectedAccount);
                            AccountManager.save();
                            this.selectedAccount = -1;
                            this.func_73876_c();
                        }
                        break;
                    case 3:
                        this.field_146297_k.func_147108_a(this.previousScreen);
                        break;
                    case 4:
                        this.field_146297_k.func_147108_a(new GuiSessionLogin(this));
                        break;
                    case 5:
                        this.field_146297_k.func_147108_a(new GuiAddToken(this));
                        break;
                    case 6:
                        this.field_146297_k.func_147108_a(new GuiCrackedLogin(this));
                        break;
                    case 7:
                        this.field_146297_k.func_147108_a(new GuiCookieLogin(this));
                        break;
                    default:
                        this.guiAccountList.func_148147_a(button);
                }
            }
        }
    }

    class GuiAccountList extends GuiSlot {
        public GuiAccountList(Minecraft mc) {
            super(
                mc,
                GuiAccountManager.this.field_146294_l,
                GuiAccountManager.this.field_146295_m,
                32,
                GuiAccountManager.this.field_146295_m - 64,
                16
            );
        }

        protected int func_148127_b() {
            return AccountManager.accounts.size();
        }

        protected boolean func_148131_a(int slotIndex) {
            return slotIndex == GuiAccountManager.this.selectedAccount;
        }

        protected int func_148137_d() {
            return (this.field_148155_a + this.func_148139_c()) / 2 + 2;
        }

        public int func_148139_c() {
            return 308;
        }

        protected int func_148138_e() {
            return AccountManager.accounts.size() * 16;
        }

        protected void func_148144_a(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            GuiAccountManager.this.selectedAccount = slotIndex;
            GuiAccountManager.this.func_73876_c();
            if (isDoubleClick) {
                GuiAccountManager.this.func_146284_a(GuiAccountManager.this.loginButton);
            }
        }

        protected void func_148123_a() {
            GuiAccountManager.this.func_146276_q_();
        }

        protected void func_180791_a(int entryID, int x, int y, int k, int mouseXIn, int mouseYIn) {
            FontRenderer fr = GuiAccountManager.this.field_146289_q;
            Account account = AccountManager.accounts.get(entryID);
            String username = account.getUsername();
            if (StringUtils.isBlank(username)) {
                username = "&7&l?";
            } else if (account.getAccessToken().equals(SessionManager.get().func_148254_d())) {
                username = String.format("&a&l%s", username);
            } else if (username.equals(SessionManager.get().func_111285_a())) {
                username = String.format("&a%s", username);
            }

            username = TextFormatting.translate(String.format("&r%s&r", username));
            GuiAccountManager.this.func_73731_b(fr, username, x + 2, y + 2, -1);
            String tag = TextFormatting.translate(account.isCracked() ? "&8[&cCracked&8]" : "&8[&bPre&8]");
            GuiAccountManager.this.func_73731_b(fr, tag, x + 2 + fr.func_78256_a(username) + 4, y + 2, -1);
            if (!account.isCracked()) {
                long currentTime = System.currentTimeMillis();
                long unbanTime = account.getUnban();
                String unban;
                if (unbanTime < 0L) {
                    unban = "&4&l⚠";
                } else if (unbanTime <= currentTime) {
                    unban = "&2&l✔";
                } else {
                    long diff = unbanTime - currentTime;
                    long s = diff / 1000L % 60L;
                    long m = diff / 60000L % 60L;
                    long h = diff / 3600000L % 24L;
                    long d = diff / 86400000L;
                    unban = String.format(
                        "%s%s%s%s",
                        d > 0L ? String.format("%dd", d) : "",
                        h > 0L ? String.format(" %dh", h) : "",
                        m > 0L ? String.format(" %dm", m) : "",
                        s > 0L ? String.format(" %ds", s) : ""
                    );
                    unban = unban.trim();
                    unban = String.format("%s &c&l⚠", unban);
                }

                unban = TextFormatting.translate(String.format("&r%s&r", unban));
                GuiAccountManager.this.func_73731_b(
                    fr, unban, x + this.func_148139_c() - 5 - fr.func_78256_a(unban), y + 2, -1
                );
            }
        }
    }
}
