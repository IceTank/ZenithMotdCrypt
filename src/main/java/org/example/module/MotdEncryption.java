package org.example.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.module.api.Module;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.network.codec.PacketHandlerStateCodec;
import com.zenith.network.server.ServerSession;
import com.zenith.network.server.ZenithServerInfoBuilder;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.example.EncryptionUtil;
import org.example.MotdCryptPlugin;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.status.PlayerInfo;
import org.geysermc.mcprotocollib.protocol.data.status.ServerStatusInfo;
import org.geysermc.mcprotocollib.protocol.data.status.VersionInfo;
import org.geysermc.mcprotocollib.protocol.packet.status.clientbound.ClientboundStatusResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.status.serverbound.ServerboundStatusRequestPacket;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.zenith.Globals.*;
import static org.example.MotdCryptPlugin.LOG;

public class MotdEncryption extends Module {
    private static final String motdMM = """
            <encrypted_motd>
            """;
    private static final String ENCRYPTION_INDICATOR = "##EncryptionBegin##";

    private final Cache<String, ServerStatusInfo> infoCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(CONFIG.server.ping.responseCacheSeconds))
            .maximumSize(10)
            .build();

    @Override
    public boolean enabledSetting() {
        return MotdCryptPlugin.PLUGIN_CONFIG.motdEncryption;
    }

    @Override
    public @Nullable PacketHandlerCodec registerServerPacketHandlerCodec() {
        return PacketHandlerCodec.serverBuilder()
                .setPriority(3)
                .setId("motd-encryption-handler")
                .state(ProtocolState.STATUS, PacketHandlerStateCodec.serverBuilder()
                        .inbound(ServerboundStatusRequestPacket.class, new StatusRequestHandler())
                        .build()).build();
    }

    private String componentToString(Component component) {
        return ComponentSerializer.serializeJson(component);
    }

    /**
     * Builds a JSON string with all the ServerStatusInfo data
     * @param info
     * @return
     */
    private String serverStatusInfoToString(ServerStatusInfo info) {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.add("motd", new JsonPrimitive(componentToString(info.getDescription())));
        if (info.getPlayerInfo() != null) {
            JsonObject playerInfo = new JsonObject();
            playerInfo.add("max", new JsonPrimitive(info.getPlayerInfo().getMaxPlayers()));
            playerInfo.add("online", new JsonPrimitive(info.getPlayerInfo().getOnlinePlayers()));
            json.add("players", playerInfo);
        }
        if (info.getVersionInfo() != null) {
            JsonObject versionInfo = new JsonObject();
            versionInfo.add("name", new JsonPrimitive(info.getVersionInfo().getVersionName()));
            versionInfo.add("protocol", new JsonPrimitive(info.getVersionInfo().getProtocolVersion()));
            json.add("version", versionInfo);
        }
        json.add("icon", new JsonPrimitive(EncryptionUtil.byteToBase64(info.getIconPng())));
        json.add("isSecureChat", new JsonPrimitive(info.isEnforcesSecureChat()));
        return gson.toJson(json);
    }

    ServerStatusInfo buildEncryptedStatusInfo(@Nullable Session session) {
        String motdBody;
        ServerStatusInfo info = new ServerStatusInfo(
                ZenithServerInfoBuilder.INSTANCE.getMotd(),
                getPlayerInfo(),
                getVersionInfo(session),
                Proxy.getInstance().getServerIcon(),
                false
        );
        try {
            String cypherText = EncryptionUtil.encrypt(serverStatusInfoToString(info), MotdCryptPlugin.PLUGIN_CONFIG.encryptionConfig.password);
//            LOG.info("Encrypted MOTD: " + cypherText);
//            LOG.info("Encryption password: " + MotdCryptPlugin.PLUGIN_CONFIG.encryptionConfig.password);
            motdBody = ENCRYPTION_INDICATOR + cypherText;
        } catch (Exception e) {
            LOG.error("Failed to encrypt MOTD", e);
            motdBody = "§c[MotdCrypt] Failed to encrypt MOTD";
        }

        Component motdComponent = ComponentSerializer.minimessage(
                motdMM,
                Placeholder.unparsed("encrypted_motd", motdBody)
        );

        return new ServerStatusInfo(
                motdComponent,
                new PlayerInfo(0, 0, Collections.emptyList()),
                getVersionInfo(session),
                new byte[0],
                false
        );
    }

    private VersionInfo getVersionInfo(@Nullable Session session) {
        int protocolId = CONFIG.server.viaversion.enabled && session instanceof ServerSession
                ? ((ServerSession) session).getProtocolVersionId()
                : MinecraftCodec.CODEC.getProtocolVersion();
        return new VersionInfo("ZenithProxy", protocolId);
    }

    public GameProfile[] getOnlinePlayerProfiles() {
        try {
            var connections = Proxy.getInstance().getActiveConnections().getArray();
            var result = new GameProfile[connections.length];
            for (int i = 0; i < connections.length; i++) {
                var connection = connections[i];
                result[i] = connection.getProfileCache().getProfile();
            }
            return result;
        } catch (final Throwable e) {
            return new GameProfile[0];
        }
    }

    private PlayerInfo getPlayerInfo() {
        var onlinePlayerCount = CONFIG.server.ping.onlinePlayerCount
                ? Proxy.getInstance().getActiveConnections().size()
                : 0;
        if (CONFIG.server.ping.onlinePlayers) {
            return new PlayerInfo(
                    CONFIG.server.ping.maxPlayers,
                    onlinePlayerCount,
                    List.of(getOnlinePlayerProfiles())
            );
        } else {
            return new PlayerInfo(
                    CONFIG.server.ping.maxPlayers,
                    onlinePlayerCount,
                    Collections.emptyList()
            );
        }
    }

    public @Nullable ServerStatusInfo buildInfo(@Nullable Session session) {
        if (!MotdCryptPlugin.PLUGIN_CONFIG.motdEncryption) {
            return ZenithServerInfoBuilder.INSTANCE.buildInfo(session);
        }
        if (!CONFIG.server.ping.enabled) return null;
        if (CONFIG.server.ping.responseCaching) {
            var cacheKey = getSessionCacheKey(session);
            try {
                // building the server status here can be expensive
                // due to accessing player caches, active connections, etc
                // its possible someone could DoS a server pretty easily
                return infoCache.get(cacheKey, () -> buildEncryptedStatusInfo(session));
            } catch (ExecutionException e) {
                SERVER_LOG.debug("Failed to build server info for {}", cacheKey, e);
                return null;
            }
        } else return buildEncryptedStatusInfo(session);
    }

    private String getSessionCacheKey(@Nullable Session session) {
        if (session != null && CONFIG.server.viaversion.enabled) { // our response has a different protocol version for each connection (mirroring them)
            String ip = session.getRemoteAddress().toString();
            if (ip.contains("/")) ip = ip.substring(ip.indexOf("/") + 1);
            if (ip.contains(":")) ip = ip.substring(0, ip.indexOf(":"));
            return ip;
        }
        return "";
    }

    private static class StatusRequestHandler implements PacketHandler<ServerboundStatusRequestPacket, ServerSession> {
        @Override
        public ServerboundStatusRequestPacket apply(ServerboundStatusRequestPacket packet, ServerSession session) {
            if (CONFIG.server.ping.logPings)
                SERVER_LOG.info("[Ping] Request from: {} [{}] to: {}:{}",
                        session.getRemoteAddress(),
                        ProtocolVersion.getProtocol(session.getProtocolVersionId()).getName(),
                        session.getConnectingServerAddress(),
                        session.getConnectingServerPort());
            ServerStatusInfo info = MODULE.get(MotdEncryption.class).buildInfo(session);
            if (info == null) session.disconnect("bye");
            else session.send(new ClientboundStatusResponsePacket(info));
            return null;
        }
    }
}
