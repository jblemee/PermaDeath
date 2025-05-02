package co.lemee.permadeath.helpers;

import co.lemee.permadeath.config.Config;
import co.lemee.permadeath.PermaDeathMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MessageParser {

    public static String deathReasonMessage(ServerPlayer deadPlayer, DamageSource source) {
        String deathMessage = source.getLocalizedDeathMessage(deadPlayer).getString();
        return deathMessage
                .replaceFirst(deadPlayer.getName().getString(), "You")
                .replaceFirst("was", "were");
    }

    public static Component banMessage(String reason, String expire) {
        String message = String.format( """
                §4§lYou died!§r
                Cause of death: §e%s§r
                Ban expires in: §e%s§r
                """,
                reason, expire);
        return Component.translatable(message);
    }

    public static Component firstTimeMessage(ServerPlayer joinedPlayer) {
        String modName = ModHelper.getModNameForModId(PermaDeathMod.MOD_ID);
        String message = String.format("[%s] §bWelcome %s! This server is currently running §4%s§r§b. Upon death, you will be banned for §6%s§r§b.",
                modName, joinedPlayer.getName().getString(), modName, getBanTimeFromConfig());
        return Component.translatable(message);
    }

    public static String getTimeRemaining(LocalDateTime currentDate, LocalDateTime expireDate) {
        long days = currentDate.until(expireDate, ChronoUnit.DAYS);
        currentDate = currentDate.plusDays(days);
        long hours = currentDate.until(expireDate, ChronoUnit.HOURS);
        currentDate = currentDate.plusHours(hours);
        long minutes = currentDate.until(expireDate, ChronoUnit.MINUTES);
        currentDate = currentDate.plusMinutes(minutes);
        long seconds = currentDate.until(expireDate, ChronoUnit.SECONDS);

        Long[] concatenatedDate = {seconds, minutes, hours, days};
        String[] timeUnits = {" second(s)", " minute(s), ", " hour(s), ", " day(s), "};

        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < concatenatedDate.length; i++) {
            Long time = concatenatedDate[i];
            if (i != 0 && time == 0) {
                break;
            }
            toReturn.insert(0, timeUnits[i]);
            toReturn.insert(0, concatenatedDate[i]);
        }
        return String.valueOf(toReturn);
    }

    public static String getBanTimeFromConfig() {
        long weeks = Config.CONFIG.weekTime.get();
        long days = Config.CONFIG.dayTime.get();
        long hours = Config.CONFIG.hourTime.get();
        long minutes = Config.CONFIG.minuteTime.get();

        Long[] concatenatedDate = {minutes, hours, days, weeks};
        String[] timeUnits = {" minute(s)", " hour(s)", " day(s)", " week(s)"};

        boolean firstPassed = false;

        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < concatenatedDate.length; i++) {
            Long time = concatenatedDate[i];
            if (time == 0) {
                continue;
            }
            if (firstPassed) {
                toReturn.insert(0, ", ");
            }
            toReturn.insert(0, timeUnits[i]);
            toReturn.insert(0, concatenatedDate[i]);

            firstPassed = true;
        }

        return String.valueOf(toReturn);
    }
}
