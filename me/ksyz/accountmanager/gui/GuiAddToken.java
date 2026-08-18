package me.ksyz.accountmanager.gui;

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
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

public class GuiAddToken extends GuiScreen {
    private final GuiScreen previousScreen;
    private GuiButton openButton = null;
    private boolean openButtonEnabled = true;
    private String status = null;
    private String cause = null;
    private ExecutorService executor = null;
    private CompletableFuture<Void> task = null;
    private boolean success = false;
    private GuiTextField tokenField;

    public GuiAddToken(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        Keyboard.enableRepeatEvents(true);
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        this.tokenField = new GuiTextField(
            1, this.field_146297_k.field_71466_p, sr.func_78326_a() / 2 - 100, sr.func_78328_b() / 2, 200, 20
        );
        this.tokenField.func_146203_f(32767);
        this.tokenField.func_146195_b(true);
        this.field_146292_n
            .add(new GuiButton(998, sr.func_78326_a() / 2 - 100, sr.func_78328_b() / 2 + 30, 200, 20, "Add"));
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
            "Add Token",
            this.field_146294_l / 2,
            this.field_146295_m / 2
                - this.field_146289_q.field_78288_b / 2
                - this.field_146289_q.field_78288_b * 2
                - 14,
            11184810
        );
        this.tokenField.func_146194_f();
        if (this.status != null) {
            this.func_73732_a(
                this.field_146289_q,
                TextFormatting.translate(this.status),
                this.field_146294_l / 2,
                this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - 14,
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
        this.tokenField.func_146201_a(typedChar, keyCode);
        if (keyCode == 1
            && (
                this.task == null
                    || this.task.isDone()
                    || this.task.isCancelled()
                    || this.task.isCompletedExceptionally()
            )) {
            this.field_146297_k.func_147108_a(this.previousScreen);
        }
    }

    protected void func_146284_a(GuiButton button) {
        if (button != null) {
            if (button.field_146124_l && button.field_146127_k == 998 && this.task == null) {
                if (this.executor == null) {
                    this.executor = Executors.newSingleThreadExecutor();
                }

                AtomicReference<String> refreshToken = new AtomicReference<>("");
                AtomicReference<String> accessToken = new AtomicReference<>("");
                MicrosoftAuth.CLIENT_ID = "00000000402b5328";
                MicrosoftAuth.SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
                this.task = MicrosoftAuth.login(this.tokenField.func_146179_b(), this.executor)
                    .handle((session, error) -> session != null)
                    .thenComposeAsync(completed -> {
                        if (completed) {
                            throw new NoSuchElementException();
                        }

                        this.status = "&7Refreshing Microsoft access tokens...&r";
                        return MicrosoftAuth.refreshMSAccessTokens(this.tokenField.func_146179_b(), this.executor);
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
                                "00000000402b5328",
                                "service::user.auth.xboxlive.com::MBI_SSL"
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
                        this.task.cancel(true);
                        this.task = null;
                        return null;
                    });
            }
        }
    }
}
