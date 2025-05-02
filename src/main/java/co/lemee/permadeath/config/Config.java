package co.lemee.permadeath.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    //Define a field to keep the config and spec for later
    public static final Config CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.ConfigValue<Integer> weekTime;
    public final ModConfigSpec.ConfigValue<Integer> dayTime;
    public final ModConfigSpec.ConfigValue<Integer> hourTime;
    public final ModConfigSpec.ConfigValue<Integer> minuteTime;

    private Config(ModConfigSpec.Builder builder) {
        //Define each property
        //One property could be a message to log to the console when the game is initialised
        weekTime = builder.comment("The week duration of the death").define("Weeks", 0);
        dayTime = builder.comment("The day duration of the death").define("Days", 0);
        hourTime = builder.comment("The hour duration of the death").define("Hours", 0);
        minuteTime = builder.comment("The minute duration of the death").define("Minutes", 0);
    }

    static {
        Pair<Config, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(Config::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}
