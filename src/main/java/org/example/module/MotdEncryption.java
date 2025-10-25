package org.example.module;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.server.MotdBuildEvent;
import com.zenith.module.api.Module;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.example.EncryptionUtil;
import org.example.MotdCryptPlugin;

import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static org.example.MotdCryptPlugin.LOG;

public class MotdEncryption extends Module {
    private static final String motdMM = """
        <encrypted_motd>
        """;

    @Override
    public boolean enabledSetting() {
        return MotdCryptPlugin.PLUGIN_CONFIG.motdEncryption;
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
                of(MotdBuildEvent.class, this::handleMotdBuildEvent)
        );
    }

    private void handleMotdBuildEvent(MotdBuildEvent event) {
        String motdBody;
        try {
            String cypherText = EncryptionUtil.encrypt(componentToString(event.getMotd()), MotdCryptPlugin.PLUGIN_CONFIG.encryptionConfig.password);
            LOG.info("Encrypted MOTD: " + cypherText);
            motdBody = "##Start Encryption##" + cypherText;
        } catch (Exception e) {
            event.setMotd(null);
            LOG.error("Failed to encrypt MOTD", e);
            return;
        }

        event.setMotd(ComponentSerializer.minimessage(
                motdMM,
                Placeholder.unparsed("encrypted_motd", motdBody)
        ));
    }

    private String componentToString(Component component) {
        return ComponentSerializer.serializeJson(component);
    }
}
