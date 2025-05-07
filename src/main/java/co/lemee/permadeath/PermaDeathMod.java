package co.lemee.permadeath;

import co.lemee.permadeath.commands.BannedPlayerSuggest;
import co.lemee.permadeath.config.Config;
import co.lemee.permadeath.core.BanList;
import co.lemee.permadeath.helpers.DateTimeCalculator;
import co.lemee.permadeath.helpers.MessageParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.Date;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

@Mod(PermaDeathMod.MOD_ID)
public class PermaDeathMod {
    public static final String MOD_ID = "permadeath";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private MinecraftServer server;
    private BanList banList;

    public PermaDeathMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for mod loading
        modEventBus.addListener(this::commonSetup);

        //Register the config
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.CONFIG_SPEC);

        // Register ourselves for server and other game events we are interested in
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Perma Death COMMON SETUP");

    }

    @SubscribeEvent
    public void onStart(ServerStartedEvent serverStartedEvent) {
        server = serverStartedEvent.getServer();
        banList = new BanList(server.getPlayerList().getBans());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        boolean permaDeathOn = Config.CONFIG.weekTime.get() != 0 || Config.CONFIG.dayTime.get() != 0 ||
                Config.CONFIG.hourTime.get() != 0 || Config.CONFIG.minuteTime.get() != 0;
        if (!server.isSingleplayer() && permaDeathOn) {
            event.getEntity().getPersistentData().putBoolean(MOD_ID + "joinedBefore", true);
            event.getEntity().displayClientMessage(
                    MessageParser.firstTimeMessage((ServerPlayer) event.getEntity()), false
            );
            LOGGER.info("Sent welcome message to {}", event.getEntity().getName().getString());
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        if (this.server != null) {
            banList.removeBanIfTimeExpire();
        }
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeath(LivingDeathEvent event) {
        boolean permaDeathOn = Config.CONFIG.weekTime.get() != 0 || Config.CONFIG.dayTime.get() != 0 ||
                Config.CONFIG.hourTime.get() != 0 || Config.CONFIG.minuteTime.get() != 0;
        if (!event.getEntity().getCommandSenderWorld().isClientSide() &&
                event.getEntity() instanceof ServerPlayer deadPlayer &&
                !server.isSingleplayer() && permaDeathOn) {
            String reason = MessageParser.deathReasonMessage(deadPlayer, event.getSource());
            Date expire = DateTimeCalculator.getExpiryDate(
                    Config.CONFIG.weekTime.get(),
                    Config.CONFIG.dayTime.get(),
                    Config.CONFIG.hourTime.get(),
                    Config.CONFIG.minuteTime.get()
            );
            banList.addToBanList(server, deadPlayer.getGameProfile(), expire, reason);
        }
    }

    @SubscribeEvent
    public void onCommandRegistration(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands");

        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        BannedPlayerSuggest bannedPlayerSuggest = new BannedPlayerSuggest();
        dispatcher.register(Commands.literal("pd").requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                        .then(
                                Commands.literal("add").then(
                                        Commands.argument("player", StringArgumentType.word())
                                                .suggests(bannedPlayerSuggest)
                                                .then(
                                                        Commands.argument("days", integer(0, 999)).executes(context -> {
                                                            this.banList.addBan(StringArgumentType.getString(context, "player"), IntegerArgumentType.getInteger(context, "days"));
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                                .executes(context -> {
                                                    this.banList.addBan(StringArgumentType.getString(context, "player"), 1);
                                                    return Command.SINGLE_SUCCESS;
                                                        }
                                                )

                                )
                        )
                        .then(
                                Commands.literal("sub").then(
                                        Commands.argument("player", StringArgumentType.word())
                                                .suggests(bannedPlayerSuggest)
                                                .then(
                                                        Commands.argument("days", integer(0, 999)).executes(context -> {
                                                            this.banList.subBan(StringArgumentType.getString(context, "player"), IntegerArgumentType.getInteger(context, "days"));
                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                                .executes(context -> {
                                                    this.banList.subBan(StringArgumentType.getString(context, "player"), 1);
                                                    return Command.SINGLE_SUCCESS;
                                                        }
                                                )

                                )
                        )
//                .executes()
        );
    }

}