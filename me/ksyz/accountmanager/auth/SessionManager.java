package me.ksyz.accountmanager.auth;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.minecraft.util.Session.Type;

public class SessionManager {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static Field field = null;

    private static Field getField() {
        if (field == null) {
            try {
                for (Field f : Minecraft.class.getDeclaredFields()) {
                    if (f.getType().isAssignableFrom(Session.class)) {
                        field = f;
                        field.setAccessible(true);
                        break;
                    }
                }
            } catch (Exception e) {
                field = null;
            }
        }

        return field;
    }

    public static Session get() {
        return mc.func_110432_I();
    }

    public static Session offline(String username) {
        String uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
        return new Session(username, uuid, "0", Type.LEGACY.toString());
    }

    public static void set(Session session) {
        try {
            getField().set(mc, session);
        } catch (Exception var2) {
        }
    }
}
