package miau;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.florianmichael.viamcp.ViaMCP;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import me.ksyz.accountmanager.AccountManager;
import miau.command.CommandManager;
import miau.command.commands.BindCommand;
import miau.command.commands.ConfigCommand;
import miau.command.commands.DenickCommand;
import miau.command.commands.FriendCommand;
import miau.command.commands.HelpCommand;
import miau.command.commands.HideCommand;
import miau.command.commands.IgnCommand;
import miau.command.commands.ItemCommand;
import miau.command.commands.ListCommand;
import miau.command.commands.ModuleCommand;
import miau.command.commands.OnlineConfigCommand;
import miau.command.commands.PlayerCommand;
import miau.command.commands.ReportCommand;
import miau.command.commands.ShowCommand;
import miau.command.commands.TargetCommand;
import miau.command.commands.ToggleCommand;
import miau.command.commands.UserConfigCommand;
import miau.command.commands.VclipCommand;
import miau.component.BadPacketsComponent;
import miau.component.BlinkComponent;
import miau.component.PingSpoofComponent;
import miau.component.RotationComponent;
import miau.component.SlotComponent;
import miau.config.Config;
import miau.event.EventManager;
import miau.management.BlinkManager;
import miau.management.DelayManager;
import miau.management.DiscordRichPresence;
import miau.management.DragManager;
import miau.management.FloatManager;
import miau.management.FriendManager;
import miau.management.LagManager;
import miau.management.PlayerStateManager;
import miau.management.RotationManager;
import miau.management.TargetManager;
import miau.module.Module;
import miau.module.ModuleManager;
import miau.module.modules.combat.AdvancedJumpReset;
import miau.module.modules.combat.AntiFireball;
import miau.module.modules.combat.ArmorBreaker;
import miau.module.modules.combat.AutoArmor;
import miau.module.modules.combat.AutoBlock;
import miau.module.modules.combat.AutoBow;
import miau.module.modules.combat.AutoLeave;
import miau.module.modules.combat.AutoRod;
import miau.module.modules.combat.AutoRod2;
import miau.module.modules.combat.AutoWeapon;
import miau.module.modules.combat.ComboOneHit;
import miau.module.modules.combat.Displace;
import miau.module.modules.combat.Fixes;
import miau.module.modules.combat.ForwardTrack;
import miau.module.modules.combat.HitBox;
import miau.module.modules.combat.KillAura;
import miau.module.modules.combat.KillAura2;
import miau.module.modules.combat.KnockbackDelay;
import miau.module.modules.combat.LegitReach;
import miau.module.modules.combat.MoreClick;
import miau.module.modules.combat.NewVelocity;
import miau.module.modules.combat.OldGrimGapple;
import miau.module.modules.combat.Piercing;
import miau.module.modules.combat.ProjectileAimBot;
import miau.module.modules.combat.Reach;
import miau.module.modules.combat.Refill;
import miau.module.modules.combat.SmartBlinker;
import miau.module.modules.combat.SmoothAimAssist;
import miau.module.modules.combat.TargetStrafe;
import miau.module.modules.combat.TeleportHit;
import miau.module.modules.combat.TimerRange;
import miau.module.modules.combat.TimerRangeV2;
import miau.module.modules.combat.Velocity;
import miau.module.modules.ghost.AimAssist;
import miau.module.modules.ghost.AutoBed;
import miau.module.modules.ghost.AutoClicker;
import miau.module.modules.ghost.AutoLadderClutch;
import miau.module.modules.ghost.BlockHit;
import miau.module.modules.ghost.BlockLadder;
import miau.module.modules.ghost.BridgeAssist;
import miau.module.modules.ghost.BslLegitTellyFix;
import miau.module.modules.ghost.Clutch;
import miau.module.modules.ghost.FastPlace;
import miau.module.modules.ghost.JumpReset;
import miau.module.modules.ghost.MoreKB;
import miau.module.modules.ghost.NoClickDelay;
import miau.module.modules.ghost.NoJumpDelay;
import miau.module.modules.ghost.SmartClicking;
import miau.module.modules.ghost.SprintReset;
import miau.module.modules.minigames.AutoBuy;
import miau.module.modules.minigames.BedTracker;
import miau.module.modules.minigames.BedwarsUtils;
import miau.module.modules.minigames.PartyDetector;
import miau.module.modules.minigames.PlayerList;
import miau.module.modules.minigames.SkywarsAlerts;
import miau.module.modules.minigames.ThePitUtils;
import miau.module.modules.misc.AntiBot;
import miau.module.modules.misc.AntiCheatDetector;
import miau.module.modules.misc.AntiObbyTrap;
import miau.module.modules.misc.AntiObfuscate;
import miau.module.modules.misc.AutoAnduril;
import miau.module.modules.misc.AutoAuth;
import miau.module.modules.misc.AutoGG;
import miau.module.modules.misc.AutoPlay;
import miau.module.modules.misc.AutoReconnect;
import miau.module.modules.misc.Balance;
import miau.module.modules.misc.BalanceFix;
import miau.module.modules.misc.CheatDetector;
import miau.module.modules.misc.ClientSpoofer;
import miau.module.modules.misc.Disabler;
import miau.module.modules.misc.Fireball;
import miau.module.modules.misc.FlagDetector;
import miau.module.modules.misc.HypixelDisabler;
import miau.module.modules.misc.KillSults;
import miau.module.modules.misc.LightningTracker;
import miau.module.modules.misc.MouseRawInput;
import miau.module.modules.misc.MoveFix;
import miau.module.modules.misc.MurderDetector;
import miau.module.modules.misc.NickHider;
import miau.module.modules.misc.NoRotate;
import miau.module.modules.misc.NowPlayingHud;
import miau.module.modules.misc.Panic;
import miau.module.modules.misc.RPC;
import miau.module.modules.misc.Spammer;
import miau.module.modules.misc.SpotifyMod;
import miau.module.modules.misc.StaffDetector;
import miau.module.modules.misc.ViewPackets;
import miau.module.modules.movement.AntiAFK;
import miau.module.modules.movement.AntiVoid;
import miau.module.modules.movement.Blink;
import miau.module.modules.movement.Blinkvoid;
import miau.module.modules.movement.Fly;
import miau.module.modules.movement.Jesus;
import miau.module.modules.movement.KeepSprint;
import miau.module.modules.movement.KnockbackBoost;
import miau.module.modules.movement.LongJump;
import miau.module.modules.movement.NoFall;
import miau.module.modules.movement.NoSlow;
import miau.module.modules.movement.NoSlowUtils;
import miau.module.modules.movement.NoWeb;
import miau.module.modules.movement.SafeWalk;
import miau.module.modules.movement.Speed;
import miau.module.modules.movement.Sprint;
import miau.module.modules.movement.TimerBalance;
import miau.module.modules.movement.TimerHop;
import miau.module.modules.network.BackTrack;
import miau.module.modules.network.FakeLag;
import miau.module.modules.network.LagRange;
import miau.module.modules.network.LagRange2;
import miau.module.modules.network.PingSpoof;
import miau.module.modules.network.TickBase;
import miau.module.modules.player.AntiDebuff;
import miau.module.modules.player.AutoBlockIn;
import miau.module.modules.player.AutoChest;
import miau.module.modules.player.AutoHead;
import miau.module.modules.player.AutoSoup;
import miau.module.modules.player.AutoSwap;
import miau.module.modules.player.AutoTool;
import miau.module.modules.player.BedNuker;
import miau.module.modules.player.ChestStealer;
import miau.module.modules.player.Freeze;
import miau.module.modules.player.GhostHand;
import miau.module.modules.player.InvManager;
import miau.module.modules.player.InvWalk;
import miau.module.modules.player.Scaffold;
import miau.module.modules.player.Scaffold2;
import miau.module.modules.player.SpeedMine;
import miau.module.modules.render.Ambience;
import miau.module.modules.render.Animations;
import miau.module.modules.render.BedESP;
import miau.module.modules.render.BetterHud2;
import miau.module.modules.render.BlockOverlay;
import miau.module.modules.render.BreakProgress;
import miau.module.modules.render.Capes;
import miau.module.modules.render.Chams;
import miau.module.modules.render.ChestESP;
import miau.module.modules.render.ClickGUI;
import miau.module.modules.render.ESP;
import miau.module.modules.render.EntityCulling;
import miau.module.modules.render.FallPosition;
import miau.module.modules.render.FreeLook;
import miau.module.modules.render.FullBright;
import miau.module.modules.render.HUD;
import miau.module.modules.render.Indicators;
import miau.module.modules.render.ItemESP;
import miau.module.modules.render.ItemPhysics;
import miau.module.modules.render.Keystrokes;
import miau.module.modules.render.KillEffect;
import miau.module.modules.render.NameTags;
import miau.module.modules.render.NoGui;
import miau.module.modules.render.NoHurtCam;
import miau.module.modules.render.PearlESP;
import miau.module.modules.render.Scoreboard;
import miau.module.modules.render.SlinkNotifs;
import miau.module.modules.render.Statistics;
import miau.module.modules.render.TargetHUD;
import miau.module.modules.render.TargetHud2;
import miau.module.modules.render.TargetMark;
import miau.module.modules.render.TimerBeacon;
import miau.module.modules.render.Tracers;
import miau.module.modules.render.Trajectories;
import miau.module.modules.render.Utility;
import miau.module.modules.render.UtilsOverlay;
import miau.module.modules.render.ViewClip;
import miau.module.modules.render.WaterMark;
import miau.notification.NotificationManager;
import miau.notification.NotificationRenderer;
import miau.property.Property;
import miau.property.PropertyManager;
import miau.util.player.PlayerTracker;
import org.lwjgl.opengl.Display;

