package com.cowhat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Path;

/**
 * The {@code /hatrix} menu. Lets the player flip the custom-texture replace/overlay setting
 * and add a new texture rule by picking a PNG with the OS's native file picker — no need to
 * hand-edit {@code cowhat-textures.json} or manually drop files into
 * {@code config/cowhat/textures/}.
 */
public class HatrixScreen extends Screen {
    private EditBox matchBox;
    private Button toggleButton;
    private StringWidget status;

    public HatrixScreen() {
        super(Component.literal("Hatrix"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 60;

        this.addRenderableWidget(new StringWidget(centerX - 100, y, 200, 20,
                Component.literal("Custom texture mode"), this.font));
        y += 22;
        toggleButton = this.addRenderableWidget(Button.builder(toggleLabel(), b -> {
            boolean overlay = HatrixSettings.toggleCustomTextureOverlay();
            b.setMessage(toggleLabel());
            status.setMessage(Component.literal(overlay
                    ? "Custom textures now sit above the head item."
                    : "Custom textures now replace the head item."));
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        this.addRenderableWidget(new StringWidget(centerX - 100, y, 200, 20,
                Component.literal("Match text (e.g. \"cow\", \"birthday\")"), this.font));
        y += 22;
        matchBox = this.addRenderableWidget(new EditBox(this.font, centerX - 100, y, 200, 20, Component.literal("Match")));
        matchBox.setHint(Component.literal("item name to match"));
        matchBox.setMaxLength(64);

        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Add Image..."), b -> pickAndAddImage())
                .bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        status = this.addRenderableWidget(new StringWidget(centerX - 100, y, 200, 20, Component.literal(""), this.font));

        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(centerX - 100, y, 200, 20).build());
    }

    private Component toggleLabel() {
        return Component.literal("Custom texture: "
                + (HatrixSettings.isCustomTextureOverlay() ? "Overlay (sits above)" : "Replace (default)"));
    }

    /**
     * Opens the OS's native file picker on a background thread (it blocks until closed, and
     * must not block the render thread), then copies the chosen PNG into
     * config/cowhat/textures/ and adds a match rule for it, back on the main client thread
     * since that needs the GL context. Never touches the network — the file never leaves this
     * machine's disk.
     */
    private void pickAndAddImage() {
        String match = matchBox.getValue();
        if (match == null || match.isBlank()) {
            status.setMessage(Component.literal("Type a match first."));
            return;
        }
        status.setMessage(Component.literal("Waiting for file picker..."));
        Thread thread = new Thread(() -> {
            FileDialog dialog = new FileDialog((Frame) null, "Select a PNG for Hatrix", FileDialog.LOAD);
            dialog.setFile("*.png");
            dialog.setVisible(true);
            String dir = dialog.getDirectory();
            String file = dialog.getFile();
            Minecraft.getInstance().execute(() -> {
                if (dir == null || file == null) {
                    status.setMessage(Component.literal("Cancelled."));
                    return;
                }
                String saved = CowHatTextures.importImage(Path.of(dir, file), match);
                status.setMessage(Component.literal(saved != null
                        ? "Added \"" + match + "\" -> " + saved
                        : "Couldn't add that image (see log)."));
            });
        }, "hatrix-file-dialog");
        thread.setDaemon(true);
        thread.start();
    }
}
