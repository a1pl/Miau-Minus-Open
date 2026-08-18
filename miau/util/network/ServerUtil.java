package miau.util.network;

import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class ServerUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();

    public static ArrayList<String> getScoreboardLines() {
        if (mc.field_71441_e == null) {
            return new ArrayList<>();
        }

        Scoreboard scoreboard = mc.field_71441_e.func_96441_U();
        if (scoreboard == null) {
            return new ArrayList<>();
        }

        ScoreObjective scoreObjective = scoreboard.func_96539_a(1);
        return scoreObjective == null
            ? new ArrayList<>()
            : scoreboard.func_96534_i(scoreObjective)
                .stream()
                .map(
                    score -> ScorePlayerTeam.func_96667_a(
                        scoreboard.func_96509_i(score.func_96653_e()), score.func_96653_e()
                    )
                )
                .collect(Collectors.toList());
    }

    public static boolean isHypixel() {
        ArrayList<String> arrayList = getScoreboardLines();
        if (arrayList.isEmpty()) {
            return false;
        } else {
            return arrayList.get(0).equals("§ewww.hypixel.ne\ud83c\udf82§et")
                ? true
                : arrayList.get(0).equals("§ewww.hypixel.ne§g§et");
        }
    }

    public static boolean hasPlayerCountInfo() {
        for (String s : getScoreboardLines()) {
            if (s.matches(".*Players: §a\\d+/\\d+.*")) {
                return true;
            }
        }

        return false;
    }
}
