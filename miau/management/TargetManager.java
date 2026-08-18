package miau.management;

import java.awt.Color;
import java.io.File;
import miau.Miau;
import miau.enums.ChatColors;

public class TargetManager extends PlayerFileManager {
    public TargetManager() {
        super(new File("./config/Miau/", "enemies.txt"), new Color(ChatColors.DARK_RED.toAwtColor()));
    }

    @Override
    public String add(String name) {
        return Miau.friendManager.isFriend(name) ? null : super.add(name);
    }
}
