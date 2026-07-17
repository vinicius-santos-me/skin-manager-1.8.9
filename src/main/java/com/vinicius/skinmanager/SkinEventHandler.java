package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SkinEventHandler {

    private boolean inicializouArquivos = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && SkinManagerMod.abrirMenuNoProximoTick) {
            SkinManagerMod.abrirMenuNoProximoTick = false;
            Minecraft.getMinecraft().displayGuiScreen(new GuiSkinManager());
        }
    }

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Pre event) {
        if (!(event.entityPlayer instanceof AbstractClientPlayer)) return;
        AbstractClientPlayer player = (AbstractClientPlayer) event.entityPlayer;

        if (player != Minecraft.getMinecraft().thePlayer) return;

        // --- CORREÇÃO DA CAPA AQUI ---
        if (!inicializouArquivos) {
            inicializouArquivos = true;
            SkinApplier.carregarEstadoCapaMenu(); // <-- Faltava isso para a capa carregar no Join!
            ConfigManager.carregarUltimaSkin();
        }

        NetworkPlayerInfo info = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info != null) {
            try {
                if (SkinManagerMod.skinAtual != null) {
                    ObfuscationReflectionHelper.setPrivateValue(NetworkPlayerInfo.class, info, SkinManagerMod.skinAtual, "locationSkin", "field_178873_i");
                    ObfuscationReflectionHelper.setPrivateValue(NetworkPlayerInfo.class, info, SkinManagerMod.isSlimAtual ? "slim" : "default", "skinType", "field_178872_h");
                    ObfuscationReflectionHelper.setPrivateValue(NetworkPlayerInfo.class, info, true, "playerTexturesLoaded", "field_181060_b");
                }

                ResourceLocation capaLoc = null;
                if (GuiSkinManager.previewCapaAtivada) {
                    capaLoc = new ResourceLocation("skinmanager", "textures/cape/" + GuiSkinManager.ARQUIVOS_CAPAS[GuiSkinManager.previewCapaIndex] + ".png");
                }
                ObfuscationReflectionHelper.setPrivateValue(NetworkPlayerInfo.class, info, capaLoc, "locationCape", "field_178874_j");

            } catch (Exception e) {}
        }
    }
}