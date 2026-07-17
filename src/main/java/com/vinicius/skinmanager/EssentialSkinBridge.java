package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Ponte entre o SkinManager e o Essential.
 *
 * O Essential renderiza o boneco na tela inicial lendo a skin
 * diretamente do SkinManager do próprio Essential (não do NetworkPlayerInfo).
 * A única forma de influenciar isso sem Mixin é sobrescrever o campo
 * "locationSkin" da instância de AbstractClientPlayer que o Essential usa
 * para o preview — que é o mc.thePlayer quando está no menu principal.
 *
 * Estratégia: quando a skin está definida e mc.thePlayer existe mas
 * mc.theWorld é null (= tela de menu), aplicamos a skin diretamente
 * no AbstractClientPlayer para o Essential ler.
 */
public class EssentialSkinBridge {

    private static boolean registrado = false;

    public static void registrar() {
        if (registrado) return;
        registrado = true;
        MinecraftForge.EVENT_BUS.register(new EssentialSkinBridge());
        SkinManagerMod.LOGGER.info("[EssentialSkinBridge] Hook registrado com sucesso.");
    }

    private int ticksMenu = 0;
    private static final int INTERVALO_MENU = 20; // checa 1x por segundo no menu

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (SkinManagerMod.skinAtual == null) return;

        Minecraft mc = Minecraft.getMinecraft();

        // Só age quando está no menu (theWorld == null) e thePlayer existe
        // Isso é exatamente quando o Essential renderiza o boneco na tela inicial
        if (mc.theWorld != null || mc.thePlayer == null) return;

        ticksMenu++;
        if (ticksMenu < INTERVALO_MENU) return;
        ticksMenu = 0;

        // Aplica a skin no player do menu para o Essential ler
        // Usa o mesmo SkinApplier mas sem exigir getNetHandler() (não tem no menu)
        try {
            net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(
                    net.minecraft.client.entity.AbstractClientPlayer.class,
                    mc.thePlayer,
                    SkinManagerMod.skinAtual,
                    "locationSkin", "field_178851_e"
            );
        } catch (Exception e) {
            // Campo não encontrado nessa versão — ignora silenciosamente
        }
    }
}