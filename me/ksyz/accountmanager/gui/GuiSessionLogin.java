package me.ksyz.accountmanager.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import me.ksyz.accountmanager.auth.SessionManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.Session;
import org.apache.commons.io.IOUtils;
import org.lwjgl.input.Keyboard;

public class GuiSessionLogin extends GuiScreen {
    private GuiScreen previousScreen;
    private String status = "Session Login";
    private GuiTextField sessionField;
    private ScaledResolution sr;

    public GuiSessionLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    public void func_73866_w_() {
        Keyboard.enableRepeatEvents(true);
        this.sr = new ScaledResolution(this.field_146297_k);
        this.sessionField = new GuiTextField(
            1, this.field_146297_k.field_71466_p, this.sr.func_78326_a() / 2 - 100, this.sr.func_78328_b() / 2, 200, 20
        );
        this.sessionField.func_146203_f(32767);
        this.sessionField.func_146195_b(true);
        this.field_146292_n
            .add(
                new GuiButton(998, this.sr.func_78326_a() / 2 - 100, this.sr.func_78328_b() / 2 + 30, 200, 20, "Login")
            );
        super.func_73866_w_();
    }

    public void func_146281_b() {
        Keyboard.enableRepeatEvents(false);
        super.func_146281_b();
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.func_146276_q_();
        this.field_146297_k
            .field_71466_p
            .func_78276_b(
                this.status,
                this.sr.func_78326_a() / 2 - this.field_146297_k.field_71466_p.func_78256_a(this.status) / 2,
                this.sr.func_78328_b() / 2 - 30,
                Color.WHITE.getRGB()
            );
        this.sessionField.func_146194_f();
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    protected void func_146284_a(GuiButton button) throws IOException {
        if (button.field_146127_k == 998) {
            try {
                String session = this.sessionField.func_146179_b();
                String username;
                String uuid;
                String token;
                if (session.contains(":")) {
                    username = session.split(":")[0];
                    uuid = session.split(":")[1];
                    token = session.split(":")[2];
                } else {
                    HttpURLConnection c = (HttpURLConnection)new URL(
                            "https://api.minecraftservices.com/minecraft/profile/"
                        )
                        .openConnection();
                    c.setRequestProperty("Content-type", "application/json");
                    c.setRequestProperty("Authorization", "Bearer " + this.sessionField.func_146179_b());
                    c.setDoOutput(true);
                    JsonObject json = new JsonParser().parse(IOUtils.toString(c.getInputStream())).getAsJsonObject();
                    username = json.get("name").getAsString();
                    uuid = json.get("id").getAsString();
                    token = session;
                }

                SessionManager.set(new Session(username, uuid, token, "mojang"));
                this.field_146297_k.func_147108_a(this.previousScreen);
            } catch (IOException IOException) {
                if (IOException.getMessage().contains("401")) {
                    this.status = "§cError: Invalid session.";
                }
            } catch (Exception e) {
                this.status = "§cError: Couldn't set session (check mc logs)";
            }
        }

        super.func_146284_a(button);
    }

    protected void func_73869_a(char typedChar, int keyCode) throws IOException {
        this.sessionField.func_146201_a(typedChar, keyCode);
        if (1 == keyCode) {
            this.field_146297_k.func_147108_a(this.previousScreen);
        } else {
            super.func_73869_a(typedChar, keyCode);
        }
    }
}
