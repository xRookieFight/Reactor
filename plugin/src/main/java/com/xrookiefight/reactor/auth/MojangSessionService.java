package com.xrookiefight.reactor.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.raphimc.netminecraft.netty.crypto.CryptUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @author xRookieFight
 * @since 2026-07-20
 */
@Slf4j
public class MojangSessionService {

    private static final String DEFAULT_HAS_JOINED_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";

    @Getter
    private final KeyPair keyPair = CryptUtil.generateKeyPair();

    private final String hasJoinedUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public MojangSessionService() {
        this(DEFAULT_HAS_JOINED_URL);
    }

    public MojangSessionService(String hasJoinedUrl) {
        this.hasJoinedUrl = hasJoinedUrl;
    }

    public CompletableFuture<GameProfile> hasJoined(String username, String serverIdHash) {
        final String url = (hasJoinedUrl + "?username=%s&serverId=%s").formatted(
                URLEncoder.encode(username, StandardCharsets.UTF_8),
                URLEncoder.encode(serverIdHash, StandardCharsets.UTF_8)
        );

        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200 || response.body() == null || response.body().isEmpty()) {
                return null;
            }
            try {
                JsonObject profile = JsonParser.parseString(response.body()).getAsJsonObject();
                UUID uuid = undash(profile.get("id").getAsString());
                String name = profile.get("name").getAsString();
                return new GameProfile(uuid, name);
            } catch (Exception e) {
                log.warn("Failed to parse session server response", e);
                return null;
            }
        });
    }

    private UUID undash(String id) {
        return UUID.fromString(id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        ));
    }

    public record GameProfile(UUID uuid, String name) {
    }
}
