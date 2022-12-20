package com.github.smuddgge.leaf.commands;

import com.github.smuddgge.leaf.MessageManager;
import com.github.smuddgge.leaf.configuration.ConfigCommands;
import com.github.smuddgge.leaf.configuration.ConfigMessages;
import com.github.smuddgge.leaf.configuration.squishyyaml.ConfigurationSection;
import com.github.smuddgge.leaf.datatype.User;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command.
 */
public class Command implements SimpleCommand {

    private final String identifier;

    private final BaseCommandType commandType;

    /**
     * Used to create a command.
     *
     * @param identifier The command's identifier in the configuration.
     */
    public Command(String identifier, BaseCommandType commandType) {
        this.identifier = identifier;
        this.commandType = commandType;
    }

    /**
     * Used to get the command's syntax.
     *
     * @return The command's syntax.
     */
    public String getSyntax() {
        return this.commandType.getSyntax();
    }

    /**
     * Used to get the tab suggestions.
     *
     * @param user The user completing the command.
     * @return The command's argument suggestions.
     */
    public CommandSuggestions getSuggestions(User user) {
        return this.commandType.getSuggestions(user);
    }

    /**
     * Executed when the command is run in the console.
     *
     * @param arguments The arguments given in the command.
     * @return The command's status.
     */
    public CommandStatus onConsoleRun(String[] arguments) {
        if (this.commandType.getSubCommandTypes().isEmpty() || arguments.length <= 0)
            return this.commandType.onConsoleRun(this.getSection(), arguments);

        for (CommandType commandType : this.commandType.getSubCommandTypes()) {
            String name = arguments[0];

            if (Objects.equals(commandType.getName(), name))
                return commandType.onConsoleRun(this.getSection(), arguments);
        }

        return this.commandType.onConsoleRun(this.getSection(), arguments);
    }

    /**
     * Executed when a player runs the command.
     *
     * @param arguments The arguments given in the command.
     * @param user      The instance of the user running the command.
     * @return The command's status.
     */
    public CommandStatus onPlayerRun(String[] arguments, User user) {
        if (this.commandType.getSubCommandTypes().isEmpty() || arguments.length <= 0)
            return this.commandType.onPlayerRun(this.getSection(), arguments, user);

        for (CommandType commandType : this.commandType.getSubCommandTypes()) {
            String name = arguments[0];

            if (Objects.equals(commandType.getName(), name))
                return commandType.onPlayerRun(this.getSection(), arguments, user);
        }

        return this.commandType.onPlayerRun(this.getSection(), arguments, user);
    }

    /**
     * Used to get the command's identifier.
     *
     * @return Commands identifier.
     */
    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Used to get the command's configuration section.
     *
     * @return The configuration section.
     */
    public ConfigurationSection getSection() {
        return ConfigCommands.getCommand(this.identifier);
    }

    /**
     * Used to get the name of the command.
     *
     * @return The name of the command.
     */
    public String getName() {
        return ConfigCommands.getCommandName(this.getIdentifier());
    }

    /**
     * Used to get the commands aliases.
     * These are other command names that will execute this command.
     *
     * @return The list of aliases.
     */
    public CommandAliases getAliases() {
        return ConfigCommands.getCommandAliases(this.getIdentifier());
    }

    /**
     * Used to get the permission to execute the command.
     *
     * @return Command permission.
     */
    public String getPermission() {
        return ConfigCommands.getCommandPermission(this.getIdentifier());
    }

    /**
     * Used to get the base command type.
     *
     * @return The base command type.
     */
    public BaseCommandType getBaseCommandType() {
        return this.commandType;
    }

    /**
     * Used to get if the command is enabled.
     *
     * @return True if the command is enabled.
     */
    public boolean isEnabled() {
        return ConfigCommands.isCommandEnabled(this.getIdentifier());
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();

        if (source instanceof Player) {
            User user = new User((Player) source);
            CommandStatus status = this.onPlayerRun(invocation.arguments(), user);

            if (status.hasIncorrectArguments()) {
                user.sendMessage(ConfigMessages.getIncorrectArguments(this.getSyntax())
                        .replace("[name]", this.getName()));
            }

            if (status.hasDatabaseDisabled()) user.sendMessage(ConfigMessages.getDatabaseDisabled());
            if (status.hasDatabaseEmpty()) user.sendMessage(ConfigMessages.getDatabaseEmpty());

            return;
        }

        CommandStatus status = this.onConsoleRun(invocation.arguments());

        if (status.hasIncorrectArguments()) {
            MessageManager.log(ConfigMessages.getIncorrectArguments(this.getSyntax())
                    .replace("[name]", this.getName()));
        }

        if (status.hasDatabaseDisabled()) MessageManager.log(ConfigMessages.getDatabaseDisabled());
        if (status.hasDatabaseEmpty()) MessageManager.log(ConfigMessages.getDatabaseEmpty());
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        String permission = this.getPermission();

        if (permission == null) return true;

        return invocation.source().hasPermission(this.getPermission());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        CommandSource source = invocation.source();

        if (source instanceof Player) {

            int index = invocation.arguments().length - 1;
            if (index == -1) index = 0;

            CommandSuggestions suggestions = this.getSuggestions(new User((Player) source));
            if (suggestions == null) suggestions = new CommandSuggestions();

            if (!this.commandType.getSubCommandTypes().isEmpty()) {
                for (CommandType commandType : this.commandType.getSubCommandTypes()) {
                    suggestions.appendBase(commandType.getName());

                    if (index == 0) continue;

                    if (Objects.equals(invocation.arguments()[0].toLowerCase(Locale.ROOT), commandType.getName().toLowerCase(Locale.ROOT))) {
                        suggestions.combineSubType(commandType.getSuggestions(new User((Player) source)));
                    }
                }
            }

            if (suggestions.get() == null) return CompletableFuture.completedFuture(List.of());
            if (suggestions.get().isEmpty()) return CompletableFuture.completedFuture(List.of());
            if (suggestions.get().size() <= index) return CompletableFuture.completedFuture(List.of());

            List<String> list = suggestions.get().get(index);

            if (list == null) return CompletableFuture.completedFuture(List.of());

            List<String> parsedList = new ArrayList<>();

            String argument = "";
            if (!(invocation.arguments().length - 1 < 0)) {
                argument = invocation.arguments()[invocation.arguments().length - 1].trim();
            }

            for (String item : list) {
                if (argument.equals("") || item.toLowerCase(Locale.ROOT).contains(argument.toLowerCase(Locale.ROOT)))
                    parsedList.add(item);
            }

            return CompletableFuture.completedFuture(parsedList);

        }

        return CompletableFuture.completedFuture(List.of());
    }
}
