package me.ksyz.accountmanager.gui;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.MicrosoftAuth;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.SystemUtils;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.apache.commons.lang3.RandomStringUtils;

public class GuiMicrosoftAuth extends GuiScreen {
    private final GuiScreen previousScreen;
    private final String state;
    private GuiButton openButton = null;
    private boolean openButtonEnabled = true;
    private GuiButton cancelButton = null;
    private String status = null;
    private String cause = null;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;
    private boolean success = false;

    public GuiMicrosoftAuth(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
        this.state = RandomStringUtils.randomAlphanumeric(8);
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        this.field_146292_n
            .add(
                this.openButton = new GuiButton(
                    0,
                    this.field_146294_l / 2 - 75 - 2,
                    this.field_146295_m / 2 + this.field_146289_q.field_78288_b / 2 + this.field_146289_q.field_78288_b,
                    75,
                    20,
                    "Open"
                )
            );
        this.field_146292_n
            .add(
                this.cancelButton = new GuiButton(
                    1,
                    this.field_146294_l / 2 + 2,
                    this.field_146295_m / 2 + this.field_146289_q.field_78288_b / 2 + this.field_146289_q.field_78288_b,
                    75,
                    20,
                    "Cancel"
                )
            );
        if (this.task == null) {
            MicrosoftAuth.CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
            MicrosoftAuth.SCOPE = "XboxLive.signin XboxLive.offline_access";
            URI url = MicrosoftAuth.getMSAuthLink(this.state);
            SystemUtils.setClipboard(url != null ? url.toString() : "");
            this.status = "&fLogin link has been copied to the clipboard!&r";
            if (this.executor == null) {
                this.executor = Executors.newSingleThreadExecutor();
            }

            AtomicReference<String> refreshToken = new AtomicReference<>("");
            AtomicReference<String> accessToken = new AtomicReference<>("");
            this.task = MicrosoftAuth.acquireMSAuthCode(this.state, this.executor)
                .thenComposeAsync(msAuthCode -> {
                    this.openButtonEnabled = false;
                    this.status = "&fAcquiring Microsoft access tokens&r";
                    return MicrosoftAuth.acquireMSAccessTokens(msAuthCode, this.executor);
                })
                .thenComposeAsync(msAccessTokens -> {
                    this.status = "&fAcquiring Xbox access token&r";
                    refreshToken.set(msAccessTokens.get("refresh_token"));
                    return MicrosoftAuth.acquireXboxAccessToken(msAccessTokens.get("access_token"), this.executor);
                })
                .thenComposeAsync(xboxAccessToken -> {
                    this.status = "&fAcquiring Xbox XSTS token&r";
                    return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, this.executor);
                })
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
                            "42a60a84-599d-44b2-a7c6-b00cdef1d6a2",
                            "XboxLive.signin XboxLive.offline_access"
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
                    this.openButtonEnabled = false;
                    this.status = String.format("&c%s&r", error.getMessage());
                    this.cause = String.format("&c%s&r", error.getCause().getMessage());
                    return null;
                });
        }
    }

    public void func_146281_b() {
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
        if (this.openButton != null) {
            this.openButton.field_146124_l = this.openButtonEnabled;
        }

        this.func_146276_q_();
        super.func_73863_a(mouseX, mouseY, partialTicks);
        this.func_73732_a(
            this.field_146289_q,
            "Microsoft Authentication",
            this.field_146294_l / 2,
            this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - this.field_146289_q.field_78288_b * 2,
            11184810
        );
        if (this.status != null) {
            this.func_73732_a(
                this.field_146289_q,
                TextFormatting.translate(this.status),
                this.field_146294_l / 2,
                this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2,
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
                this.field_146289_q,
                TextFormatting.translate(this.cause),
                3,
                this.field_146295_m - 2 - this.field_146289_q.field_78288_b,
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
        if (button != null) {
            if (button.field_146124_l) {
                switch (button.field_146127_k) {
                    case 0:
                        SystemUtils.openWebLink(MicrosoftAuth.getMSAuthLink(this.state));
                        break;
                    case 1:
                        this.field_146297_k.func_147108_a(new GuiAccountManager(this.previousScreen));
                }
            }
        }
    }
}
