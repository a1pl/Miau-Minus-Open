package miau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import miau.Miau;
import miau.command.Command;
import miau.enums.ChatColors;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ItemCommand extends Command {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public ItemCommand() {
        super(new ArrayList<>(Arrays.asList("itemname", "item")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        ItemStack stack = mc.field_71439_g.field_71071_by.func_70448_g();
        if (stack != null) {
            String display = stack.func_82833_r().replace('§', '&');
            String registryName = stack.func_77973_b().getRegistryName();
            String compound = stack.func_77942_o() ? stack.func_77978_p().toString().replace('§', '&') : "";
            ChatUtil.sendRaw(
                String.format("%s%s (%s) %s", ChatColors.formatColor(Miau.clientName), display, registryName, compound)
            );
        }
    }
}
