package com.cowhat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLError;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * The {@code /hatrix} menu. Lets the player flip the custom-texture replace/overlay setting
 * and add a new texture rule by picking a PNG with the OS's native file picker — no need to
 * hand-edit {@code cowhat-textures.json} or manually drop files into
 * {@code config/cowhat/textures/}.
 *
 * The picker is SDL3's ({@code SDL_ShowOpenFileDialog}), which Minecraft already ships and
 * whose window it already uses. AWT can't be used: Minecraft runs with
 * {@code java.awt.headless=true}, so any AWT dialog throws.
 */
public class HatrixScreen extends Screen {
    private EditBox matchBox;
    private StringWidget status;
    // Kept across init() calls so resizing the window doesn't wipe what the player typed.
    private String matchText = "";
    private Component statusText = Component.empty();
    private boolean pickerOpen = false;

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
        this.addRenderableWidget(Button.builder(toggleLabel(), b -> {
            boolean overlay = HatrixSettings.toggleCustomTextureOverlay();
            b.setMessage(toggleLabel());
            setStatus(overlay ? "Custom textures now sit above the head item." : "Custom textures now replace the head item.");
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        this.addRenderableWidget(new StringWidget(centerX - 100, y, 200, 20,
                Component.literal("Match text (e.g. \"cow\", \"birthday\")"), this.font));
        y += 22;
        matchBox = this.addRenderableWidget(new EditBox(this.font, centerX - 100, y, 200, 20, Component.literal("Match")));
        matchBox.setHint(Component.literal("item name to match"));
        matchBox.setMaxLength(64);
        matchBox.setValue(matchText);
        matchBox.setResponder(v -> matchText = v);

        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Add Image..."), b -> pickAndAddImage())
                .bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        status = this.addRenderableWidget(new StringWidget(centerX - 150, y, 300, 20, statusText, this.font));

        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(centerX - 100, y, 200, 20).build());
    }

    private void setStatus(String message) {
        statusText = Component.literal(message);
        status.setMessage(statusText);
    }

    private Component toggleLabel() {
        return Component.literal("Custom texture: "
                + (HatrixSettings.isCustomTextureOverlay() ? "Overlay (sits above)" : "Replace (default)"));
    }

    /**
     * Opens the native file picker. It's asynchronous: SDL calls back (possibly on another
     * thread) once the dialog closes, and the import then runs back on the client thread, since
     * it uploads a texture. Never touches the network — the file never leaves this machine.
     */
    private void pickAndAddImage() {
        if (pickerOpen) return;
        String match = matchText.strip();
        if (match.isEmpty()) {
            setStatus("Type a match first.");
            return;
        }

        // The filter array and its strings must stay valid until the callback runs, so they
        // live off-heap and are freed there, together with the callback itself.
        ByteBuffer name = MemoryUtil.memUTF8("PNG images");
        ByteBuffer pattern = MemoryUtil.memUTF8("png");
        SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1);
        filters.get(0).name(name).pattern(pattern);
        SDL_DialogFileCallback[] self = new SDL_DialogFileCallback[1];
        self[0] = SDL_DialogFileCallback.create((userdata, filelist, filter) -> {
            // filelist: NULL on error, an empty NULL-terminated list on cancel.
            String picked = null;
            String error = null;
            if (filelist == MemoryUtil.NULL) {
                error = SDLError.SDL_GetError();
            } else {
                long first = MemoryUtil.memGetAddress(filelist);
                if (first != MemoryUtil.NULL) picked = MemoryUtil.memUTF8(first);
            }
            String path = picked, err = error;
            // schedule(), not execute(): SDL may call this synchronously on the client thread,
            // and execute() would then run inline and free this callback while it's on the stack.
            Minecraft.getInstance().schedule(() -> {
                self[0].free();
                filters.free();
                MemoryUtil.memFree(name);
                MemoryUtil.memFree(pattern);
                pickerOpen = false;
                onPicked(path, err, match);
            });
        });

        pickerOpen = true;
        setStatus("Waiting for file picker...");
        long window = Minecraft.getInstance().getWindow().handle();
        SDLDialog.SDL_ShowOpenFileDialog(self[0], MemoryUtil.NULL, window, filters, (CharSequence) null, false);
    }

    private void onPicked(String path, String error, String match) {
        if (error != null) {
            CowHatClient.LOGGER.warn("Hatrix: file picker failed: {}", error);
            setStatus("File picker failed — use /hatrix addtexture instead.");
            return;
        }
        if (path == null) {
            setStatus("Cancelled.");
            return;
        }
        String failure = CowHatTextures.importImage(Path.of(path), match);
        setStatus(failure == null ? "Added \"" + match + "\" -> " + Path.of(path).getFileName() : "Couldn't add it: " + failure);
    }
}
