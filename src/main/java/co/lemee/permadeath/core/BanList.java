package co.lemee.permadeath.core;

import co.lemee.permadeath.PermaDeathMod;
import co.lemee.permadeath.helpers.MessageParser;
import co.lemee.permadeath.helpers.ModHelper;
import com.google.common.io.Files;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.time.DateUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

public class BanList {
    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
    private final UserBanList userBanList;

    public BanList(UserBanList banList) {
        this.userBanList = banList;
    }

    private static GameProfile createGameProfile(JsonObject json) {
        if (json.has("uuid") && json.has("name")) {
            String s = json.get("uuid").getAsString();

            UUID uuid;
            try {
                uuid = UUID.fromString(s);
            } catch (Throwable var4) {
                return null;
            }

            return new GameProfile(uuid, json.get("name").getAsString());
        } else {
            return null;
        }
    }

    public void addToBanList(MinecraftServer server, GameProfile deadPlayer, Date expire, String reason) {
        if (!userBanList.isBanned(deadPlayer)) {
            ServerPlayer serverplayer = server.getPlayerList().getPlayer(deadPlayer.getId());
            UserBanListEntry entry = new UserBanListEntry(deadPlayer, new Date(), ModHelper.getModNameForModId(PermaDeathMod.MOD_ID), expire, reason);
            userBanList.add(entry);
            LocalDateTime ldtCurr = LocalDateTime.ofInstant(new Date().toInstant(), ZoneId.systemDefault());
            LocalDateTime ldtExpire = LocalDateTime.ofInstant(expire.toInstant(), ZoneId.systemDefault());

            Component component = MessageParser.banMessage(reason,
                    MessageParser.getTimeRemaining(ldtCurr, ldtExpire));
            assert serverplayer != null;
            serverplayer.connection.disconnect(component);
        }
    }

    public void removeBanIfTimeExpire() {
        Date currDate = new Date();
        userBanList.getEntries().removeIf(entry -> (entry.getExpires() != null && currDate.after((entry.getExpires()))));
    }

    public void addBan(String deadPlayerName, int days) {
        GameProfile bannedUserProfile = this.getBannedPlayers().stream().filter(entry -> entry.getName().equalsIgnoreCase(deadPlayerName)).findFirst().orElse(null);
        if (bannedUserProfile == null) {
            PermaDeathMod.LOGGER.error("No corresponding banned user profile found for " + deadPlayerName);
            return;
        }
        UserBanListEntry bannedUser = this.userBanList.get(bannedUserProfile);
        if (bannedUser == null) {
            PermaDeathMod.LOGGER.error("No corresponding banned user found for " + deadPlayerName);
            return;
        }
        Date currentExpiresDate = Optional.ofNullable(bannedUser.getExpires()).orElse(new Date());
        userBanList.remove(bannedUser);
        userBanList.add(new UserBanListEntry(bannedUserProfile, bannedUser.getCreated(), ModHelper.getModNameForModId(PermaDeathMod.MOD_ID), DateUtils.addDays(currentExpiresDate, days), bannedUser.getReason()));
    }

    public void subBan(String deadPlayerName, int days) {
        this.addBan(deadPlayerName, -days);
        this.removeBanIfTimeExpire();
    }

    public Collection<GameProfile> getBannedPlayers() {
        File userBanFile = this.userBanList.getFile();
        Collection<GameProfile> result = new ArrayList<>();
        if (userBanFile.exists()) {
            try (BufferedReader bufferedreader = Files.newReader(userBanFile, StandardCharsets.UTF_8)) {
                JsonArray jsonarray = GSON.fromJson(bufferedreader, JsonArray.class);
                if (jsonarray != null) {
                    for (JsonElement jsonelement : jsonarray) {
                        JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "entry");
                        GameProfile profile = createGameProfile(jsonobject);
                        if (profile != null) {
                            result.add(profile);
                        }
                    }
                }
            } catch (IOException ioexception) {
                PermaDeathMod.LOGGER.error(ioexception.getMessage());
            }
        }
        return result;
    }

    public void removeAll() {
        userBanList.getEntries().clear();
    }
}
