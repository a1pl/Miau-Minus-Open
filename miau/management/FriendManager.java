package miau.management;

import java.awt.Color;
import java.io.File;
import miau.Miau;
import miau.enums.ChatColors;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File("./config/Miau/", "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }

    @Override
    public String add(String name) {
        return Miau.targetManager.isFriend(name) ? null : super.add(name);
    }
}
