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

                // Antes: skins de 64px eram copiadas cruas com Graphics.drawImage
                // pra um canvas ARGB. Isso NÃO limpa a camada de overlay (chapéu,
                // jaqueta, mangas) — se o PNG de origem não tem canal alpha de
                // verdade (comum em arquivos exportados sem transparência),
                // drawImage grava alpha=255 (opaco) em tudo, inclusive nas áreas
                // que deveriam ficar transparentes. É exatamente isso que aparece
                // como quadradinhos pretos/brancos embaixo do braço quando o
                // modelo muda de Steve (4px) pra Alex (3px), já que a geometria
                // passa a expor um pedaço diferente da textura.
                //
                // Correção: usar sempre o parseUserSkin() nativo do jogo — é o
                // MESMO pós-processador que o SkinFetcher.java já usa pra skins
                // baixadas da Mojang (32 ou 64px), então ele já sabe tratar os
                // dois formatos e sanear a camada de overlay corretamente.
                final BufferedImage imgFull = new ImageBufferDownload().parseUserSkin(imgCrua);

                // Compensação experimental do desalinhamento de 1px do braço
                // Slim (ver comentário em SkinApplier.compensarDesalinhamentoSlim).
                // Só entra em ação quando a skin é aplicada com isSlim=true.
                final BufferedImage imgFinal = isSlim
                        ? SkinApplier.compensarDesalinhamentoSlim(imgFull)
                        : imgFull;

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    String chave = "skin_local_" + arquivo.getName();
                    DynamicTexture texturaDinamica = new DynamicTexture(imgFinal);
                    ResourceLocation texturaAplicavel = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(chave, texturaDinamica);

                    SkinManagerMod.skinAtual = texturaAplicavel;
                    SkinManagerMod.isSlimAtual = isSlim;
                    SkinManagerMod.imagemBaseAtual = imgFull; // crua, sem compensação — pra poder reprocessar depois
                    SkinManagerMod.chaveTexturaAtual = chave;

                    SkinApplier.aplicarSkinEModelo(texturaAplicavel, isSlim);
                    
                    // --- GATILHO DE PERSISTÊNCIA: Salva qual skin foi aplicada para carregar depois ---
                    SkinManagerMod.origemAtual = "LOCAL:" + arquivo.getName();
                    ConfigManager.salvarUltimaSkin(SkinManagerMod.origemAtual, isSlim);
                });
            } catch (Exception e) {}
        }).start();
    }
}