package me.ksyz.accountmanager.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.ksyz.accountmanager.utils.SSLUtils;
import net.minecraft.util.Session;
import net.minecraft.util.Session.Type;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public final class MicrosoftAuth {
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
        .setConnectionRequestTimeout(30000)
        .setConnectTimeout(30000)
        .setSocketTimeout(30000)
        .build();
    public static String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    public static String SCOPE = "XboxLive.signin XboxLive.offline_access";
    private static final int PORT = 25575;

    private static CloseableHttpClient createTrustedHttpClient() {
        try {
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(
                SSLUtils.getSSLContext().getSocketFactory(),
                new String[]{"TLSv1.2"},
                null,
                new BrowserCompatHostnameVerifier()
            );
            return HttpClientBuilder.create().setSSLSocketFactory(sf).build();
        } catch (Exception var1) {
            return HttpClients.createDefault();
        }
    }

    private static CloseableHttpClient createTrustedHttpClientNoRedirect() {
        try {
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(
                SSLUtils.getSSLContext().getSocketFactory(),
                new String[]{"TLSv1.2"},
                null,
                new BrowserCompatHostnameVerifier()
            );
            return HttpClientBuilder.create().setSSLSocketFactory(sf).disableRedirectHandling().build();
        } catch (Exception var1) {
            return HttpClients.custom().disableRedirectHandling().build();
        }
    }

    public static URI getMSAuthLink(String state) {
        try {
            URIBuilder uriBuilder = new URIBuilder("https://login.live.com/oauth20_authorize.srf")
                .addParameter("client_id", CLIENT_ID)
                .addParameter("response_type", "code")
                .addParameter("redirect_uri", String.format("http://localhost:%d/callback", 25575))
                .addParameter("scope", SCOPE)
                .addParameter("state", state)
                .addParameter("prompt", "select_account");
            return uriBuilder.build();
        } catch (Exception e) {
            return null;
        }
    }

    public static CompletableFuture<String> acquireMSAuthCode(String state, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    HttpServer server = HttpServer.create(new InetSocketAddress(25575), 0);
                    CountDownLatch latch = new CountDownLatch(1);
                    AtomicReference<String> authCode = new AtomicReference<>(null);
                    AtomicReference<String> errorMsg = new AtomicReference<>(null);
                    server.createContext(
                        "/callback",
                        exchange -> {
                            Map<String, String> query = URLEncodedUtils.parse(
                                    exchange.getRequestURI().toString().replaceAll("/callback\\?", ""),
                                    StandardCharsets.UTF_8
                                )
                                .stream()
                                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                            if (!state.equals(query.get("state"))) {
                                errorMsg.set(
                                    String.format(
                                        "State mismatch! Expected '%s' but got '%s'.", state, query.get("state")
                                    )
                                );
                            } else if (query.containsKey("code")) {
                                authCode.set(query.get("code"));
                            } else if (query.containsKey("error")) {
                                errorMsg.set(
                                    String.format("%s: %s", query.get("error"), query.get("error_description"))
                                );
                            }

                            InputStream stream = MicrosoftAuth.class.getResourceAsStream("/callback.html");
                            byte[] response = stream != null ? IOUtils.toByteArray(stream) : new byte[0];
                            exchange.getResponseHeaders().add("Content-Type", "text/html");
                            exchange.sendResponseHeaders(200, response.length);
                            exchange.getResponseBody().write(response);
                            exchange.getResponseBody().close();
                            latch.countDown();
                        }
                    );

                    try {
                        server.start();
                        latch.await();
                        return Optional.ofNullable(authCode.get())
                            .filter(code -> !StringUtils.isBlank(code))
                            .orElseThrow(
                                () -> new Exception(
                                    Optional.ofNullable(errorMsg.get())
                                        .orElse("There was no auth code or error description present.")
                                )
                            );
                    } finally {
                        server.stop(2);
                    }
                } catch (InterruptedException e) {
                    throw new CancellationException("Microsoft auth code acquisition was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to acquire Microsoft auth code!", e);
                }
            },
            executor
        );
    }

    private static String buildCookieHeader(Map<String, String> jar) {
        StringBuilder sb = new StringBuilder();

        for (Entry<String, String> entry : jar.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }

            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }

        return sb.toString();
    }

    public static CompletableFuture<String> acquireMSAuthCodeFromCookies(Map<String, String> cookies, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClientNoRedirect();

                    String rawQuery;
                    label189: {
                        String var38;
                        try {
                            label210: {
                                String redirectUri = String.format("http://localhost:%d/callback", 25575);
                                String url = new URIBuilder("https://login.live.com/oauth20_authorize.srf")
                                    .addParameter("client_id", CLIENT_ID)
                                    .addParameter("response_type", "code")
                                    .addParameter("redirect_uri", redirectUri)
                                    .addParameter("scope", SCOPE)
                                    .addParameter("state", "cookielogin")
                                    .build()
                                    .toString();
                                Map<String, String> jar = new LinkedHashMap<>();
                                if (cookies != null) {
                                    jar.putAll(cookies);
                                }

                                if (jar.isEmpty()) {
                                    throw new Exception("No cookies were provided.");
                                }

                                HttpRequestBase request = new HttpGet(url);
                                int hop = 0;

                                int status;
                                while (true) {
                                    if (hop >= 15) {
                                        throw new Exception("Too many redirects while authenticating with cookies.");
                                    }

                                    request.setConfig(REQUEST_CONFIG);
                                    request.setHeader(
                                        "User-Agent",
                                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                    );
                                    request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml");
                                    request.setHeader("Cookie", buildCookieHeader(jar));
                                    HttpResponse res = client.execute(request);
                                    status = res.getStatusLine().getStatusCode();

                                    for (Header header : res.getHeaders("Set-Cookie")) {
                                        String[] kv = header.getValue().split(";", 2)[0].split("=", 2);
                                        if (kv.length == 2 && !kv[0].trim().isEmpty() && !StringUtils.isBlank(kv[1])) {
                                            jar.put(kv[0].trim(), kv[1].trim());
                                        }
                                    }

                                    if (status >= 300 && status < 400) {
                                        Header location = res.getFirstHeader("Location");
                                        EntityUtils.consumeQuietly(res.getEntity());
                                        if (location == null || StringUtils.isBlank(location.getValue())) {
                                            throw new Exception("Received a redirect without a location.");
                                        }

                                        URI resolved = request.getURI().resolve(location.getValue().trim());
                                        if (resolved.toString().startsWith(redirectUri)) {
                                            String rawQueryx = resolved.getRawQuery();
                                            Map<String, String> query = URLEncodedUtils.parse(
                                                    rawQueryx == null ? "" : rawQueryx, StandardCharsets.UTF_8
                                                )
                                                .stream()
                                                .collect(
                                                    Collectors.toMap(
                                                        NameValuePair::getName, NameValuePair::getValue, (a, b) -> a
                                                    )
                                                );
                                            if (StringUtils.isBlank(query.get("code"))) {
                                                if (query.containsKey("error")) {
                                                    throw new Exception(
                                                        String.format(
                                                            "%s: %s",
                                                            query.get("error"),
                                                            Optional.ofNullable(query.get("error_description"))
                                                                .orElse("")
                                                        )
                                                    );
                                                }

                                                throw new Exception("The callback did not contain an auth code.");
                                            }

                                            rawQuery = query.get("code");
                                            break label189;
                                        }

                                        request = new HttpGet(resolved.toString());
                                    } else {
                                        String body = res.getEntity() != null
                                            ? EntityUtils.toString(res.getEntity())
                                            : "";
                                        int idx = body.indexOf(redirectUri);
                                        if (idx >= 0) {
                                            int end = idx;

                                            while (
                                                end < body.length() && "\"'< >\\\r\n\t".indexOf(body.charAt(end)) < 0
                                            ) {
                                                end++;
                                            }

                                            String candidate = body.substring(idx, end).replace("&amp;", "&");

                                            try {
                                                rawQuery = URI.create(candidate).getRawQuery();
                                                Map<String, String> query = URLEncodedUtils.parse(
                                                        rawQuery == null ? "" : rawQuery, StandardCharsets.UTF_8
                                                    )
                                                    .stream()
                                                    .collect(
                                                        Collectors.toMap(
                                                            NameValuePair::getName,
                                                            NameValuePair::getValue,
                                                            (a, b) -> a
                                                        )
                                                    );
                                                if (!StringUtils.isBlank(query.get("code"))) {
                                                    var38 = query.get("code");
                                                    break label210;
                                                }

                                                if (query.containsKey("error")) {
                                                    throw new Exception(
                                                        String.format(
                                                            "%s: %s",
                                                            query.get("error"),
                                                            Optional.ofNullable(query.get("error_description"))
                                                                .orElse("")
                                                        )
                                                    );
                                                }
                                            } catch (IllegalArgumentException var20) {
                                            }
                                        }

                                        Matcher actionMatcher = Pattern.compile("action=[\"']([^\"']+)[\"']")
                                            .matcher(body);
                                        if (!actionMatcher.find()) {
                                            break;
                                        }

                                        String action = actionMatcher.group(1).replace("&amp;", "&");
                                        HttpPost post = new HttpPost(request.getURI().resolve(action).toString());
                                        List<NameValuePair> params = new ArrayList<>();
                                        Matcher inputMatcher = Pattern.compile("<input[^>]+>").matcher(body);

                                        while (inputMatcher.find()) {
                                            String input = inputMatcher.group();
                                            if (input.contains("type=\"hidden\"")
                                                || input.contains("type=hidden")
                                                || input.contains("type='hidden'")) {
                                                Matcher nameMatcher = Pattern.compile("name=[\"']([^\"']+)[\"']")
                                                    .matcher(input);
                                                Matcher valueMatcher = Pattern.compile("value=[\"']([^\"']*)[\"']")
                                                    .matcher(input);
                                                if (nameMatcher.find() && valueMatcher.find()) {
                                                    params.add(
                                                        new BasicNameValuePair(
                                                            nameMatcher.group(1),
                                                            valueMatcher.group(1).replace("&amp;", "&")
                                                        )
                                                    );
                                                }
                                            }
                                        }

                                        if (params.isEmpty()) {
                                            break;
                                        }

                                        post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                                        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
                                        request = post;
                                    }

                                    hop++;
                                }

                                throw new Exception(
                                    String.format(
                                        "Cookies are invalid or expired (interaction required, HTTP %d).", status
                                    )
                                );
                            }
                        } catch (Throwable var21) {
                            if (client != null) {
                                try {
                                    client.close();
                                } catch (Throwable var19) {
                                    var21.addSuppressed(var19);
                                }
                            }

                            throw var21;
                        }

                        if (client != null) {
                            client.close();
                        }

                        return var38;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return rawQuery;
                } catch (InterruptedException e) {
                    throw new CancellationException("Cookie authentication was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to authenticate with cookies!", e);
                }
            },
            executor
        );
    }

    public static CompletableFuture<Map<String, String>> acquireMSAccessTokens(String authCode, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClient();

                    Map var8;
                    try {
                        HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                        request.setConfig(REQUEST_CONFIG);
                        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        request.setEntity(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair("client_id", CLIENT_ID),
                                    new BasicNameValuePair("grant_type", "authorization_code"),
                                    new BasicNameValuePair("code", authCode),
                                    new BasicNameValuePair(
                                        "redirect_uri", String.format("http://localhost:%d/callback", 25575)
                                    )
                                ),
                                "UTF-8"
                            )
                        );
                        HttpResponse res = client.execute(request);
                        JsonObject json = new JsonParser()
                            .parse(EntityUtils.toString(res.getEntity()))
                            .getAsJsonObject();
                        String accessToken = Optional.ofNullable(json.get("access_token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("error")
                                        ? String.format(
                                            "%s: %s",
                                            json.get("error").getAsString(),
                                            json.get("error_description").getAsString()
                                        )
                                        : "There was no Microsoft access token or error description present."
                                )
                            );
                        String refreshToken = Optional.ofNullable(json.get("refresh_token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("error")
                                        ? String.format(
                                            "%s: %s",
                                            json.get("error").getAsString(),
                                            json.get("error_description").getAsString()
                                        )
                                        : "There was no Microsoft refresh token or error description present."
                                )
                            );
                        Map<String, String> result = new HashMap<>();
                        result.put("access_token", accessToken);
                        result.put("refresh_token", refreshToken);
                        var8 = result;
                    } catch (Throwable var10) {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (Throwable var9) {
                                var10.addSuppressed(var9);
                            }
                        }

                        throw var10;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return var8;
                } catch (InterruptedException e) {
                    throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
                }
            },
            executor
        );
    }

    public static CompletableFuture<Map<String, String>> refreshMSAccessTokens(String msToken, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClient();

                    Map var8;
                    try {
                        HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                        request.setConfig(REQUEST_CONFIG);
                        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                        request.setEntity(
                            new UrlEncodedFormEntity(
                                Arrays.asList(
                                    new BasicNameValuePair("client_id", CLIENT_ID),
                                    new BasicNameValuePair("grant_type", "refresh_token"),
                                    new BasicNameValuePair("refresh_token", msToken),
                                    CLIENT_ID.equals("00000000402b5328")
                                        ? new BasicNameValuePair("scope", SCOPE)
                                        : new BasicNameValuePair(
                                            "redirect_uri", String.format("http://localhost:%d/callback", 25575)
                                        )
                                ),
                                "UTF-8"
                            )
                        );
                        HttpResponse res = client.execute(request);
                        JsonObject json = new JsonParser()
                            .parse(EntityUtils.toString(res.getEntity()))
                            .getAsJsonObject();
                        String accessToken = Optional.ofNullable(json.get("access_token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("error")
                                        ? String.format(
                                            "%s: %s",
                                            json.get("error").getAsString(),
                                            json.get("error_description").getAsString()
                                        )
                                        : "There was no Microsoft access token or error description present."
                                )
                            );
                        String refreshToken = Optional.ofNullable(json.get("refresh_token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("error")
                                        ? String.format(
                                            "%s: %s",
                                            json.get("error").getAsString(),
                                            json.get("error_description").getAsString()
                                        )
                                        : "There was no Microsoft refresh token or error description present."
                                )
                            );
                        Map<String, String> result = new HashMap<>();
                        result.put("access_token", accessToken);
                        result.put("refresh_token", refreshToken);
                        var8 = result;
                    } catch (Throwable var10) {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (Throwable var9) {
                                var10.addSuppressed(var9);
                            }
                        }

                        throw var10;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return var8;
                } catch (InterruptedException e) {
                    throw new CancellationException("Microsoft access tokens acquisition was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to acquire Microsoft access tokens!", e);
                }
            },
            executor
        );
    }

    public static CompletableFuture<String> acquireXboxAccessToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClient();

                    String var7;
                    try {
                        HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
                        JsonObject entity = new JsonObject();
                        JsonObject properties = new JsonObject();
                        properties.addProperty("AuthMethod", "RPS");
                        properties.addProperty("SiteName", "user.auth.xboxlive.com");
                        properties.addProperty(
                            "RpsTicket",
                            CLIENT_ID.equals("00000000402b5328") ? accessToken : String.format("d=%s", accessToken)
                        );
                        entity.add("Properties", properties);
                        entity.addProperty("RelyingParty", "http://auth.xboxlive.com");
                        entity.addProperty("TokenType", "JWT");
                        request.setConfig(REQUEST_CONFIG);
                        request.setHeader("Content-Type", "application/json");
                        request.setEntity(new StringEntity(entity.toString()));
                        HttpResponse res = client.execute(request);
                        JsonObject json = res.getStatusLine().getStatusCode() == 200
                            ? new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject()
                            : new JsonObject();
                        var7 = Optional.ofNullable(json.get("Token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("XErr")
                                        ? String.format(
                                            "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                        )
                                        : "There was no access token or error description present."
                                )
                            );
                    } catch (Throwable var9) {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (Throwable var8) {
                                var9.addSuppressed(var8);
                            }
                        }

                        throw var9;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return var7;
                } catch (InterruptedException e) {
                    throw new CancellationException("Xbox Live access token acquisition was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to acquire Xbox Live access token!", e);
                }
            },
            executor
        );
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(String accessToken, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClient();

                    Map var8;
                    try {
                        HttpPost request = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
                        JsonObject entity = new JsonObject();
                        JsonObject properties = new JsonObject();
                        JsonArray userTokens = new JsonArray();
                        userTokens.add(new JsonPrimitive(accessToken));
                        properties.addProperty("SandboxId", "RETAIL");
                        properties.add("UserTokens", userTokens);
                        entity.add("Properties", properties);
                        entity.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                        entity.addProperty("TokenType", "JWT");
                        request.setConfig(REQUEST_CONFIG);
                        request.setHeader("Content-Type", "application/json");
                        request.setEntity(new StringEntity(entity.toString()));
                        HttpResponse res = client.execute(request);
                        JsonObject json = res.getStatusLine().getStatusCode() == 200
                            ? new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject()
                            : new JsonObject();
                        var8 = Optional.ofNullable(json.get("Token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .map(
                                token -> {
                                    String uhs = json.get("DisplayClaims")
                                        .getAsJsonObject()
                                        .get("xui")
                                        .getAsJsonArray()
                                        .get(0)
                                        .getAsJsonObject()
                                        .get("uhs")
                                        .getAsString();
                                    Map<String, String> result = new HashMap<>();
                                    result.put("Token", token);
                                    result.put("uhs", uhs);
                                    return result;
                                }
                            )
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("XErr")
                                        ? String.format(
                                            "%s: %s", json.get("XErr").getAsString(), json.get("Message").getAsString()
                                        )
                                        : "There was no access token or error description present."
                                )
                            );
                    } catch (Throwable var10) {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (Throwable var9) {
                                var10.addSuppressed(var9);
                            }
                        }

                        throw var10;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return var8;
                } catch (InterruptedException e) {
                    throw new CancellationException("Xbox Live XSTS token acquisition was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to acquire Xbox Live XSTS token!", e);
                }
            },
            executor
        );
    }

    public static CompletableFuture<String> acquireMCAccessToken(String xstsToken, String userHash, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClient();

                    String var6;
                    try {
                        HttpPost request = new HttpPost(
                            URI.create("https://api.minecraftservices.com/authentication/login_with_xbox")
                        );
                        request.setConfig(REQUEST_CONFIG);
                        request.setHeader("Content-Type", "application/json");
                        request.setEntity(
                            new StringEntity(
                                String.format("{\"identityToken\": \"XBL3.0 x=%s;%s\"}", userHash, xstsToken)
                            )
                        );
                        HttpResponse res = client.execute(request);
                        JsonObject json = new JsonParser()
                            .parse(EntityUtils.toString(res.getEntity()))
                            .getAsJsonObject();
                        var6 = Optional.ofNullable(json.get("access_token"))
                            .<String>map(JsonElement::getAsString)
                            .filter(token -> !StringUtils.isBlank(token))
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("error")
                                        ? String.format(
                                            "%s: %s",
                                            json.get("error").getAsString(),
                                            json.get("errorMessage").getAsString()
                                        )
                                        : "There was no access token or error description present."
                                )
                            );
                    } catch (Throwable var8) {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (Throwable var7) {
                                var8.addSuppressed(var7);
                            }
                        }

                        throw var8;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return var6;
                } catch (InterruptedException e) {
                    throw new CancellationException("Minecraft access token acquisition was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to acquire Minecraft access token!", e);
                }
            },
            executor
        );
    }

    public static CompletableFuture<Session> login(String mcToken, Executor executor) {
        return CompletableFuture.supplyAsync(
            () -> {
                try {
                    CloseableHttpClient client = createTrustedHttpClient();

                    Session var5;
                    try {
                        HttpGet request = new HttpGet(URI.create("https://api.minecraftservices.com/minecraft/profile"));
                        request.setConfig(REQUEST_CONFIG);
                        request.setHeader("Authorization", "Bearer " + mcToken);
                        HttpResponse res = client.execute(request);
                        JsonObject json = new JsonParser()
                            .parse(EntityUtils.toString(res.getEntity()))
                            .getAsJsonObject();
                        var5 = Optional.ofNullable(json.get("id"))
                            .<String>map(JsonElement::getAsString)
                            .filter(uuid -> !StringUtils.isBlank(uuid))
                            .map(
                                uuid -> new Session(
                                    json.get("name").getAsString(), uuid, mcToken, Type.MOJANG.toString()
                                )
                            )
                            .orElseThrow(
                                () -> new Exception(
                                    json.has("error")
                                        ? String.format(
                                            "%s: %s",
                                            json.get("error").getAsString(),
                                            json.get("errorMessage").getAsString()
                                        )
                                        : "There was no profile or error description present."
                                )
                            );
                    } catch (Throwable var7) {
                        if (client != null) {
                            try {
                                client.close();
                            } catch (Throwable var6) {
                                var7.addSuppressed(var6);
                            }
                        }

                        throw var7;
                    }

                    if (client != null) {
                        client.close();
                    }

                    return var5;
                } catch (InterruptedException e) {
                    throw new CancellationException("Minecraft profile fetching was cancelled!");
                } catch (Exception e) {
                    throw new CompletionException("Unable to fetch Minecraft profile!", e);
                }
            },
            executor
        );
    }
}
