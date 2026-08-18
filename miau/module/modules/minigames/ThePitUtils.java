package miau.module.modules.minigames;

import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.FloatProperty;
import miau.util.client.ChatUtil;
import miau.util.network.PacketUtil;
import miau.util.time.TimerUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.BlockPos;

public class ThePitUtils extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty autoFlashQuestion = new BooleanProperty("autoflashquestion", true);
    public final FloatProperty delay = new FloatProperty(
        "delay", 0.0F, 0.0F, 0.0F, 10.0F, this.autoFlashQuestion::getValue
    );
    public final BooleanProperty autoEgg = new BooleanProperty("autoegg", true);
    public final BooleanProperty debug = new BooleanProperty("debug", false);
    private String pendingAnswer = null;
    private long answerTime = 0L;
    private final TimerUtil eggTimer = new TimerUtil();

    public ThePitUtils() {
        super("ThePitUtils", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.pendingAnswer != null && System.currentTimeMillis() >= this.answerTime) {
            mc.field_71439_g.func_71165_d(this.pendingAnswer);
            if (this.debug.getValue()) {
                ChatUtil.display("&a[ThePitUtils] Answered: &f" + this.pendingAnswer);
            }

            this.pendingAnswer = null;
        }

        if (this.autoEgg.getValue() && mc.field_71439_g != null && mc.field_71441_e != null && mc.field_71442_b != null
            )
         {
            float reach = mc.field_71442_b.func_78757_d();
            int range = (int)Math.ceil(reach);

            for (int x = -range; x <= range; x++) {
                for (int y = -range; y <= range; y++) {
                    for (int z = -range; z <= range; z++) {
                        BlockPos pos = new BlockPos(
                            mc.field_71439_g.field_70165_t + x,
                            mc.field_71439_g.field_70163_u + y,
                            mc.field_71439_g.field_70161_v + z
                        );
                        Block block = mc.field_71441_e.func_180495_p(pos).func_177230_c();
                        if (block == Blocks.field_150380_bt) {
                            double distSq = mc.field_71439_g.func_174831_c(pos);
                            if (distSq <= reach * reach) {
                                PacketUtil.sendPacket(
                                    new C08PacketPlayerBlockPlacement(
                                        pos, 1, mc.field_71439_g.func_70694_bm(), 0.5F, 0.5F, 0.5F
                                    )
                                );
                                mc.field_71439_g.func_71038_i();
                                if (this.debug.getValue()) {
                                    ChatUtil.display(
                                        "&a[ThePitUtils] Clicked Dragon Egg at "
                                            + pos.func_177958_n()
                                            + ", "
                                            + pos.func_177956_o()
                                            + ", "
                                            + pos.func_177952_p()
                                    );
                                }

                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S02PacketChat) {
                S02PacketChat chatPacket = (S02PacketChat)packet;
                String unformattedText = chatPacket.func_148915_c().func_150260_c();
                if (this.autoFlashQuestion.getValue() && unformattedText.contains("Nhanh như chớp! Giải: ")) {
                    String equation = unformattedText.substring(unformattedText.indexOf("Giải: ") + 6).trim();

                    try {
                        double result = eval(equation);
                        String answerStr = result == (long)result
                            ? String.valueOf((long)result)
                            : String.valueOf(result);
                        float minDelay = this.delay.getValue();
                        float maxDelay = this.delay.getSecondValue();
                        float delaySecs = minDelay + (float)Math.random() * (maxDelay - minDelay);
                        long delayMs = (long)(delaySecs * 1000.0F);
                        if (delayMs > 0L) {
                            this.pendingAnswer = answerStr;
                            this.answerTime = System.currentTimeMillis() + delayMs;
                        } else {
                            mc.field_71439_g.func_71165_d(answerStr);
                            if (this.debug.getValue()) {
                                ChatUtil.display("&a[ThePitUtils] Answered: &f" + answerStr);
                            }
                        }
                    } catch (Exception e) {
                        if (this.debug.getValue()) {
                            ChatUtil.display("&c[ThePitUtils] Failed to parse equation: " + equation);
                        }
                    }
                }
            }
        }
    }

    private static double eval(String str) {
        return (new Object() {
            int pos = -1;
            int ch;

            void nextChar() {
                this.ch = ++this.pos < str.length() ? str.charAt(this.pos) : -1;
            }

            boolean eat(int charToEat) {
                while (this.ch == 32) {
                    this.nextChar();
                }

                if (this.ch == charToEat) {
                    this.nextChar();
                    return true;
                } else {
                    return false;
                }
            }

            double parse() {
                this.nextChar();
                double x = this.parseExpression();
                if (this.pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char)this.ch);
                } else {
                    return x;
                }
            }

            double parseExpression() {
                double x = this.parseTerm();

                while (true) {
                    while (!this.eat(43)) {
                        if (!this.eat(45)) {
                            return x;
                        }

                        x -= this.parseTerm();
                    }

                    x += this.parseTerm();
                }
            }

            double parseTerm() {
                double x = this.parseFactor();

                while (true) {
                    while (this.eat(42) || this.eat(120) || this.eat(88)) {
                        x *= this.parseFactor();
                    }

                    if (!this.eat(47)) {
                        return x;
                    }

                    x /= this.parseFactor();
                }
            }

            double parseFactor() {
                if (this.eat(43)) {
                    return this.parseFactor();
                }

                if (this.eat(45)) {
                    return -this.parseFactor();
                }

                int startPos = this.pos;
                double x;
                if (this.eat(40)) {
                    x = this.parseExpression();
                    this.eat(41);
                } else {
                    if ((this.ch < 48 || this.ch > 57) && this.ch != 46) {
                        throw new RuntimeException("Unexpected: " + (char)this.ch);
                    }

                    while (this.ch >= 48 && this.ch <= 57 || this.ch == 46) {
                        this.nextChar();
                    }

                    x = Double.parseDouble(str.substring(startPos, this.pos));
                }

                return x;
            }
        }).parse();
    }
}
