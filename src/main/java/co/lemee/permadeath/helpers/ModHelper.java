package co.lemee.permadeath.helpers;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class ModHelper {
    private static final Map<String, String> cache = new HashMap<>();

    public static String getModNameForModId(String modId) {
        return cache.computeIfAbsent(modId, ModHelper::computeModNameForModId);
    }

    private static String computeModNameForModId(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(ModContainer::getModInfo)
                .map(IModInfo::getDisplayName)
                .orElseGet(() -> StringUtils.capitalize(modId));
    }
}