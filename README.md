# ZenithProxy Motd Crypt Plugin

This is a plugin that encrypts the MOTD Entry on your server lists. This is intended to prevent people from being able to read the information in the MOTD such as Account Name, Online time and queue position.
You will need a client mod that is able to decrypt the MOTD Message if you want to be able to read the MOTD.
This does not replace a regular whitelist or authentication system, it is only intended to prevent casual snooping of MOTD information.

## Client mod
A fabric client mod to decrypt the MOTD message can be found here: [ZenithMotdCryptClient](https://github.com/IceTank/ZenithMotdCryptClient)

## Commands

- `/motdcrypt password [new password]` - Sets or reads the current encryption key

## Installing Plugins

Plugins are only supported on the `java` ZenithProxy release channel (i.e. not `linux`).

Place plugin jars in the `plugins` folder inside the same folder as the ZenithProxy launcher.

Restart ZenithProxy to load plugins. Loading plugins after launch or hot reloading is not supported.

## Creating Plugins

Use this repository as a template to create your own plugin repository.

### Plugin Structure

Each plugin needs a main class that implements `ZenithProxyPlugin` and is annotated with `@Plugin`.

Plugin metadata like its unique id, version, and supported MC versions is defined in the `@Plugin` annotation.

[See example](https://github.com/rfresh2/ZenithProxyExamplePlugin/blob/1.21.4/src/main/java/org/example/ExamplePlugin.java)

### Plugin API

The `ZenithProxyPlugin` interface requires you to implement an `onLoad` method.

This method provides a `PluginAPI` object that you can use to register modules, commands, and config files.

`Module` and `Command` classes are implemented the same as in the ZenithProxy source code.

I recommend looking at existing modules, commands, and plugins for examples.

* [Module](https://github.com/rfresh2/ZenithProxy/tree/1.21.4/src/main/java/com/zenith/module)
* [Command](https://github.com/rfresh2/ZenithProxy/tree/1.21.4/src/main/java/com/zenith/command)
* Plugins
  * [ZenithProxyVillagerTrader](https://github.com/rfresh2/ZenithProxyVillagerTrader)
  * [ZenithProxyWebAPI](https://github.com/rfresh2/ZenithProxyWebAPI)
  * [ZenithProxyChatControl](https://github.com/rfresh2/ZenithProxyChatControl)
  * More in [my discord server](https://discord.com/channels/1127460556710883391/1369081651564515358)

### JavaDocs

https://maven.2b2t.vc/javadoc/releases/com/zenith/ZenithProxy/1.21.4-SNAPSHOT

### Building Plugins

Execute the Gradle `build` task: `./gradlew build` - or double-click the task in Intellij

The built plugin jar will be in the `build/libs` directory.

### Testing Plugins

Execute the `run` task: `./gradlew run` - or double-click the task in Intellij

This will run ZenithProxy with your plugin loaded in the `run` directory.

### New Plugin Checklist

1. Edit `gradle.properties`:
   - `plugin_name` - Name of your plugin, used in the plugin jar name (e.g. `ExamplePlugin`)
   - `maven_group` - Java package for your project (e.g. `com.github.rfresh2`)
1. Move files to your new corresponding package / maven group:
   - Example: `src/main/java/org/example` -> `src/main/java/com/github/rfresh2`
   - First create the new package in `src/main/java`. Then click and drag original subpackages/classes to your new one
   - Do this with Intellij to avoid manually editing all the source files
   - You must also move folders/package for the `src/main/templates` folder
   - Also make sure to update the package import at the very top of `BuiltConstants.java`, it will not be done automatically
1. Edit `ExamplePlugin.java`, or remove it and create a new main class
   - Make sure to update the `@Plugin` annotation
