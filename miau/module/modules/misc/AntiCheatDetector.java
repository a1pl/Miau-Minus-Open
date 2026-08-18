package miau.module.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import miau.event.EventTarget;
import miau.event.impl.PacketEvent;
import miau.event.impl.TickEvent;
import miau.event.types.EventType;
import miau.module.Module;
import miau.property.properties.BooleanProperty;
import miau.property.properties.IntProperty;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

public class AntiCheatDetector extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final String PREFIX = "&7[&bAntiCheatDetector&7] &f";
    public static String detectedACName = "";
    public final BooleanProperty debug = new BooleanProperty("debug", false);
    public final IntProperty sampleSize = new IntProperty("sample-size", 5, 4, 12);
    public final IntProperty timeoutTicks = new IntProperty("timeout-ticks", 40, 20, 120);
    private final List<Integer> actionNumbers = new ArrayList<>();
    private boolean checking;
    private int ticksPassed;
    private String lastDetection = "";

    public AntiCheatDetector() {
        super("AntiCheatDetector", false, false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && event.getType() == EventType.RECEIVE) {
            if (event.getPacket() instanceof S01PacketJoinGame) {
                this.startCheck();
            } else {
                if (this.checking && event.getPacket() instanceof S32PacketConfirmTransaction) {
                    this.handleTransaction(((S32PacketConfirmTransaction)event.getPacket()).func_148890_d());
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && this.checking && ++this.ticksPassed > this.timeoutTicks.getValue()) {
            this.detect("None", "Low", false);
        }
    }

    private void startCheck() {
        this.reset();
        this.checking = true;
        detectedACName = "";
        if (this.getServerAddress().contains("hypixel")) {
            this.detect("Watchdog", "High", false);
        }
    }

    private void handleTransaction(short action) {
        this.actionNumbers.add(Integer.valueOf(action));
        this.ticksPassed = 0;
        if (this.debug.getValue()) {
            ChatUtil.display("&7[&bAntiCheatDetector&7] &fID: &e" + action);
        }

        if (this.actionNumbers.size() >= this.sampleSize.getValue()) {
            AntiCheatDetector.DetectionResult result = this.analyzeActionNumbers();
            this.detect(result.name, result.confidence, result.logDebug);
        }
    }

    private AntiCheatDetector.DetectionResult analyzeActionNumbers() {
        List<Integer> diffs = this.getDiffs();
        int first = this.actionNumbers.get(0);
        if (this.isOldVulcan()) {
            return AntiCheatDetector.DetectionResult.high("Old Vulcan");
        }

        if (this.isDuplicateThenIncrement()) {
            return AntiCheatDetector.DetectionResult.medium("Verus");
        }

        if (this.isPolarBurst(diffs)) {
            return AntiCheatDetector.DetectionResult.medium("Polar");
        }

        if (first < -3000 && this.actionNumbers.contains(0)) {
            return AntiCheatDetector.DetectionResult.medium("Intave");
        }

        if (this.isConstantStep(diffs)) {
            int step = diffs.get(0);
            if (step == 1) {
                return AntiCheatDetector.DetectionResult.high(this.detectPositiveStep(first));
            }

            if (step == -1) {
                return AntiCheatDetector.DetectionResult.high(this.detectNegativeStep(first));
            }
        }

        return AntiCheatDetector.DetectionResult.unknown();
    }

    private boolean isConstantStep(List<Integer> diffs) {
        if (diffs.isEmpty()) {
            return false;
        }

        int first = diffs.get(0);

        for (int diff : diffs) {
            if (diff != first) {
                return false;
            }
        }

        return true;
    }

    private boolean isDuplicateThenIncrement() {
        return this.actionNumbers.size() >= 4
            && this.actionNumbers.get(0).equals(this.actionNumbers.get(1))
            && this.isIncrementingFrom(2, 1);
    }

    private boolean isPolarBurst(List<Integer> diffs) {
        if (diffs.size() >= 3 && diffs.get(0) >= 100 && diffs.get(1) == -1) {
            for (int i = 2; i < diffs.size(); i++) {
                if (diffs.get(i) != -1) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isOldVulcan() {
        return this.actionNumbers.size() >= 4
            && this.actionNumbers.get(0) == -30767
            && this.actionNumbers.get(1) == -30766
            && this.actionNumbers.get(2) == -25767
            && this.isIncrementingFrom(3, 1);
    }

    private boolean isIncrementingFrom(int start, int step) {
        for (int i = start; i < this.actionNumbers.size() - 1; i++) {
            if (this.actionNumbers.get(i + 1) - this.actionNumbers.get(i) != step) {
                return false;
            }
        }

        return true;
    }

    private String detectPositiveStep(int first) {
        if (this.inRange(first, -23772, -23762)) {
            return "Vulcan";
        } else if (this.inRange(first, 95, 105) || this.inRange(first, -20005, -19995)) {
            return "Matrix";
        } else {
            return this.inRange(first, -32773, -32762) ? "Grizzly" : "Verus";
        }
    }

    private String detectNegativeStep(int first) {
        if (this.inRange(first, -8287, -8280)) {
            return "Errata";
        } else if (first < -3000) {
            return "Intave";
        } else if (this.inRange(first, -5, 0)) {
            return "Grim";
        } else {
            return this.inRange(first, -3000, -2995) ? "Karhu" : "Polar";
        }
    }

    private boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private void detect(String name, String confidence, boolean logDebug) {
        detectedACName = name;
        this.notifyDetection(name, confidence);
        if (logDebug && this.debug.getValue()) {
            this.logNumbers();
        }

        this.reset();
    }

    private void notifyDetection(String name, String confidence) {
        String key = name + ':' + confidence;
        if (!key.equals(this.lastDetection)) {
            this.lastDetection = key;
            ChatUtil.display("&7[&bAntiCheatDetector&7] &fAnticheat detected: " + name + " (" + confidence + ")");
        }
    }

    private List<Integer> getDiffs() {
        List<Integer> diffs = new ArrayList<>();

        for (int i = 0; i < this.actionNumbers.size() - 1; i++) {
            diffs.add(this.actionNumbers.get(i + 1) - this.actionNumbers.get(i));
        }

        return diffs;
    }

    private void logNumbers() {
        ChatUtil.display("&7[&bAntiCheatDetector&7] &fAction Numbers: &e" + this.join(this.actionNumbers));
        ChatUtil.display("&7[&bAntiCheatDetector&7] &fDifferences: &e" + this.join(this.getDiffs()));
    }

    private String join(List<Integer> values) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }

            builder.append(values.get(i));
        }

        return builder.toString();
    }

    private String getServerAddress() {
        ServerData serverData = mc.func_147104_D();
        return serverData != null && serverData.field_78845_b != null
            ? serverData.field_78845_b.toLowerCase(Locale.ROOT)
            : "";
    }

    private void reset() {
        this.actionNumbers.clear();
        this.ticksPassed = 0;
        this.checking = false;
    }

    @Override
    public void onEnabled() {
        this.startCheck();
    }

    @Override
    public void onDisabled() {
        this.reset();
    }

    private static class DetectionResult {
        private final String name;
        private final String confidence;
        private final boolean logDebug;

        private DetectionResult(String name, String confidence, boolean logDebug) {
            this.name = name;
            this.confidence = confidence;
            this.logDebug = logDebug;
        }

        private static AntiCheatDetector.DetectionResult high(String name) {
            return new AntiCheatDetector.DetectionResult(name, "High", false);
        }

        private static AntiCheatDetector.DetectionResult medium(String name) {
            return new AntiCheatDetector.DetectionResult(name, "Medium", false);
        }

        private static AntiCheatDetector.DetectionResult unknown() {
            return new AntiCheatDetector.DetectionResult("Unknown", "Low", true);
        }
    }
}
