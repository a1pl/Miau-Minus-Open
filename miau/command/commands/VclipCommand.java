package miau.command.commands;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import miau.command.Command;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;

public class VclipCommand extends Command {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    public VclipCommand() {
        super(new ArrayList<>(Collections.singletonList("vclip")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2) {
            double distance = 0.0;

            try {
                distance = Double.parseDouble(args.get(1));
            } catch (NumberFormatException var8) {
            } finally {
                mc.field_71439_g
                    .func_70634_a(
                        mc.field_71439_g.field_70165_t,
                        mc.field_71439_g.field_70163_u + distance,
                        mc.field_71439_g.field_70161_v
                    );
                ChatUtil.display("%sClipped (%s blocks)", df.format(distance));
            }
        } else {
            ChatUtil.display("%sUsage: .%s <&odistance&r>&r", args.get(0).toLowerCase(Locale.ROOT));
        }
    }
}
