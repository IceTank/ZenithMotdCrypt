package org.example;

import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.example.command.MotdCryptCommand;
import org.example.module.MotdEncryption;

@Plugin(
    id = "motd-crypt",
    version = BuildConstants.VERSION,
    description = "ZenithProxy Motd Encryption",
    url = "https://github.com/icetank/zenith-proxy-motd-crypt",
    authors = {"icetank"},
    mcVersions = {"1.21.4"} // to indicate any MC version: @Plugin(mcVersions = "*")
                            // if you touch packet classes, you almost certainly need to pin to a single mc version
)
public class MotdCryptPlugin implements ZenithProxyPlugin {
    // public static for simple access from modules and commands
    // or alternatively, you could pass these around in constructors
    public static ModuleCryptConfig PLUGIN_CONFIG;
    public static ComponentLogger LOG;

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        LOG.info("Loading Motd Encryption module...");
        PLUGIN_CONFIG = pluginAPI.registerConfig("motd-crypt", ModuleCryptConfig.class);
        pluginAPI.registerModule(new MotdEncryption());
        pluginAPI.registerCommand(new MotdCryptCommand());
        LOG.info("Motd Encryption module registered.");
    }
}
