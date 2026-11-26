package com.cowhat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

/**
 * Client-side only — never sent to the server, same as any other client command. Registers
 * {@code /hatrix}:
 * - {@code /hatrix toggle}: flips whether a matched custom texture replaces the head item
 *   (default) or sits above it additively, like cat ears.
 * - {@code /hatrix addtexture <match> <file>}: adds a rule to cowhat-textures.json. The PNG
 *   must already be in config/cowhat/textures/; this never downloads anything.
 */
public final class HatrixCommand {
    private HatrixCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("hatrix")
                        .executes(HatrixCommand::help)
                        .then(ClientCommands.literal("toggle").executes(HatrixCommand::toggle))
                        .then(ClientCommands.literal("addtexture")
                                .then(ClientCommands.argument("match", StringArgumentType.string())
                                        .then(ClientCommands.argument("file", StringArgumentType.string())
                                                .executes(HatrixCommand::addTexture))))
        ));
    }

    private static int help(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Component.literal(
                "Hatrix: /hatrix toggle (replace vs. overlay for custom textures), "
                + "/hatrix addtexture <match> <file> (add a texture rule; PNG must already be in config/cowhat/textures/)."));
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        boolean overlay = HatrixSettings.toggleCustomTextureOverlay();
        ctx.getSource().sendFeedback(Component.literal(overlay
                ? "Hatrix: custom textures now sit above the head item instead of replacing it."
                : "Hatrix: custom textures now replace the head item (default)."));
        return 1;
    }

    private static int addTexture(CommandContext<FabricClientCommandSource> ctx) {
        String match = StringArgumentType.getString(ctx, "match");
        String file = StringArgumentType.getString(ctx, "file");
        boolean ok = CowHatTextures.addRule(match, file);
        if (ok) {
            ctx.getSource().sendFeedback(Component.literal("Hatrix: added a texture rule \"" + match + "\" -> " + file));
            return 1;
        }
        ctx.getSource().sendError(Component.literal(
                "Hatrix: couldn't find config/cowhat/textures/" + file + " — place the PNG there first."));
        return 0;
    }
}
