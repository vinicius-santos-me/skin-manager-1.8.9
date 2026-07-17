package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ChatComponentText;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Properties;

public class SkinApplier {

    // --- Cache de Capa ---
    private static boolean capaAtivadaCache = false;
    private static int capaIndexCache = 0;
    private static boolean cacheCarregado = false;

    // --- Ferramenta Suprema Contra Obfuscação (Por Tipo) ---
    private static Field campoSkin = null;
    private static Field campoCapa = null;
    private static Field campoSkinType = null;
    private static Field campoTexturesLoaded = null;
    private static Field campoPlayerInfoCache = null;

    private static File getCapaFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "skinmanager_capas.properties");
    }

    private static void inicializarCampos() throws Exception {
        if (campoSkin != null) return; // Já foi mapeado

        // 1. Vasculha NetworkPlayerInfo baseando-se APENAS nos TIPOS das variáveis
        for (Field f : NetworkPlayerInfo.class.getDeclaredFields()) {
            if (f.getType() == ResourceLocation.class) {
                if (campoSkin == null) {
                    campoSkin = f; // 1º ResourceLocation: Skin
                } else if (campoCapa == null) {
                    campoCapa = f; // 2º ResourceLocation: Capa
                }
            } else if (f.getType() == String.class) {
                if (campoSkinType == null) {
                    campoSkinType = f; // Única String: skinType (slim/default)
                }
            } else if (f.getType() == boolean.class) {
                if (campoTexturesLoaded == null) {
                    campoTexturesLoaded = f; // Único boolean: playerTexturesLoaded
                }
            }
        }

        // 2. Vasculha o AbstractClientPlayer atrás da variável do NetworkPlayerInfo
        for (Field f : AbstractClientPlayer.class.getDeclaredFields()) {
            if (NetworkPlayerInfo.class.isAssignableFrom(f.getType())) {
                campoPlayerInfoCache = f;
                break;
            }
        }

        // 3. Libera as permissões
        if (campoSkin != null) campoSkin.setAccessible(true);
        if (campoCapa != null) campoCapa.setAccessible(true);
        if (campoSkinType != null) campoSkinType.setAccessible(true);
        if (campoTexturesLoaded != null) campoTexturesLoaded.setAccessible(true);
        if (campoPlayerInfoCache != null) campoPlayerInfoCache.setAccessible(true);
    }

    public static void salvarCapa(boolean usar, int index) {
        capaAtivadaCache = usar;
        capaIndexCache = index;
        cacheCarregado = true;
        try {
            Properties p = new Properties();
            p.setProperty("usarCapa", String.valueOf(usar));
            p.setProperty("capaIndex", String.valueOf(index));
            p.store(new FileOutputStream(getCapaFile()), "Configuracao de Capa");
        } catch (Exception e) {
            SkinManagerMod.LOGGER.error("Erro ao salvar capa!", e);
        }
    }

    public static void carregarEstadoCapaMenu() {
        try {
            File f = getCapaFile();
            if (f.exists()) {
                Properties p = new Properties();
                p.load(new FileInputStream(f));
                capaAtivadaCache = Boolean.parseBoolean(p.getProperty("usarCapa", "false"));
                capaIndexCache = Integer.parseInt(p.getProperty("capaIndex", "0"));
                cacheCarregado = true;
                GuiSkinManager.previewCapaAtivada = capaAtivadaCache;
                GuiSkinManager.previewCapaIndex = capaIndexCache;
            }
        } catch (Exception e) {
            SkinManagerMod.LOGGER.error("Erro ao carregar estado da capa!", e);
        }
    }

    public static void aplicarCapaSalvaNoMundo() {
        if (!cacheCarregado) {
            try {
                File f = getCapaFile();
                if (f.exists()) {
                    Properties p = new Properties();
                    p.load(new FileInputStream(f));
                    capaAtivadaCache = Boolean.parseBoolean(p.getProperty("usarCapa", "false"));
                    capaIndexCache = Integer.parseInt(p.getProperty("capaIndex", "0"));
                    cacheCarregado = true;
                }
            } catch (Exception e) {
                return;
            }
        }
        aplicarCapaNoJogo(capaAtivadaCache, capaIndexCache);
    }

    public static boolean aplicarSkinEModelo(ResourceLocation tex, boolean slim) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info != null) {
            try {
                inicializarCampos();

                if (campoSkin != null) campoSkin.set(info, tex);
                if (campoSkinType != null) campoSkinType.set(info, slim ? "slim" : "default");
                if (campoTexturesLoaded != null) campoTexturesLoaded.set(info, true);
                if (campoPlayerInfoCache != null) campoPlayerInfoCache.set(mc.thePlayer, null);

                return true;
            } catch (Exception e) {
                SkinManagerMod.LOGGER.error("Erro na injecao!", e);
                mc.thePlayer.addChatMessage(new ChatComponentText("§c[SkinManager] Erro: " + e.toString()));
            }
        }
        return false;
    }

    public static void aplicarCapaNoJogo(boolean usarCapa, int capaIndex) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info != null) {
            try {
                inicializarCampos();

                ResourceLocation capaLoc = null;
                if (usarCapa) {
                    capaLoc = new ResourceLocation("skinmanager", "textures/cape/" + GuiSkinManager.ARQUIVOS_CAPAS[capaIndex] + ".png");
                }

                if (campoCapa != null) campoCapa.set(info, capaLoc);
                if (campoPlayerInfoCache != null) campoPlayerInfoCache.set(mc.thePlayer, null);

            } catch (Exception e) {
                SkinManagerMod.LOGGER.error("Erro na capa!", e);
                mc.thePlayer.addChatMessage(new ChatComponentText("§c[SkinManager] Erro Capa: " + e.toString()));
            }
        }
    }
}