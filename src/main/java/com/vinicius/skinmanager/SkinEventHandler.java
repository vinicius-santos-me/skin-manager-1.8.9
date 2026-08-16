package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SkinEventHandler {

    private boolean inicializouArquivos = false;

    // Contador de reforço por frame — evita fazer reflection em TODO frame
    // (era isso que causava custo desnecessário); ainda reforça rápido o
    // suficiente para não perder nenhum frame visível ao entrar no mundo.
    private int framesDesdeEntrada = 0;
    private static final int FRAMES_REFORCO = 10; // reforça nos primeiros 10 frames após join

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

        if (!inicializouArquivos) {
            inicializouArquivos = true;
            SkinApplier.carregarEstadoCapaMenu();
            ConfigManager.carregarUltimaSkin();
        }

        // Só reforça nos primeiros frames após o join — depois disso o
        // PersistenceHandler cuida do monitoramento contínuo, evitando
        // fazer reflection pesada em todo frame renderizado.
        if (framesDesdeEntrada >= FRAMES_REFORCO) return;
        framesDesdeEntrada++;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null) return;
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info == null) return;

        // Usa o SkinApplier (busca de campo POR TIPO) em vez do
        // ObfuscationReflectionHelper com nomes fixos — os nomes obfuscados
        // fixos ("field_178874_j" etc.) falhavam silenciosamente sempre que
        // outro mod (ex.: Patcher, via Mixin) alterava a estrutura da classe.
        // A busca por tipo já está funcionando no restante do mod.
        if (SkinManagerMod.skinAtual != null) {
            SkinApplier.aplicarSkinEModelo(SkinManagerMod.skinAtual, SkinManagerMod.isSlimAtual);
        }
        SkinApplier.aplicarCapaSalvaNoMundo();
    }

    @SubscribeEvent
    public void aoEntrarNoMundo(EntityJoinWorldEvent event) {
        if (event.entity == null || event.entity != Minecraft.getMinecraft().thePlayer) return;

        // Reseta o contador de reforço a cada novo join (login, troca de
        // servidor, respawn no fim do mundo, etc.) para garantir que a
        // capa/skin sejam reforçadas nos primeiros frames do NOVO mundo.
        framesDesdeEntrada = 0;

        SkinApplier.aplicarCapaSalvaNoMundo();
        if (SkinManagerMod.skinAtual != null) {
            SkinApplier.aplicarSkinEModelo(SkinManagerMod.skinAtual, SkinManagerMod.isSlimAtual);
        }
    }
}