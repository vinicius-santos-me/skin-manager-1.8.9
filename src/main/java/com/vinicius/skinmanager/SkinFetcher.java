package com.vinicius.skinmanager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class SkinFetcher {

    private static final int TIMEOUT_MS = 5000;
    private static final Map<String, String[]> sessionCache = new HashMap<>();

    public static void buscarSkinNaMojang(String nick, Boolean isSlimForcado, boolean salvarConfig, boolean mostrarMensagem) {
        new Thread(() -> {
            try {
                String[] data = getMojangData(nick);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    ResourceLocation res = new ResourceLocation("skinmanager", "active_" + nick.toLowerCase());
                    ThreadDownloadImageData img = new ThreadDownloadImageData(null, data[0], null, new ImageBufferDownload());
                    Minecraft.getMinecraft().getTextureManager().loadTexture(res, img);

                    SkinManagerMod.skinAtual = res;
                    SkinManagerMod.isSlimAtual = (isSlimForcado != null) ? isSlimForcado : Boolean.parseBoolean(data[1]);

                    SkinApplier.aplicarSkinEModelo(res, SkinManagerMod.isSlimAtual);
                    
                    if (salvarConfig) {
                        ConfigManager.salvarUltimaSkin("ONLINE:" + nick, SkinManagerMod.isSlimAtual);
                    }
                    
                    if (mostrarMensagem) {
                        ChatHelper.enviar(ChatHelper.VERDE + "Skin de " + ChatHelper.AMARELO + nick + ChatHelper.VERDE + " aplicada!");
                    }
                });
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (mostrarMensagem) {
                        ChatHelper.enviar(ChatHelper.VERMELHO + "Erro ao procurar nick ou sem ligacao.");
                    }
                });
            }
        }).start();
    }

    public static void buscarSkinPreview(String nick, Runnable aoTerminar, Runnable aoErrar) {
        new Thread(() -> {
            try {
                String[] data = getMojangData(nick);
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    ResourceLocation res = new ResourceLocation("skinmanager", "prev_" + nick.toLowerCase());
                    ThreadDownloadImageData img = new ThreadDownloadImageData(null, data[0], null, new ImageBufferDownload());
                    Minecraft.getMinecraft().getTextureManager().loadTexture(res, img);

                    GuiSkinManager.previewSkin = res;
                    GuiSkinManager.previewSlim = Boolean.parseBoolean(data[1]);
                    GuiSkinManager.previewIsOnline = true;
                    GuiSkinManager.previewNickOnline = nick;
                    if (aoTerminar != null) aoTerminar.run();
                });
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    if (aoErrar != null) aoErrar.run();
                });
            }
        }).start();
    }


    public static void baixarSkinParaHD(String nick) {
        new Thread(() -> {
            try {
                String[] data = getMojangData(nick);
                String urlSkin = data[0];

                HttpURLConnection con = (HttpURLConnection) new URL(urlSkin).openConnection();
                con.setConnectTimeout(TIMEOUT_MS);
                con.setReadTimeout(TIMEOUT_MS);
                BufferedImage img = ImageIO.read(con.getInputStream());

                if (img == null) throw new Exception("A imagem retornou nula");

                File dir = new File(Minecraft.getMinecraft().mcDataDir, "minhas_skins");
                if (!dir.exists()) dir.mkdirs();

                String nomeBase = nick.toLowerCase();
                File arquivoSkin = new File(dir, nomeBase + ".png");
                int contador = 2;
                
                while (arquivoSkin.exists()) {
                    arquivoSkin = new File(dir, nomeBase + "_" + contador + ".png");
                    contador++;
                }

                ImageIO.write(img, "png", arquivoSkin);

                final String nomeSalvo = arquivoSkin.getName(); 

                Minecraft.getMinecraft().addScheduledTask(() -> {
                    ChatHelper.enviar(ChatHelper.VERDE + "A Skin foi salva como " + ChatHelper.AMARELO + nomeSalvo + ChatHelper.VERDE + " com sucesso!");
                });
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                        ChatHelper.enviar(ChatHelper.VERMELHO + "Erro ao baixar a skin para o HD.")
                );
            }
        }).start();
    }

    private static String[] getMojangData(String nick) throws Exception {
        if (sessionCache.containsKey(nick.toLowerCase())) {
            return sessionCache.get(nick.toLowerCase());
        }
        HttpURLConnection con1 = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + nick).openConnection();
        con1.setConnectTimeout(TIMEOUT_MS);
        con1.setReadTimeout(TIMEOUT_MS);
        JsonObject j1 = new JsonParser().parse(new InputStreamReader(con1.getInputStream())).getAsJsonObject();

        HttpURLConnection con2 = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + j1.get("id").getAsString()).openConnection();
        con2.setConnectTimeout(TIMEOUT_MS);
        con2.setReadTimeout(TIMEOUT_MS);
        JsonObject j2 = new JsonParser().parse(new InputStreamReader(con2.getInputStream())).getAsJsonObject();

        String b64 = j2.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
        JsonObject j3 = new JsonParser().parse(new String(Base64.getDecoder().decode(b64))).getAsJsonObject();
        JsonObject skinObj = j3.getAsJsonObject("textures").getAsJsonObject("SKIN");

        String url = skinObj.get("url").getAsString();
        boolean slim = skinObj.has("metadata") && "slim".equals(skinObj.getAsJsonObject("metadata").get("model").getAsString());

        String[] result = new String[]{url, String.valueOf(slim)};
        sessionCache.put(nick.toLowerCase(), result);
        return result;
    }
}