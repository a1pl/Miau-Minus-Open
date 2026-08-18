package de.florianmichael.viamcp.gui;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;

public class AsyncVersionSlider extends GuiButton {
    private float dragValue;
    private final List<ProtocolVersion> values = new ArrayList<>(ViaLoadingBase.PROTOCOLS);
    private float sliderValue;
    public boolean dragging;

    public AsyncVersionSlider(int buttonId, int x, int y, int widthIn, int heightIn) {
        super(buttonId, x, y, Math.max(widthIn, 110), heightIn, "");
        Collections.reverse(this.values);
        this.dragValue = (float)this.values.indexOf(ViaLoadingBase.getInstance().getTargetVersion())
            / (this.values.size() - 1);
        this.sliderValue = this.dragValue;
        this.field_146126_j = this.values.get((int)(this.sliderValue * (this.values.size() - 1))).getName();
    }

    public void func_146112_a(Minecraft mc, int mouseX, int mouseY) {
        super.func_146112_a(mc, mouseX, mouseY);
    }

    protected int func_146114_a(boolean mouseOver) {
        return 0;
    }

    protected void func_146119_b(Minecraft mc, int mouseX, int mouseY) {
        if (this.field_146125_m) {
            if (this.dragging) {
                this.sliderValue = (float)(mouseX - (this.field_146128_h + 4)) / (this.field_146120_f - 8);
                this.sliderValue = MathHelper.func_76131_a(this.sliderValue, 0.0F, 1.0F);
                this.dragValue = this.sliderValue;
                this.field_146126_j = this.values.get((int)(this.sliderValue * (this.values.size() - 1))).getName();
                ViaLoadingBase.getInstance()
                    .reload(this.values.get((int)(this.sliderValue * (this.values.size() - 1))));
            }

            mc.func_110434_K().func_110577_a(field_146122_a);
            GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
            this.func_73729_b(
                this.field_146128_h + (int)(this.sliderValue * (this.field_146120_f - 8)),
                this.field_146129_i,
                0,
                66,
                4,
                20
            );
            this.func_73729_b(
                this.field_146128_h + (int)(this.sliderValue * (this.field_146120_f - 8)) + 4,
                this.field_146129_i,
                196,
                66,
                4,
                20
            );
        }
    }

    public boolean func_146116_c(Minecraft mc, int mouseX, int mouseY) {
        if (super.func_146116_c(mc, mouseX, mouseY)) {
            this.sliderValue = (float)(mouseX - (this.field_146128_h + 4)) / (this.field_146120_f - 8);
            this.sliderValue = MathHelper.func_76131_a(this.sliderValue, 0.0F, 1.0F);
            this.dragValue = this.sliderValue;
            this.field_146126_j = this.values.get((int)(this.sliderValue * (this.values.size() - 1))).getName();
            ViaLoadingBase.getInstance().reload(this.values.get((int)(this.sliderValue * (this.values.size() - 1))));
            this.dragging = true;
            return true;
        } else {
            return false;
        }
    }

    public void func_146118_a(int mouseX, int mouseY) {
        this.dragging = false;
    }

    public void setVersion(int protocol) {
        this.dragValue = (float)this.values.indexOf(ProtocolVersion.getProtocol(protocol)) / (this.values.size() - 1);
        this.sliderValue = this.dragValue;
        this.field_146126_j = this.values.get((int)(this.sliderValue * (this.values.size() - 1))).getName();
    }
}
