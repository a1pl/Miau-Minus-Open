package de.florianmichael.viamcp.gui;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import de.florianmichael.viamcp.protocolinfo.ProtocolInfo;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;

public class GuiProtocolSelector extends GuiScreen {
    private final GuiScreen parent;
    public GuiProtocolSelector.SlotList list;

    public GuiProtocolSelector(GuiScreen parent) {
        this.parent = parent;
    }

    public void func_73866_w_() {
        super.func_73866_w_();
        this.field_146292_n
            .add(new GuiButton(1, this.field_146294_l / 2 - 100, this.field_146295_m - 25, 200, 20, "Back"));
        this.list = new GuiProtocolSelector.SlotList(
            this.field_146297_k, this.field_146294_l, this.field_146295_m, 32, this.field_146295_m - 32
        );
    }

    protected void func_146284_a(GuiButton guiButton) throws IOException {
        this.list.func_148147_a(guiButton);
        if (guiButton.field_146127_k == 1) {
            this.field_146297_k.func_147108_a(this.parent);
        }
    }

    public void func_146274_d() throws IOException {
        this.list.func_178039_p();
        super.func_146274_d();
    }

    public void func_73863_a(int mouseX, int mouseY, float partialTicks) {
        this.list.func_148128_a(mouseX, mouseY, partialTicks);
        GlStateManager.func_179094_E();
        GlStateManager.func_179139_a(2.0, 2.0, 2.0);
        String title = EnumChatFormatting.BOLD + "ViaMCP";
        this.func_73731_b(
            this.field_146289_q, title, (this.field_146294_l - this.field_146289_q.func_78256_a(title) * 2) / 4, 5, -1
        );
        GlStateManager.func_179121_F();
        this.func_73731_b(this.field_146289_q, "by EnZaXD/Flori2007", 1, 1, -1);
        this.func_73731_b(this.field_146289_q, "Discord: EnZaXD#6257", 1, 11, -1);
        ProtocolInfo protocolInfo = ProtocolInfo.fromProtocolVersion(ViaLoadingBase.getInstance().getTargetVersion());
        String versionTitle = "Version: "
            + ViaLoadingBase.getInstance().getTargetVersion().getName()
            + " - "
            + protocolInfo.getName();
        String versionReleased = "Released: " + protocolInfo.getReleaseDate();
        int fixedHeight = (5 + this.field_146289_q.field_78288_b) * 2 + 2;
        this.func_73731_b(
            this.field_146289_q,
            "" + EnumChatFormatting.GRAY + EnumChatFormatting.BOLD + "Version Information",
            (this.field_146294_l - this.field_146289_q.func_78256_a("Version Information")) / 2,
            fixedHeight,
            -1
        );
        this.func_73731_b(
            this.field_146289_q,
            versionTitle,
            (this.field_146294_l - this.field_146289_q.func_78256_a(versionTitle)) / 2,
            fixedHeight + this.field_146289_q.field_78288_b,
            -1
        );
        this.func_73731_b(
            this.field_146289_q,
            versionReleased,
            (this.field_146294_l - this.field_146289_q.func_78256_a(versionReleased)) / 2,
            fixedHeight + this.field_146289_q.field_78288_b * 2,
            -1
        );
        super.func_73863_a(mouseX, mouseY, partialTicks);
    }

    class SlotList extends GuiSlot {
        public SlotList(Minecraft mc, int width, int height, int top, int bottom) {
            super(mc, width, height, top + 30, bottom, 18);
        }

        protected int func_148127_b() {
            return ViaLoadingBase.getProtocols().size();
        }

        protected void func_148144_a(int i, boolean b, int i1, int i2) {
            ProtocolVersion protocolVersion = ViaLoadingBase.getProtocols().get(i);
            ViaLoadingBase.getInstance().reload(protocolVersion);
        }

        protected boolean func_148131_a(int i) {
            return false;
        }

        protected void func_148123_a() {
            GuiProtocolSelector.this.func_146276_q_();
        }

        protected void func_180791_a(int i, int i1, int i2, int i3, int i4, int i5) {
            GuiProtocolSelector.this.func_73732_a(
                this.field_148161_k.field_71466_p,
                (
                        ViaLoadingBase.PROTOCOLS.indexOf(ViaLoadingBase.getInstance().getTargetVersion()) == i
                            ? EnumChatFormatting.GREEN.toString() + EnumChatFormatting.BOLD
                            : EnumChatFormatting.GRAY.toString()
                    )
                    + ViaLoadingBase.getProtocols().get(i).getName(),
                this.field_148155_a / 2,
                i2 + 2,
                -1
            );
            GlStateManager.func_179094_E();
            GlStateManager.func_179139_a(0.5, 0.5, 0.5);
            GuiProtocolSelector.this.func_73732_a(
                this.field_148161_k.field_71466_p,
                "PVN: " + ViaLoadingBase.getProtocols().get(i).getVersion(),
                this.field_148155_a,
                (i2 + 2) * 2 + 20,
                -1
            );
            GlStateManager.func_179121_F();
        }
    }
}