public class Miau {
    public static final boolean DEVELOPMENT_SWITCH = false;
    public static String clientName = "&7[&cM&6i&ea&au&7-]&r ";
    public static String version = "1.1.0-beta";
    public static RotationManager rotationManager;
    public static FloatManager floatManager;
    public static BlinkManager blinkManager;
    public static DelayManager delayManager;
    public static LagManager lagManager;
    public static PlayerStateManager playerStateManager;
    public static FriendManager friendManager;
    public static TargetManager targetManager;
    public static PropertyManager propertyManager;
    public static ModuleManager moduleManager;
    public static CommandManager commandManager;
    public static DiscordRichPresence discordRichPresence;
    public static NotificationManager notificationManager;
    public static DragManager dragManager;
    public static PlayerTracker playerTracker;
    public static BadPacketsComponent badPacketsComponent;
    public static SlotComponent slotComponent;

    public Miau() {
        this.init();
    }

    public void init() {
        rotationManager = new RotationManager();
        floatManager = new FloatManager();
        blinkManager = new BlinkManager();
        delayManager = new DelayManager();
        lagManager = new LagManager();
        playerStateManager = new PlayerStateManager();
        friendManager = new FriendManager();
        targetManager = new TargetManager();
        propertyManager = new PropertyManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();
        discordRichPresence = new DiscordRichPresence();
        notificationManager = new NotificationManager();
        dragManager = new DragManager();
        EventManager.register(rotationManager);
        EventManager.register(floatManager);
        EventManager.register(blinkManager);
        EventManager.register(delayManager);
        EventManager.register(lagManager);
        EventManager.register(moduleManager);
        EventManager.register(commandManager);
        EventManager.register(discordRichPresence);
        EventManager.register(NotificationRenderer.getInstance());
        EventManager.register(dragManager);
        badPacketsComponent = new BadPacketsComponent();
        EventManager.register(badPacketsComponent);
        slotComponent = new SlotComponent();
        EventManager.register(slotComponent);
        EventManager.register(new PingSpoofComponent());
        EventManager.register(new BlinkComponent());
        EventManager.register(new RotationComponent());
        playerTracker = new PlayerTracker();
        EventManager.register(playerTracker);
        moduleManager.modules.put(AimAssist.class, new AimAssist());
        moduleManager.modules.put(Ambience.class, new Ambience());
        moduleManager.modules.put(Animations.class, new Animations());
        moduleManager.modules.put(AntiAFK.class, new AntiAFK());
        moduleManager.modules.put(AntiBot.class, new AntiBot());
        moduleManager.modules.put(AntiCheatDetector.class, new AntiCheatDetector());
        moduleManager.modules.put(AntiDebuff.class, new AntiDebuff());
        moduleManager.modules.put(AntiFireball.class, new AntiFireball());
        moduleManager.modules.put(AntiObbyTrap.class, new AntiObbyTrap());
        moduleManager.modules.put(AntiObfuscate.class, new AntiObfuscate());
        moduleManager.modules.put(AntiVoid.class, new AntiVoid());
        moduleManager.modules.put(ArmorBreaker.class, new ArmorBreaker());
        moduleManager.modules.put(AutoAnduril.class, new AutoAnduril());
        moduleManager.modules.put(AutoArmor.class, new AutoArmor());
        moduleManager.modules.put(AutoAuth.class, new AutoAuth());
        moduleManager.modules.put(AutoGG.class, new AutoGG());
        moduleManager.modules.put(AutoPlay.class, new AutoPlay());
        moduleManager.modules.put(AutoBlockIn.class, new AutoBlockIn());
        moduleManager.modules.put(AutoBlock.class, new AutoBlock());
        moduleManager.modules.put(AutoBow.class, new AutoBow());
        moduleManager.modules.put(AutoBed.class, new AutoBed());
        moduleManager.modules.put(AutoBuy.class, new AutoBuy());
        moduleManager.modules.put(AutoChest.class, new AutoChest());
        moduleManager.modules.put(AutoClicker.class, new AutoClicker());
        moduleManager.modules.put(AutoHead.class, new AutoHead());
        moduleManager.modules.put(AutoLadderClutch.class, new AutoLadderClutch());
        moduleManager.modules.put(AutoLeave.class, new AutoLeave());
        moduleManager.modules.put(BlockLadder.class, new BlockLadder());
        moduleManager.modules.put(BslLegitTellyFix.class, new BslLegitTellyFix());
        moduleManager.modules.put(SpotifyMod.class, new SpotifyMod());
        moduleManager.modules.put(AutoReconnect.class, new AutoReconnect());
        moduleManager.modules.put(AutoSoup.class, new AutoSoup());
        moduleManager.modules.put(AutoSwap.class, new AutoSwap());
        moduleManager.modules.put(AutoTool.class, new AutoTool());
        moduleManager.modules.put(AutoWeapon.class, new AutoWeapon());
        moduleManager.modules.put(BackTrack.class, new BackTrack());
        moduleManager.modules.put(BedESP.class, new BedESP());
        moduleManager.modules.put(BedNuker.class, new BedNuker());
        moduleManager.modules.put(BedTracker.class, new BedTracker());
        moduleManager.modules.put(BedwarsUtils.class, new BedwarsUtils());
        moduleManager.modules.put(PartyDetector.class, new PartyDetector());
        moduleManager.modules.put(Blink.class, new Blink());
        moduleManager.modules.put(Blinkvoid.class, new Blinkvoid());
        moduleManager.modules.put(BlockHit.class, new BlockHit());
        moduleManager.modules.put(BlockOverlay.class, new BlockOverlay());
        moduleManager.modules.put(BreakProgress.class, new BreakProgress());
        moduleManager.modules.put(BridgeAssist.class, new BridgeAssist());
        moduleManager.modules.put(Capes.class, new Capes());
        moduleManager.modules.put(Chams.class, new Chams());
        moduleManager.modules.put(CheatDetector.class, new CheatDetector());
        moduleManager.modules.put(ChestESP.class, new ChestESP());
        moduleManager.modules.put(ChestStealer.class, new ChestStealer());
        moduleManager.modules.put(ClickGUI.class, new ClickGUI());
        moduleManager.modules.put(ComboOneHit.class, new ComboOneHit());
        moduleManager.modules.put(NoGui.class, new NoGui());
        moduleManager.modules.put(ClientSpoofer.class, new ClientSpoofer());
        moduleManager.modules.put(Clutch.class, new Clutch());
        moduleManager.modules.put(Disabler.class, new Disabler());
        moduleManager.modules.put(Displace.class, new Displace());
        moduleManager.modules.put(ESP.class, new ESP());
        moduleManager.modules.put(EntityCulling.class, new EntityCulling());
        moduleManager.modules.put(FakeLag.class, new FakeLag());
        moduleManager.modules.put(FastPlace.class, new FastPlace());
        moduleManager.modules.put(FlagDetector.class, new FlagDetector());
        moduleManager.modules.put(Fixes.class, new Fixes());
        moduleManager.modules.put(Fly.class, new Fly());
        moduleManager.modules.put(FreeLook.class, new FreeLook());
        moduleManager.modules.put(Freeze.class, new Freeze());
        moduleManager.modules.put(ForwardTrack.class, new ForwardTrack());
        moduleManager.modules.put(FullBright.class, new FullBright());
        moduleManager.modules.put(Fireball.class, new Fireball());
        moduleManager.modules.put(GhostHand.class, new GhostHand());
        moduleManager.modules.put(JumpReset.class, new JumpReset());
        moduleManager.modules.put(HUD.class, new HUD());
        moduleManager.modules.put(HypixelDisabler.class, new HypixelDisabler());
        moduleManager.modules.put(HitBox.class, new HitBox());
        moduleManager.modules.put(Indicators.class, new Indicators());
        moduleManager.modules.put(InvManager.class, new InvManager());
        moduleManager.modules.put(InvWalk.class, new InvWalk());
        moduleManager.modules.put(ItemESP.class, new ItemESP());
        moduleManager.modules.put(ItemPhysics.class, new ItemPhysics());
        moduleManager.modules.put(Jesus.class, new Jesus());
        moduleManager.modules.put(KeepSprint.class, new KeepSprint());
        moduleManager.modules.put(Keystrokes.class, new Keystrokes());
        moduleManager.modules.put(KillAura.class, new KillAura());
        moduleManager.modules.put(KillAura2.class, new KillAura2());
        moduleManager.modules.put(KillEffect.class, new KillEffect());
        moduleManager.modules.put(KillSults.class, new KillSults());
        moduleManager.modules.put(KnockbackDelay.class, new KnockbackDelay());
        moduleManager.modules.put(KnockbackBoost.class, new KnockbackBoost());
        moduleManager.modules.put(LagRange.class, new LagRange());
        moduleManager.modules.put(LagRange2.class, new LagRange2());
        moduleManager.modules.put(LegitReach.class, new LegitReach());
        moduleManager.modules.put(LightningTracker.class, new LightningTracker());
        moduleManager.modules.put(LongJump.class, new LongJump());
        moduleManager.modules.put(MoveFix.class, new MoveFix());
        moduleManager.modules.put(MouseRawInput.class, new MouseRawInput());
        moduleManager.modules.put(MurderDetector.class, new MurderDetector());
        moduleManager.modules.put(NameTags.class, new NameTags());
        moduleManager.modules.put(NickHider.class, new NickHider());
        moduleManager.modules.put(NoClickDelay.class, new NoClickDelay());
        moduleManager.modules.put(NoFall.class, new NoFall());
        moduleManager.modules.put(NoHurtCam.class, new NoHurtCam());
        moduleManager.modules.put(NoJumpDelay.class, new NoJumpDelay());
        moduleManager.modules.put(NoRotate.class, new NoRotate());
        moduleManager.modules.put(NoSlow.class, new NoSlow());
        moduleManager.modules.put(NoSlowUtils.class, new NoSlowUtils());
        moduleManager.modules.put(OldGrimGapple.class, new OldGrimGapple());
        moduleManager.modules.put(AutoRod.class, new AutoRod());
        moduleManager.modules.put(AutoRod2.class, new AutoRod2());
        moduleManager.modules.put(NowPlayingHud.class, new NowPlayingHud());
        moduleManager.modules.put(ViewPackets.class, new ViewPackets());
        moduleManager.modules.put(Panic.class, new Panic());
        moduleManager.modules.put(Piercing.class, new Piercing());
        moduleManager.modules.put(PingSpoof.class, new PingSpoof());
        moduleManager.modules.put(PlayerList.class, new PlayerList());
        moduleManager.modules.put(SkywarsAlerts.class, new SkywarsAlerts());
        moduleManager.modules.put(ProjectileAimBot.class, new ProjectileAimBot());
        moduleManager.modules.put(RPC.class, new RPC());
        moduleManager.modules.put(Reach.class, new Reach());
        moduleManager.modules.put(Refill.class, new Refill());
        moduleManager.modules.put(SafeWalk.class, new SafeWalk());
        moduleManager.modules.put(Scaffold.class, new Scaffold());
        moduleManager.modules.put(Scaffold2.class, new Scaffold2());
        moduleManager.modules.put(Scoreboard.class, new Scoreboard());
        moduleManager.modules.put(Spammer.class, new Spammer());
        moduleManager.modules.put(Speed.class, new Speed());
        moduleManager.modules.put(TimerHop.class, new TimerHop());
        moduleManager.modules.put(TimerBalance.class, new TimerBalance());
        moduleManager.modules.put(SpeedMine.class, new SpeedMine());
        moduleManager.modules.put(Sprint.class, new Sprint());
        moduleManager.modules.put(SmoothAimAssist.class, new SmoothAimAssist());
        moduleManager.modules.put(StaffDetector.class, new StaffDetector());
        moduleManager.modules.put(BalanceFix.class, new BalanceFix());
        moduleManager.modules.put(NoWeb.class, new NoWeb());
        moduleManager.modules.put(Balance.class, new Balance());
        moduleManager.modules.put(SprintReset.class, new SprintReset());
        moduleManager.modules.put(SmartBlinker.class, new SmartBlinker());
        moduleManager.modules.put(SmartClicking.class, new SmartClicking());
        moduleManager.modules.put(SlinkNotifs.class, new SlinkNotifs());
        moduleManager.modules.put(BetterHud2.class, new BetterHud2());
        moduleManager.modules.put(Statistics.class, new Statistics());
        moduleManager.modules.put(TargetHUD.class, new TargetHUD());
        moduleManager.modules.put(TargetHud2.class, new TargetHud2());
        moduleManager.modules.put(TargetStrafe.class, new TargetStrafe());
        moduleManager.modules.put(TimerRange.class, new TimerRange());
        moduleManager.modules.put(TimerRangeV2.class, new TimerRangeV2());
        moduleManager.modules.put(TeleportHit.class, new TeleportHit());
        moduleManager.modules.put(ThePitUtils.class, new ThePitUtils());
        moduleManager.modules.put(TickBase.class, new TickBase());
        moduleManager.modules.put(Tracers.class, new Tracers());
        moduleManager.modules.put(Trajectories.class, new Trajectories());
        moduleManager.modules.put(PearlESP.class, new PearlESP());
        moduleManager.modules.put(FallPosition.class, new FallPosition());
        moduleManager.modules.put(TimerBeacon.class, new TimerBeacon());
        moduleManager.modules.put(UtilsOverlay.class, new UtilsOverlay());
        moduleManager.modules.put(Utility.class, new Utility());
        moduleManager.modules.put(Velocity.class, new Velocity());
        moduleManager.modules.put(ViewClip.class, new ViewClip());
        moduleManager.modules.put(MoreKB.class, new MoreKB());
        moduleManager.modules.put(MoreClick.class, new MoreClick());
        moduleManager.modules.put(AdvancedJumpReset.class, new AdvancedJumpReset());
        moduleManager.modules.put(NewVelocity.class, new NewVelocity());
        moduleManager.modules.put(Statistics.class, new Statistics());
        moduleManager.modules.put(WaterMark.class, new WaterMark());
        moduleManager.modules.put(TargetMark.class, new TargetMark());
        moduleManager.modules.put(CheatDetector.class, new CheatDetector());
        commandManager.commands.add(new BindCommand());
        commandManager.commands.add(new ConfigCommand());
        commandManager.commands.add(new OnlineConfigCommand());
        commandManager.commands.add(new UserConfigCommand());
        commandManager.commands.add(new DenickCommand());
        commandManager.commands.add(new FriendCommand());
        commandManager.commands.add(new HelpCommand());
        commandManager.commands.add(new HideCommand());
        commandManager.commands.add(new IgnCommand());
        commandManager.commands.add(new ItemCommand());
        commandManager.commands.add(new ListCommand());
        commandManager.commands.add(new ModuleCommand());
        commandManager.commands.add(new PlayerCommand());
        commandManager.commands.add(new ShowCommand());
        commandManager.commands.add(new TargetCommand());
        commandManager.commands.add(new ToggleCommand());
        commandManager.commands.add(new VclipCommand());
        commandManager.commands.add(new ReportCommand());

        for (Module module : moduleManager.modules.values()) {
            ArrayList<Property<?>> properties = new ArrayList<>();

            for (Field field : module.getClass().getDeclaredFields()) {
                field.setAccessible(true);

                Object obj;
                try {
                    obj = field.get(module);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                if (obj instanceof Property) {
                    ((Property)obj).setOwner(module);
                    properties.add((Property<?>)obj);
                }
            }

            for (Property<?> p : module.getAdditionalProperties()) {
                p.setOwner(module);
                properties.add(p);
            }

            propertyManager.properties.put(module.getClass(), properties);
            EventManager.register(module);
        }

        Config config = new Config("default", true);
        if (config.file.exists()) {
            config.load();
        }

        if (friendManager.file.exists()) {
            friendManager.load();
        }

        if (targetManager.file.exists()) {
            targetManager.load();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (moduleManager != null && propertyManager != null) {
                config.save();
            }
        }));

        try {
            InputStreamReader reader = new InputStreamReader(
                Objects.requireNonNull(Miau.class.getResourceAsStream("/version.json")), StandardCharsets.UTF_8
            );

            try {
                JsonObject modInfo = new JsonParser().parse(reader).getAsJsonObject();
                version = modInfo.get("version").getAsString();
            } catch (Throwable var11) {
                try {
                    reader.close();
                } catch (Throwable var10) {
                    var11.addSuppressed(var10);
                }

                throw var11;
            }

            reader.close();
        } catch (Exception e) {
            version = "1.1.0-beta";
        }

        Display.setTitle(ClientInfo.getDisplayVersion());
        AccountManager.init();
        ViaMCP.create();
    }

    public static Locale getLocale() {
        return Locale.getDefault();
    }

    public static void setLocale(Locale locale) {
        Locale.setDefault(locale);
    }

    public static void terminate() {
    }
}
