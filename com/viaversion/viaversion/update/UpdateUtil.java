package com.viaversion.viaversion.update;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParseException;
import com.viaversion.viaversion.util.GsonUtil;
import com.viaversion.viaversion.util.Pair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UpdateUtil {
    private static final String PREFIX = "§a§l[ViaVersion] §a";
    private static final String URL = "https://update.viaversion.com/";
    private static final String PLUGIN = "ViaVersion/";

    public static void sendUpdateMessage(UUID uuid) {
        Via.getPlatform()
            .runAsync(
                () -> {
                    Pair<Level, String> message = getUpdateMessage(false);
                    if (message != null) {
                        Via.getPlatform()
                            .runSync(() -> Via.getPlatform().sendMessage(uuid, "§a§l[ViaVersion] §a" + message.value()));
                    }
                }
            );
    }

    public static void sendUpdateMessage() {
        Via.getPlatform().runAsync(() -> {
            Pair<Level, String> message = getUpdateMessage(true);
            if (message != null) {
                Via.getPlatform().runSync(() -> Via.getPlatform().getLogger().log(message.key(), message.value()));
            }
        });
    }

    private static @Nullable Pair<Level, String> getUpdateMessage(boolean console) {
        if (Via.getPlatform().getPluginVersion().equals("${version}")) {
            return new Pair<>(Level.WARNING, "You are using a debug/custom version, consider updating.");
        }

        String newestString;
        try {
            newestString = getNewestVersion();
        } catch (IOException | JsonParseException ignored) {
            return console ? new Pair<>(Level.WARNING, "Could not check for updates, check your connection.") : null;
        }

        Version current;
        try {
            current = new Version(Via.getPlatform().getPluginVersion());
        } catch (IllegalArgumentException e) {
            return new Pair<>(Level.INFO, "You are using a custom version, consider updating.");
        }

        Version newest = new Version(newestString);
        if (current.compareTo(newest) < 0) {
            return new Pair<>(
                Level.WARNING, "There is a newer plugin version available: " + newest + ", you're on: " + current
            );
        } else if (console && current.compareTo(newest) != 0) {
            String tag = current.getTag().toLowerCase(Locale.ROOT);
            return !tag.endsWith("dev") && !tag.endsWith("snapshot")
                ? new Pair<>(Level.WARNING, "You are running a newer version of the plugin than is released!")
                : new Pair<>(
                    Level.INFO,
                    "You are running a development version of the plugin, please report any bugs to GitHub."
                );
        } else {
            return null;
        }
    }

    private static String getNewestVersion() throws IOException {
        URL url = new URL("https://update.viaversion.com/ViaVersion/");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setUseCaches(false);
        connection.addRequestProperty(
            "User-Agent",
            "ViaVersion " + Via.getPlatform().getPluginVersion() + " " + Via.getPlatform().getPlatformName()
        );
        connection.addRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        StringBuilder builder = new StringBuilder();

        String input;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            while ((input = reader.readLine()) != null) {
                builder.append(input);
            }
        }

        JsonObject statistics = GsonUtil.getGson().fromJson(builder.toString(), JsonObject.class);
        return statistics.get("name").getAsString();
    }
}
