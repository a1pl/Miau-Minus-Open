package miau.command.commands;

import java.util.ArrayList;
import java.util.Arrays;
import miau.command.Command;
import miau.util.client.ChatUtil;

public class ReportCommand extends Command {
    public ReportCommand() {
        super(new ArrayList<>(Arrays.asList("report", "hacker", "cheater")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            ChatUtil.display("&7Usage: &e.report <player> <reason>&7");
            ChatUtil.display("&7Example: &e.report Steve killaura&7");
        } else {
            String targetPlayer = args.get(1).trim();
            StringBuilder reasonBuilder = new StringBuilder();

            for (int i = 2; i < args.size(); i++) {
                if (reasonBuilder.length() > 0) {
                    reasonBuilder.append(" ");
                }

                reasonBuilder.append(args.get(i));
            }

            String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : "suspicious behavior";
            ChatUtil.sendMessage("/report " + targetPlayer + " " + reason);
            ChatUtil.display("&aReport sent! &7Reported &f%s &7for &f%s&7.", targetPlayer, reason);
        }
    }
}
