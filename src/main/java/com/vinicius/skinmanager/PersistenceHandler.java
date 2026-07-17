package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PersistenceHandler {

    private boolean aguardandoAplicacao = false;

    // Nossa trava de segurança para ler o arquivo apenas UMA vez
    private boolean configCarregada = false;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        // 1. CARREGAMENTO SEGURO: Só lê a skin salva quando o OpenGL já estiver ligado!
        if (!configCarregada && mc.getTextureManager() != null) {
            configCarregada = true;
            ConfigManager.carregarUltimaSkin();
        }

        // 2. Menu atrasado (para não bugar o chat)
        if (SkinManagerMod.abrirMenuNoProximoTick && mc.theWorld != null) {
            SkinManagerMod.abrirMenuNoProximoTick = false;
            mc.displayGuiScreen(new GuiSkinManager());
        }

        // 3. O FIX DEFINITIVO DA TROCA DE SKIN (Alarme de persistência)
        if (SkinManagerMod.skinAtual != null && mc.thePlayer != null && mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());

            if (info != null && !SkinManagerMod.skinAtual.equals(info.getLocationSkin())) {
                aguardandoAplicacao = true;
            }

            if (aguardandoAplicacao) {
                boolean sucesso = SkinApplier.aplicarSkinEModelo(SkinManagerMod.skinAtual, SkinManagerMod.isSlimAtual);
                if (sucesso) {
                    aguardandoAplicacao = false;
                }
            }
        }
    }
}