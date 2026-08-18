package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import me.ksyz.accountmanager.auth.SessionManager;
import me.ksyz.accountmanager.utils.Notification;
import me.ksyz.accountmanager.utils.TextFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

public class GuiCrackedLogin extends GuiScreen {
    private final GuiScreen previousScreen;
    private String status = "&fEnter a username to add a cracked account.&r";
    private GuiTextField usernameField;

    public GuiCrackedLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        this.field_146292_n.clear();
        Keyboard.enableRepeatEvents(true);
        ScaledResolution sr = new ScaledResolution(this.field_146297_k);
        this.usernameField = new GuiTextField(
            1, this.field_146297_k.field_71466_p, sr.func_78326_a() / 2 - 100, sr.func_78328_b() / 2, 200, 20
        );
        this.usernameField.func_146203_f(16);
        this.usernameField.func_146195_b(true);
        this.field_146292_n
            .add(new GuiButton(998, sr.func_78326_a() / 2 - 100, sr.func_78328_b() / 2 + 30, 200, 20, "Add Cracked"));
    }

    public void func_146281_b() {
        Keyboard.enableRepeatEvents(false);
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.func_146276_q_();
        this.func_73732_a(
            this.field_146289_q,
            "Cracked Account",
            this.field_146294_l / 2,
            this.field_146295_m / 2
                - this.field_146289_q.field_78288_b / 2
                - this.field_146289_q.field_78288_b * 2
                - 14,
            11184810
        );
        this.func_73732_a(
            this.field_146289_q,
            TextFormatting.translate(this.status),
            this.field_146294_l / 2,
            this.field_146295_m / 2 - this.field_146289_q.field_78288_b / 2 - 14,
            -1
        );
        this.usernameField.func_146194_f();
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    protected void func_146284_a(GuiButton button) {
        if (button != null && button.field_146127_k == 998) {
            String username = this.usernameField.func_146179_b().trim();
            if (!username.matches("^\\w{3,16}$")) {
                this.status = "&cInvalid username (3-16 letters, digits or _).&r";
            } else {
                boolean exists = false;

                for (Account account : AccountManager.accounts) {
                    if (account.isCracked() && account.getUsername().equalsIgnoreCase(username)) {
                        username = account.getUsername();
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    AccountManager.accounts.add(Account.cracked(username));
                    AccountManager.save();
                }

                Session session = SessionManager.offline(username);
                SessionManager.set(session);
                this.field_146297_k
                    .func_147108_a(
                        new GuiAccountManager(
                            this.previousScreen,
                            new Notification(
                                TextFormatting.translate(
                                    String.format(
                                        exists
                                            ? "&eCracked account already exists! Logged in as %s&r"
                                            : "&aAdded cracked account! (%s)&r",
                                        session.func_111285_a()
                                    )
                                ),
                                5000L
                            )
                        )
                    );
            }
        }
    }

    protected void func_73869_a(char typedChar, int keyCode) {
        this.usernameField.func_146201_a(typedChar, keyCode);
        if (keyCode == 28) {
            this.func_146284_a((GuiButton)this.field_146292_n.get(0));
        } else if (keyCode == 1) {
            this.field_146297_k.func_147108_a(this.previousScreen);
        }
    }
}
