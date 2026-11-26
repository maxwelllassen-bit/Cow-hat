package com.cowhat;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.UUID;

/**
 * Client-side only — never sent to the server, same as any other client command. Registers
 * {@code /hatrix}:
 * - {@code /hatrix} alone opens the {@link HatrixScreen} menu — toggle the replace/overlay
 *   setting and add a texture rule by picking a PNG with the OS file picker, no typing needed.
 * - {@code /hatrix toggle}: the same toggle, from chat, for scripting/convenience.
 * - {@code /hatrix addtexture <match> <file>}: the same add-a-rule action, from chat. The PNG
 *   must already be in config/cowhat/textures/; this never downloads anything.
 * - {@code /hatrix debug}: prints, to your own chat, exactly what the mod sees about your
 *   currently-worn head item and your own account — for diagnosing why a cosmetic isn't
 *   showing up, without needing to dig through the game log.
 */
public final class HatrixCommand {
    private HatrixCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("hatrix")
                        .executes(HatrixCommand::openMenu)
                        .then(ClientCommands.literal("toggle").executes(HatrixCommand::toggle))
                        .then(ClientCommands.literal("addtexture")
                                .then(ClientCommands.argument("match", StringArgumentType.string())
                                        .then(ClientCommands.argument("file", StringArgumentType.string())
                                                .executes(HatrixCommand::addTexture))))
                        .then(ClientCommands.literal("debug").executes(HatrixCommand::debug))
        ));
    }

    private static int openMenu(CommandContext<FabricClientCommandSource> ctx) {
        // Opening a Screen directly from a command's executor gets wiped out: closing the chat
        // input after a submitted command clears the screen in the same tick, right after this
        // runs. Deferring to the next tick lets our screen survive that.
        Minecraft client = ctx.getSource().getClient();
        client.execute(() -> client.setScreenAndShow(new HatrixScreen()));
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
        String error = CowHatTextures.addRule(match, file);
        if (error == null) {
            ctx.getSource().sendFeedback(Component.literal("Hatrix: added a texture rule \"" + match + "\" -> " + file));
            return 1;
        }
        ctx.getSource().sendError(Component.literal("Hatrix: " + error + "."));
        return 0;
    }

    private static int debug(CommandContext<FabricClientCommandSource> ctx) {
        LocalPlayer player = ctx.getSource().getPlayer();
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        String name = head.isEmpty() ? null : head.getHoverName().getString();
        String lowerItem = name == null ? null : name.toLowerCase(Locale.ROOT);
        boolean cow = lowerItem != null && lowerItem.contains("cow");
        String wearerName = player.getGameProfile().name();
        String wearer = wearerName.toLowerCase(Locale.ROOT);
        UUID wearerUuid = player.getUUID();
        boolean starAuth = CowHatAuth.isAuthorized(wearer, wearerUuid);
        boolean texAuth = CowHatTextures.isAuthorized(wearer, wearerUuid);
        boolean earsAuth = CatEars.isAuthorized(wearer, wearerUuid);
        boolean bearAuth = Bear.isAuthorized(wearer, wearerUuid);
        boolean texMatch = texAuth && CowHatTextures.find(lowerItem) != null;
        String lineIcon = cow && bearAuth ? "bear" : cow && starAuth ? "star" : null;
        String showing = !texMatch ? (lineIcon != null ? lineIcon : "no head swap")
                : HatrixSettings.isCustomTextureOverlay() && lineIcon != null ? lineIcon + " + custom texture"
                : "custom texture";

        ctx.getSource().sendFeedback(Component.literal(
                "Hatrix debug:\n"
                + "  head item: " + (name == null ? "(none)" : "\"" + name + "\"") + "\n"
                + "  name contains \"cow\": " + cow + "\n"
                + "  your account: name=\"" + wearerName + "\" uuid=" + wearerUuid + "\n"
                + "  on cowhat-allowlist.json (star): " + starAuth + "\n"
                + "  on cowhat-textures-allowlist.json: " + texAuth + " (rule matches item: " + texMatch + ")\n"
                + "  on cowhat-catears.json: " + earsAuth + "\n"
                + "  on cowhat-bear.json: " + bearAuth + "\n"
                + "  => showing now: " + showing
                + (earsAuth ? " + cat ears" : "")));
        return 1;
    }
}
