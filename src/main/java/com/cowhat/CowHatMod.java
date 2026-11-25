package com.cowhat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.WeakHashMap;

/**
 * Client-side only. Any player wearing a "cow hat" (helmet slot item whose name
 * contains "cow") has that item hidden and a Star of David drawn above their head.
 */
@Mod(modid = "cowhat", name = "Cow Hat", version = "1.0.0", clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class CowHatMod {

    private static final int HELMET_SLOT = 3; // armorInventory index
    private final WeakHashMap<EntityPlayer, ItemStack> hidden = new WeakHashMap<>();

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static boolean isCowHat(ItemStack s) {
        if (s == null) return false;
        String name = EnumChatFormatting.getTextWithoutFormattingCodes(s.getDisplayName());
        return name != null && name.toLowerCase().contains("cow");
    }

    // Temporarily remove the cow hat so vanilla doesn't render it.
    @SubscribeEvent
    public void pre(RenderPlayerEvent.Pre e) {
        EntityPlayer p = e.entityPlayer;
        ItemStack hat = p.inventory.armorInventory[HELMET_SLOT];
        if (isCowHat(hat)) {
            hidden.put(p, hat);
            p.inventory.armorInventory[HELMET_SLOT] = null;
        }
    }

    @SubscribeEvent
    public void post(RenderPlayerEvent.Post e) {
        EntityPlayer p = e.entityPlayer;
        ItemStack hat = hidden.remove(p);
        if (hat == null) return;
        p.inventory.armorInventory[HELMET_SLOT] = hat; // restore

        float pt = e.partialRenderTick;
        float bodyYaw = p.prevRenderYawOffset + (p.renderYawOffset - p.prevRenderYawOffset) * pt;
        float headYaw = p.prevRotationYawHead + (p.rotationYawHead - p.prevRotationYawHead) * pt;
        double y = e.y + p.height + 0.45 - (p.isSneaking() ? 0.2 : 0.0);

        GlStateManager.pushMatrix();
        GlStateManager.translate(e.x, y, e.z);
        GlStateManager.rotate(-headYaw, 0, 1, 0);
        drawStar(0.35f);
        GlStateManager.popMatrix();
    }

    private static void drawStar(float r) {
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);

        Tessellator t = Tessellator.getInstance();
        WorldRenderer w = t.getWorldRenderer();
        w.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int tri = 0; tri < 2; tri++) {
            double off = tri == 0 ? 90 : 270;
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(off + i * 120);
                w.pos(Math.cos(a) * r, Math.sin(a) * r, tri * 0.001).color(0, 56, 184, 255).endVertex(); // #0038B8
            }
        }
        t.draw();

        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
    }
}
