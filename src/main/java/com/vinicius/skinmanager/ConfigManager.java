package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ConfigManager {

    private static File getArquivoConfig() {
        File pasta = new File(Minecraft.getMinecraft().mcDataDir, "minhas_skins");
        if (!pasta.exists()) pasta.mkdirs();
        return new File(pasta, "last_skin.txt");
    }

    public static void salvarUltimaSkin(String origem, boolean isSlim) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(getArquivoConfig()), StandardCharsets.UTF_8))) {
            writer.println(origem + ";" + isSlim);
            writer.flush();
            System.out.println("[SkinManager-Save] Dados gravados: " + origem + " (Slim: " + isSlim + ")");
        } catch (Exception e) {
            System.out.println("[SkinManager-Error] Falha ao salvar config: " + e.getMessage());
        }
    }

    public static void limparConfig() {
        File config = getArquivoConfig();
        if (config.exists()) {
            config.delete();
            System.out.println("[SkinManager-Reset] Arquivo de config deletado. Voltando para skin original.");
        }
    }

    public static void carregarUltimaSkin() {
        File config = getArquivoConfig();
        if (!config.exists()) return;

        try (Scanner scanner = new Scanner(config, "UTF-8")) {
            if (scanner.hasNextLine()) {
                String linha = scanner.nextLine();
                String[] dados = linha.split(";");

                if (dados.length == 2) {
                    String origem = dados[0];
                    boolean isSlim = Boolean.parseBoolean(dados[1]);

                    if (origem.startsWith("ONLINE:")) {
                        // false = NÃO salvar novamente, false = NÃO enviar mensagem no chat (login silencioso)
                        SkinFetcher.buscarSkinNaMojang(origem.replace("ONLINE:", ""), isSlim, false, false);
                    } else if (origem.startsWith("LOCAL:")) {
                        String nomeArquivo = origem.replace("LOCAL:", "");
                        File arquivoSkin = new File(new File(Minecraft.getMinecraft().mcDataDir, "minhas_skins"), nomeArquivo);
                        if (arquivoSkin.exists()) {
                            LocalSkinLoader.carregarSkinLocal(arquivoSkin, isSlim);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[SkinManager-Error] Falha ao ler config: " + e.getMessage());
        }
    }
}