package miau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import miau.Miau;
import miau.command.Command;
import miau.config.online.OnlineConfigApplier;
import miau.config.online.OnlineConfigClient;
import miau.config.online.OnlineConfigEntry;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

public class UserConfigCommand extends Command {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Miau UserConfig");
        thread.setDaemon(true);
        return thread;
    });
    private final OnlineConfigClient client = new OnlineConfigClient();
    private volatile List<OnlineConfigEntry> cache = Collections.emptyList();

    public UserConfigCommand() {
        super(new ArrayList<>(Arrays.asList("userconfig", "usercfg", "ucfg")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            this.usage();
        } else {
            String sub = args.get(1).toLowerCase(Locale.ROOT);
            if (sub.equals("list") || sub.equals("l")) {
                this.async(this::listConfigs);
            } else if (sub.equals("load")) {
                if (args.size() < 3) {
                    ChatUtil.display(Miau.clientName + "Missing user config id/name&r");
                    return;
                }

                this.async(() -> this.loadConfig(String.join(" ", args.subList(2, args.size()))));
            } else if (sub.equals("info")) {
                if (args.size() < 3) {
                    ChatUtil.display(Miau.clientName + "Missing user config id/name&r");
                    return;
                }

                this.async(() -> this.showInfo(String.join(" ", args.subList(2, args.size()))));
            } else {
                this.usage();
            }
        }
    }

    private void listConfigs() {
        try {
            List<OnlineConfigEntry> entries = Collections.unmodifiableList(
                new ArrayList<>(this.client.listUserConfigs())
            );
            this.runOnClientThread(() -> {
                this.cache = entries;
                ChatUtil.display(Miau.clientName + (entries.isEmpty() ? "No user configs found&r" : "User configs:&r"));

                for (OnlineConfigEntry entry : entries) {
                    this.sendEntry(entry);
                }
            });
        } catch (Exception e) {
            this.runOnClientThread(
                () -> ChatUtil.sendFormatted(
                    Miau.clientName + "Failed to list user configs: &c" + e.getMessage() + "&r"
                )
            );
        }
    }

    private void loadConfig(String input) {
        try {
            OnlineConfigEntry entry = this.findEntry(input);
            if (entry == null) {
                this.runOnClientThread(
                    () -> ChatUtil.display(Miau.clientName + "User config not found (&o" + input + "&r)&r")
                );
                return;
            }

            String json = this.client.loadUserConfig(entry.getId());
            this.runOnClientThread(() -> this.applyConfig(entry, json));
        } catch (Exception e) {
            this.runOnClientThread(
                () -> ChatUtil.sendFormatted(Miau.clientName + "Failed to load user config: &c" + e.getMessage() + "&r")
            );
        }
    }

    private void showInfo(String input) {
        try {
            OnlineConfigEntry entry = this.findEntry(input);
            this.runOnClientThread(() -> {
                if (entry == null) {
                    ChatUtil.display(Miau.clientName + "User config not found (&o" + input + "&r)&r");
                } else {
                    this.showMetadata(entry);
                }
            });
        } catch (Exception e) {
            this.runOnClientThread(
                () -> ChatUtil.sendFormatted(
                    Miau.clientName + "Failed to get user config info: &c" + e.getMessage() + "&r"
                )
            );
        }
    }

    private OnlineConfigEntry findEntry(String input) throws Exception {
        List<OnlineConfigEntry> entries = this.cache;
        if (entries.isEmpty()) {
            entries = Collections.unmodifiableList(new ArrayList<>(this.client.listUserConfigs()));
            this.cache = entries;
        }

        for (OnlineConfigEntry entry : entries) {
            if (entry.getId().equalsIgnoreCase(input) || entry.getName().equalsIgnoreCase(input)) {
                return entry;
            }
        }

        return null;
    }

    private void applyConfig(OnlineConfigEntry entry, String json) {
        try {
            this.showMetadata(entry);
            int applied = new OnlineConfigApplier().apply(json);
            ChatUtil.display("%sUser config loaded (&a&o%s&r) &7- applied %d setting(s)&r", entry.getName(), applied);
        } catch (Exception e) {
            ChatUtil.display(Miau.clientName + "Failed to load user config: &c" + e.getMessage() + "&r");
        }
    }

    private void sendEntry(OnlineConfigEntry entry) {
        String command = ".userconfig load " + entry.getId();
        String version = entry.getVersion().isEmpty() ? "" : " §7v§e" + entry.getVersion();
        String line = String.format(
            "§7» §f%s%s §7[§b%s§7] §7by §a%s", entry.getName(), version, entry.getId(), entry.getAuthor()
        );
        ChatUtil.send(
            new ChatComponentText(line)
                .func_150255_a(
                    new ChatStyle()
                        .func_150241_a(new ClickEvent(Action.RUN_COMMAND, command))
                        .func_150209_a(
                            new HoverEvent(
                                net.minecraft.event.HoverEvent.Action.SHOW_TEXT,
                                new ChatComponentText(
                                    command + "\n§7id: §f" + entry.getId() + "\n§7loads: §f" + entry.getLoadCount()
                                )
                            )
                        )
                )
        );
    }

    private void showMetadata(OnlineConfigEntry entry) {
        ChatUtil.display(Miau.clientName + "User config info:&r");
        ChatUtil.display("&fName: &a" + entry.getName() + "&r");
        ChatUtil.display("&fID: &b" + entry.getId() + "&r");
        ChatUtil.display("&fAuthor: &a" + entry.getAuthor() + "&r");
        ChatUtil.display("&fUpload time: &b" + this.safe(entry.date) + "&r");
        ChatUtil.display("&fLoads: &e" + entry.getLoadCount() + "&r");
        if (!entry.getVersion().isEmpty()) {
            ChatUtil.display("&fVersion: &e" + entry.getVersion() + "&r");
        }

        if (entry.description != null && !entry.description.trim().isEmpty()) {
            ChatUtil.display("&fNote: &7" + entry.description + "&r");
        }
    }

    private void usage() {
        ChatUtil.display(
            Miau.clientName
                + "Usage: .userconfig &olist&r | .userconfig &oload&r <&oid/name&r> | .userconfig &oinfo&r <&oid/name&r>"
        );
    }

    private void async(Runnable task) {
        EXECUTOR.execute(task);
    }

    private void runOnClientThread(Runnable task) {
        Minecraft.func_71410_x().func_152344_a(task);
    }

    private String safe(String value) {
        return value != null && !value.trim().isEmpty() ? value : "unknown";
    }
}
