package miau.util.spotify;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import miau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;

public class SpotifyAPI {
    public boolean isPlaying = false;
    public String trackName = "";
    public String artistName = "";
    public String albumUrl = "";
    public String currentMbid = "";
    private String clientId = "";
    private String clientSecret = "";
    private String accessToken = "";
    private String refreshToken = "";
    private Thread pollingThread;
    private HttpServer server;
    private static final String REDIRECT_URI = "https://api.getmiau.today/api/spotify/callback";
    private static final File SPOTIFY_CREDS_DIR = new File(Minecraft.func_71410_x().field_71412_D, "Miau_Spotify.json");

    public void startConnection(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.loadTokens();
        if (this.accessToken.isEmpty() && this.refreshToken.isEmpty()) {
            this.startAuthServer();
            this.openBrowserForAuth();
        } else {
            this.startPolling();
        }
    }

    public void stopConnection() {
        if (this.pollingThread != null && this.pollingThread.isAlive()) {
            this.pollingThread.interrupt();
        }

        if (this.server != null) {
            this.server.stop(0);
            this.server = null;
        }

        this.isPlaying = false;
    }

    private void startAuthServer() {
        try {
            if (this.server != null) {
                return;
            }

            this.server = HttpServer.create(new InetSocketAddress(8080), 0);
            this.server.createContext("/callback", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String query = exchange.getRequestURI().getQuery();
                    String response = "Spotify Authentication Complete! You can close this tab.";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    if (query != null && query.contains("code=")) {
                        String code = query.split("code=")[1].split("&")[0];
                        SpotifyAPI.this.exchangeCodeForTokens(code);
                    }
                }
            });
            this.server.setExecutor(null);
            this.server.start();
            ChatUtil.display("§aStarted local HTTPS server on port 8080 for Spotify Auth.");
        } catch (Exception e) {
            ChatUtil.display("§cFailed to start local HTTPS server for Spotify Auth: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void openBrowserForAuth() {
        try {
            String authUrl = "https://accounts.spotify.com/authorize?client_id="
                + URLEncoder.encode(this.clientId, "UTF-8")
                + "&response_type=code&redirect_uri="
                + URLEncoder.encode("https://api.getmiau.today/api/spotify/callback", "UTF-8")
                + "&scope="
                + URLEncoder.encode("user-read-currently-playing user-read-playback-state", "UTF-8");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(authUrl));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void exchangeCodeForTokens(String code) {
        try {
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String authHeader = Base64.getEncoder()
                .encodeToString((this.clientId + ":" + this.clientSecret).getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + authHeader);
            conn.setDoOutput(true);
            String body = "grant_type=authorization_code&code="
                + code
                + "&redirect_uri="
                + URLEncoder.encode("https://api.getmiau.today/api/spotify/callback", "UTF-8");
            OutputStream os = conn.getOutputStream();

            try {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            } catch (Throwable var12) {
                if (os != null) {
                    try {
                        os.close();
                    } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                    }
                }

                throw var12;
            }

            if (os != null) {
                os.close();
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }

                in.close();
                JsonObject json = new JsonParser().parse(content.toString()).getAsJsonObject();
                this.accessToken = json.get("access_token").getAsString();
                if (json.has("refresh_token")) {
                    this.refreshToken = json.get("refresh_token").getAsString();
                }

                this.saveTokens();
                ChatUtil.display("§aSuccessfully authenticated with Spotify!");
                if (this.server != null) {
                    this.server.stop(0);
                    this.server = null;
                }

                this.startPolling();
            } else {
                ChatUtil.display("§cFailed to exchange code for tokens. Status: " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshAccessToken() {
        if (!this.refreshToken.isEmpty()) {
            try {
                URL url = new URL("https://accounts.spotify.com/api/token");
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                String authHeader = Base64.getEncoder()
                    .encodeToString((this.clientId + ":" + this.clientSecret).getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + authHeader);
                conn.setDoOutput(true);
                String body = "grant_type=refresh_token&refresh_token=" + this.refreshToken;
                OutputStream os = conn.getOutputStream();

                try {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                } catch (Throwable var11) {
                    if (os != null) {
                        try {
                            os.close();
                        } catch (Throwable var10) {
                            var11.addSuppressed(var10);
                        }
                    }

                    throw var11;
                }

                if (os != null) {
                    os.close();
                }

                int status = conn.getResponseCode();
                if (status == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder content = new StringBuilder();

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    in.close();
                    JsonObject json = new JsonParser().parse(content.toString()).getAsJsonObject();
                    this.accessToken = json.get("access_token").getAsString();
                    this.saveTokens();
                } else {
                    ChatUtil.display("§cFailed to refresh Spotify token. Re-authenticating...");
                    this.accessToken = "";
                    this.refreshToken = "";
                    this.saveTokens();
                    this.startAuthServer();
                    this.openBrowserForAuth();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startPolling() {
        if (this.pollingThread != null && this.pollingThread.isAlive()) {
            this.pollingThread.interrupt();
        }

        this.pollingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    this.pollSpotify();
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.pollingThread.start();
        ChatUtil.display("§aConnected to Spotify API...");
    }

    private void pollSpotify() throws Exception {
        if (!this.accessToken.isEmpty()) {
            URL url = new URL("https://api.spotify.com/v1/me/player/currently-playing");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + this.accessToken);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int status = conn.getResponseCode();
            if (status == 401) {
                this.refreshAccessToken();
            } else {
                if (status == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder content = new StringBuilder();

                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    in.close();
                    conn.disconnect();
                    this.parseResponse(content.toString());
                } else if (status == 204) {
                    this.isPlaying = false;
                    conn.disconnect();
                } else {
                    this.isPlaying = false;
                    conn.disconnect();
                }
            }
        }
    }

    private void parseResponse(String responseBody) {
        try {
            if (responseBody.isEmpty()) {
                this.isPlaying = false;
                return;
            }

            JsonObject json = new JsonParser().parse(responseBody).getAsJsonObject();
            if (json.has("is_playing")) {
                this.isPlaying = json.get("is_playing").getAsBoolean();
            }

            if (json.has("item") && !json.get("item").isJsonNull()) {
                JsonObject item = json.getAsJsonObject("item");
                if (item.has("name")) {
                    this.trackName = item.get("name").getAsString();
                }

                if (item.has("artists")) {
                    JsonArray artists = item.getAsJsonArray("artists");
                    if (artists.size() > 0) {
                        this.artistName = artists.get(0).getAsJsonObject().get("name").getAsString();
                    }
                }

                if (item.has("album")) {
                    JsonObject album = item.getAsJsonObject("album");
                    if (album.has("images")) {
                        JsonArray images = album.getAsJsonArray("images");
                        if (images.size() > 0) {
                            this.albumUrl = images.get(0).getAsJsonObject().get("url").getAsString();
                        }
                    }
                }

                if (item.has("id")) {
                    this.currentMbid = item.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.isPlaying = false;
        }
    }

    private void saveTokens() {
        JsonObject tokenObj = new JsonObject();
        tokenObj.addProperty("access_token", this.accessToken);
        tokenObj.addProperty("refresh_token", this.refreshToken);

        try {
            Writer writer = new BufferedWriter(new FileWriter(SPOTIFY_CREDS_DIR));

            try {
                new Gson().toJson(tokenObj, writer);
            } catch (Throwable var6) {
                try {
                    writer.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }

                throw var6;
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTokens() {
        if (SPOTIFY_CREDS_DIR.exists()) {
            try {
                Reader reader = new FileReader(SPOTIFY_CREDS_DIR);

                try {
                    JsonObject tokenObj = new JsonParser().parse(reader).getAsJsonObject();
                    if (tokenObj.has("access_token")) {
                        this.accessToken = tokenObj.get("access_token").getAsString();
                    }

                    if (tokenObj.has("refresh_token")) {
                        this.refreshToken = tokenObj.get("refresh_token").getAsString();
                    }
                } catch (Throwable var5) {
                    try {
                        reader.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }

                    throw var5;
                }

                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
