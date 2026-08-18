package miau.module.modules.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.Render2DEvent;
import miau.module.Module;
import miau.notification.NotificationType;
import miau.property.properties.DragProperty;
import miau.property.properties.ModeProperty;
import miau.property.properties.TextProperty;
import miau.util.animation.Animation;
import miau.util.animation.Direction;
import miau.util.animation.impl.DecelerateAnimation;
import miau.util.font.Font;
import miau.util.font.FontRepository;
import miau.util.render.ColorUtil;
import miau.util.render.RenderUtil;
import miau.util.shader.RoundedUtils;
import miau.util.spotify.LastFmAPI;
import miau.util.spotify.SpotifyAPI;
import miau.util.vector.Vector2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class SpotifyMod extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private final ModeProperty apiMode = new ModeProperty("API Mode", 1, new String[]{"LastFM", "Spotify"});
    private final TextProperty lastFmUser = new TextProperty(
        "Username", "", () -> this.apiMode.getModeString().equals("LastFM")
    );
    private final TextProperty lastFmApiKey = new TextProperty(
        "LASTFM API Key", "", () -> this.apiMode.getModeString().equals("LastFM")
    );
    private final TextProperty spotifyClientId = new TextProperty(
        "Spotify Client ID", "", () -> this.apiMode.getModeString().equals("Spotify")
    );
    private final TextProperty spotifyClientSecret = new TextProperty(
        "Spotify Client Secret", "", () -> this.apiMode.getModeString().equals("Spotify")
    );
    private final ModeProperty backgroundColor = new ModeProperty(
        "Background", 0, new String[]{"Miau Minus", "Average", "Spotify Grey", "Sync"}
    );
    private final DragProperty drag = new DragProperty("Spotify", new Vector2d(5.0, 150.0));
    public final float height = 50.0F;
    public final float albumCoverSize = 50.0F;
    private final float playerWidth = 135.0F;
    private final float width = 185.0F;
    private final Animation scrollTrack = new DecelerateAnimation(10000, 1.0, Direction.BACKWARDS);
    private final Animation scrollArtist = new DecelerateAnimation(10000, 1.0, Direction.BACKWARDS);
    public LastFmAPI api;
    public SpotifyAPI spotifyApi;
    private boolean downloadedCover;
    private ResourceLocation currentAlbumCover;
    private Color imageColor = Color.WHITE;
    private String lastDownloadedId = "";
    private final Color greyColor = new Color(30, 30, 30);
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final File LASTFM_CREDS_DIR = new File(Minecraft.func_71410_x().field_71412_D, "Miau_LastFm.json");

    public SpotifyMod() {
        super("Spotify", false, true);
        this.drag.render = true;
    }

    @Override
    public void onEnabled() {
        if (mc.field_71439_g == null) {
            this.toggle();
        } else {
            if (this.apiMode.getModeString().equals("Spotify")) {
                String clientId = this.spotifyClientId.getValue();
                String clientSecret = this.spotifyClientSecret.getValue();
                if (this.spotifyApi == null) {
                    this.spotifyApi = new SpotifyAPI();
                }

                if (clientId.isEmpty() || clientSecret.isEmpty()) {
                    this.loadCredentials();
                    clientId = this.spotifyClientId.getValue();
                    clientSecret = this.spotifyClientSecret.getValue();
                    if (clientId.isEmpty() || clientSecret.isEmpty()) {
                        Miau.notificationManager
                            .pop(
                                "Error", "Please input Spotify Client ID and Secret in settings", NotificationType.WARN
                            );
                        this.toggle();
                        return;
                    }
                }

                this.saveCredentials();
                this.spotifyApi.startConnection(clientId, clientSecret);
            } else {
                String user = this.lastFmUser.getValue();
                String key = this.lastFmApiKey.getValue();
                if (this.api == null) {
                    this.api = new LastFmAPI();
                }

                if (user.equals("") || key.equals("")) {
                    this.loadCredentials();
                    user = this.lastFmUser.getValue();
                    key = this.lastFmApiKey.getValue();
                    if (user.equals("") || key.equals("")) {
                        Miau.notificationManager
                            .pop("Error", "Please input Last.fm User and API Key in settings", NotificationType.WARN);

                        try {
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().browse(new URI("https://idle.e-z.tools/p/b8tsrcndhv"));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        this.toggle();
                        return;
                    }
                }

                this.saveCredentials();
                this.api.startConnection(user, key);
            }

            super.onEnabled();
        }
    }

    @Override
    public void onDisabled() {
        if (this.spotifyApi != null) {
            this.spotifyApi.stopConnection();
        }

        super.onDisabled();
    }

    public boolean isPlaying() {
        return this.apiMode.getModeString().equals("Spotify")
            ? this.spotifyApi != null && this.spotifyApi.isPlaying
            : this.api != null && this.api.isPlaying;
    }

    public String getTrackName() {
        return this.apiMode.getModeString().equals("Spotify")
            ? (this.spotifyApi != null ? this.spotifyApi.trackName : "Unknown")
            : (this.api != null ? this.api.trackName : "Unknown");
    }

    public String getArtistName() {
        return this.apiMode.getModeString().equals("Spotify")
            ? (this.spotifyApi != null ? this.spotifyApi.artistName : "Unknown")
            : (this.api != null ? this.api.artistName : "Unknown");
    }

    public String getAlbumUrl() {
        return this.apiMode.getModeString().equals("Spotify")
            ? (this.spotifyApi != null ? this.spotifyApi.albumUrl : "")
            : (this.api != null ? this.api.albumUrl : "");
    }

    public String getCurrentMbid() {
        return this.apiMode.getModeString().equals("Spotify")
            ? (this.spotifyApi != null ? this.spotifyApi.currentMbid : "")
            : (this.api != null ? this.api.currentMbid : "");
    }

    public static void scissor(double x, double y, double width, double height) {
        ScaledResolution sr = new ScaledResolution(Minecraft.func_71410_x());
        double scale = sr.func_78325_e();
        y = sr.func_78328_b() - y;
        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;
        GL11.glScissor((int)x, (int)(y - height), (int)width, (int)height);
    }

    @EventTarget
    public void onRender2DEvent(Render2DEvent event) {
        if (this.isPlaying()) {
            float x = (float)this.drag.position.x;
            float y = (float)this.drag.position.y;
            if (this.backgroundColor.getModeString().equals("Miau Minus")) {
                this.drag.scale.x = 170.0;
                this.drag.scale.y = 205.0;
                this.renderMiauMode(x, y);
            } else {
                this.drag.scale.x = 185.0;
                this.drag.scale.y = 50.0;
                this.renderClassicMode(x, y);
            }
        }
    }

    private void renderClassicMode(float x, float y) {
        Color color2 = ColorUtil.darker(this.imageColor, 0.65F);
        switch (this.backgroundColor.getModeString()) {
            case "Average":
                float[] hsb = Color.RGBtoHSB(
                    this.imageColor.getRed(), this.imageColor.getGreen(), this.imageColor.getBlue(), null
                );
                if (hsb[2] < 0.5F) {
                    color2 = ColorUtil.brighter(this.imageColor, 0.65F);
                }

                RoundedUtils.drawRound(x + 35.0F, y, 150.0F, 50.0F, 6.0F, this.imageColor);
                break;
            case "Spotify Grey":
                RoundedUtils.drawRound(x + 35.0F, y, 150.0F, 50.0F, 6.0F, this.greyColor);
                break;
            case "Sync":
                RoundedUtils.drawRound(x + 35.0F, y, 150.0F, 50.0F, 6.0F, ColorUtil.rainbow(0));
        }

        Font font18 = FontRepository.getFont("inter-medium", 18.0F);
        Font font22 = FontRepository.getFont("inter-bold", 22.0F);
        GL11.glEnable(3089);
        scissor(x + 50.0F, y, 135.0, 50.0);
        String currentTrackName = this.getTrackName();
        String currentArtistName = this.getArtistName();
        if (this.scrollTrack.getDirection() == Direction.BACKWARDS && this.scrollTrack.getOutput() == 0.0) {
            this.scrollTrack.reset();
        }

        if (this.scrollArtist.getDirection() == Direction.BACKWARDS && this.scrollArtist.getOutput() == 0.0) {
            this.scrollArtist.reset();
        }

        boolean needsToScrollTrack = font22.width(currentTrackName) > 135.0F;
        boolean needsToScrollArtist = font18.width(currentArtistName) > 135.0F;
        float trackX = (float)(
            x + 50.0F - font22.width(currentTrackName)
                + (font22.width(currentTrackName) + 135.0F) * this.scrollTrack.getOutput()
        );
        font22.draw(currentTrackName, needsToScrollTrack ? trackX : x + 50.0F + 4.0F, y + 8.0F, -1);
        float artistX = (float)(
            x + 50.0F - font18.width(currentArtistName)
                + (font18.width(currentArtistName) + 135.0F) * this.scrollArtist.getOutput()
        );
        font18.draw(currentArtistName, needsToScrollArtist ? artistX : x + 50.0F + 4.0F, y + 26.0F, -1);
        GL11.glDisable(3089);
        this.downloadAlbumArt();
        if (this.currentAlbumCover != null && this.downloadedCover) {
            RenderUtil.resetColor();
            mc.func_110434_K().func_110577_a(this.currentAlbumCover);
            GlStateManager.func_179124_c(1.0F, 1.0F, 1.0F);
            GL11.glEnable(3042);
            RoundedUtils.drawRoundTextured(x, y, 50.0F, 50.0F, 6.0F, 1.0F);
        }
    }

    private void renderMiauMode(float x, float y) {
        float width = 170.0F;
        float height = 205.0F;
        Color colorTop = new Color(130, 28, 46, 140);
        Color colorBottom = new Color(8, 3, 5, 216);
        RoundedUtils.drawGradientRound(x, y, width, height, 15.0F, colorBottom, colorTop, colorBottom, colorTop);
        float padding = 9.0F;
        float artSize = width - padding * 2.0F;
        float artX = x + padding;
        float artY = y + padding;
        this.downloadAlbumArt();
        if (this.currentAlbumCover != null && this.downloadedCover) {
            RenderUtil.resetColor();
            mc.func_110434_K().func_110577_a(this.currentAlbumCover);
            GlStateManager.func_179124_c(1.0F, 1.0F, 1.0F);
            GL11.glEnable(3042);
            RoundedUtils.drawRoundTextured(artX, artY, artSize, artSize, 11.0F, 1.0F);
        } else {
            RoundedUtils.drawRound(artX, artY, artSize, artSize, 11.0F, new Color(255, 128, 149, 255));
        }

        float infoY = artY + artSize + 10.0F;
        Font font19 = FontRepository.getFont("inter-bold", 19.0F);
        Font font13 = FontRepository.getFont("inter-medium", 13.5F);
        String title = this.getTrackName() != null ? this.getTrackName() : "Unknown";
        String artist = this.getArtistName() != null ? this.getArtistName() : "Unknown";
        font19.draw(title, artX, infoY, Color.WHITE.getRGB());
        font13.draw(artist, artX, infoY + 11.0F, new Color(255, 255, 255, 147).getRGB());
    }

    private void downloadAlbumArt() {
        String currentMbid = this.getCurrentMbid();
        String albumUrl = this.getAlbumUrl();
        if (currentMbid != null && !currentMbid.equals(this.lastDownloadedId)) {
            this.downloadedCover = false;
            this.lastDownloadedId = currentMbid;
            if (albumUrl != null && !albumUrl.isEmpty()) {
                ThreadDownloadImageData albumCover = new ThreadDownloadImageData(
                    null,
                    albumUrl,
                    null,
                    new IImageBuffer() {
                        public BufferedImage func_78432_a(BufferedImage image) {
                            SpotifyMod.this.imageColor = SpotifyMod.averageColor(
                                image, image.getWidth(), image.getHeight(), 1
                            );
                            SpotifyMod.this.downloadedCover = true;
                            return image;
                        }

                        public void func_152634_a() {
                        }
                    }
                );
                mc.func_110434_K()
                    .func_110579_a(
                        this.currentAlbumCover = new ResourceLocation("lastfmAlbums/" + System.currentTimeMillis()),
                        albumCover
                    );
            }
        }
    }

    public void saveCredentials() {
        JsonObject keyObject = new JsonObject();
        keyObject.addProperty("user", this.lastFmUser.getValue());
        keyObject.addProperty("key", this.lastFmApiKey.getValue());
        keyObject.addProperty("spotify_client_id", this.spotifyClientId.getValue());
        keyObject.addProperty("spotify_client_secret", this.spotifyClientSecret.getValue());

        try {
            Writer writer = new BufferedWriter(new FileWriter(LASTFM_CREDS_DIR));
            GSON.toJson(keyObject, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadCredentials() {
        try {
            JsonObject fileContent = new JsonParser().parse(new FileReader(LASTFM_CREDS_DIR)).getAsJsonObject();
            if (fileContent.has("user")) {
                this.lastFmUser.setValue(fileContent.get("user").getAsString());
            }

            if (fileContent.has("key")) {
                this.lastFmApiKey.setValue(fileContent.get("key").getAsString());
            }

            if (fileContent.has("spotify_client_id")) {
                this.spotifyClientId.setValue(fileContent.get("spotify_client_id").getAsString());
            }

            if (fileContent.has("spotify_client_secret")) {
                this.spotifyClientSecret.setValue(fileContent.get("spotify_client_secret").getAsString());
            }
        } catch (FileNotFoundException var3) {
        }
    }

    public static Color averageColor(BufferedImage image, int width, int height, int pixelStep) {
        int[] color = new int[3];
        int count = 0;
        int i = 0;

        while (i < width) {
            for (int j = 0; j < height; j += pixelStep) {
                Color pixel = new Color(image.getRGB(i, j));
                color[0] += pixel.getRed();
                color[1] += pixel.getGreen();
                color[2] += pixel.getBlue();
                count++;
            }

            i += pixelStep;
        }

        return new Color(color[0] / count, color[1] / count, color[2] / count);
    }
}
