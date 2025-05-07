package co.lemee.permadeath.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class BannedPlayerSuggest implements SuggestionProvider<CommandSourceStack> {

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        if (!(context.getSource() instanceof CommandSourceStack commandSourceStack)) {
            return Suggestions.empty();
        } else {
            return SharedSuggestionProvider.suggest(
                    commandSourceStack.getServer().getPlayerList().getBans().getUserList(),
                    builder
            );
        }
    }
}

