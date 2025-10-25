package org.example.command;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import org.example.MotdCryptPlugin;
import org.example.module.MotdEncryption;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

/*
 * @author IceTank
 * @since 25.10.2025
 */
public class MotdCryptCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
                .name("motdcrypt")
                .description("Encrypts MOTD messages for Zenith proxy.")
                .category(CommandCategory.MODULE)
                .usageLines("/motdcrypt password [new password] - Sets or reads the password used for MOTD encryption.")
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("motdcrypt")
                .then(argument("toggle", toggle()).executes(c -> {
                    MotdCryptPlugin.PLUGIN_CONFIG.motdEncryption = getToggle(c, "toggle");
                    // make sure to sync so the module is actually toggled
                    MODULE.get(MotdEncryption.class).syncEnabledFromConfig();
                    c.getSource().getEmbed()
                            .title("MotdEncryption: " + toggleStrCaps(MotdCryptPlugin.PLUGIN_CONFIG.motdEncryption))
                            .primaryColor();
                }))
                .then(literal("password")
                        .executes(c -> {
                            c.getSource().getEmbed().title("MotdCrypt Password: " + MotdCryptPlugin.PLUGIN_CONFIG.encryptionConfig.password);
                        })
                        .then(argument("newPassword", string()).executes(c -> {
                            String newPassword = getString(c, "newPassword");
                            MotdCryptPlugin.PLUGIN_CONFIG.encryptionConfig.password = newPassword;

                            c.getSource().getEmbed().title("MotdCrypt Password updated to: " + newPassword);
                        }))
                );
    }
}
