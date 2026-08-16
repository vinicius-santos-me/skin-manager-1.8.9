package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class PersistenceHandler {

    private boolean aguardandoAplicacao = false;
    private boolean configCarregada = false;

    // Intervalo entre verificações de integridade (a cada 0.5s) — o
    // SkinEventHandler já cobre os primeiros frames após o join com mais
    // intensidade; aqui cuidamos do monitoramento contínuo durante a sessão
    // (outro mod sobrescrevendo depois de um tempo, etc.) sem custo pesado.
    private static final int INTERVALO_VERIFICACAO = 10;
    private int ticksVerificacao = 0;

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();

        // 1. CARREGAMENTO SEGURO: só lê a skin salva quando o OpenGL já estiver ligado
        if (!configCarregada && mc.getTextureManager() != null) {
            configCarregada = true;
            ConfigManager.carregarUltimaSkin();
        }

        // 2. Menu atrasado (para não bugar o chat)
        if (SkinManagerMod.abrirMenuNoProximoTick && mc.theWorld != null) {
            SkinManagerMod.abrirMenuNoProximoTick = false;
            mc.displayGuiScreen(new GuiSkinManager());
        }

        // 3. Alarme de persistência — verifica em intervalos, não todo tick
        if (SkinManagerMod.skinAtual == null || mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        ticksVerificacao++;
        if (ticksVerificacao < INTERVALO_VERIFICACAO && !aguardandoAplicacao) return;
        ticksVerificacao = 0;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info == null) return;

        if (!SkinManagerMod.skinAtual.equals(info.getLocationSkin())) {
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