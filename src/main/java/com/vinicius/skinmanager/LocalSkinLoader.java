package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class LocalSkinLoader {

    public static void carregarSkinLocal(File arquivo, boolean isSlim) {
        new Thread(() -> {
            try {
                BufferedImage imgCrua = ImageIO.read(arquivo);
                if (imgCrua == null) return;

                final BufferedImage imgFull;

                if (imgCrua.getHeight() == 32) {
                    imgFull = new ImageBufferDownload().parseUserSkin(imgCrua);
                } else {
                    imgFull = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                    imgFull.getGraphics().drawImage(imgCrua, 0, 0, null);
                }

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    DynamicTexture texturaDinamica = new DynamicTexture(imgFull);
                    ResourceLocation texturaAplicavel = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("skin_local_" + arquivo.getName(), texturaDinamica);

                    SkinManagerMod.skinAtual = texturaAplicavel;
                    SkinManagerMod.isSlimAtual = isSlim;

                    SkinApplier.aplicarSkinEModelo(texturaAplicavel, isSlim);
                    
                    // --- GATILHO DE PERSISTÊNCIA: Salva qual skin foi aplicada para carregar depois ---
                    ConfigManager.salvarUltimaSkin("LOCAL:" + arquivo.getName(), isSlim);
                });
            } catch (Exception e) {}
        }).start();
    }
}