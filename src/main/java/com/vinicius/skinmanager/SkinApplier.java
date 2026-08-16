package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ChatComponentText;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Properties;

public class SkinApplier {

    // --- Cache de Capa ---
    private static boolean capaAtivadaCache = false;
    private static int capaIndexCache = 0;
    private static boolean cacheCarregado = false;

    // --- Ferramenta Suprema Contra Obfuscação (Por Tipo) ---
    // Agora todos são PUBLIC para o GuiSkinManager conseguir acessar e forçar atualizações!
    public static Field campoSkin = null;
    public static Field campoCapa = null;
    public static Field campoSkinType = null;
    public static Field campoTexturesLoaded = null;
    public static Field campoPlayerInfoCache = null;

    private static File getCapaFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "skinmanager_capas.properties");
    }

    public static void inicializarCampos() throws Exception {
        if (campoSkin != null) return; // Já foi mapeado

        Minecraft mcTmp = Minecraft.getMinecraft();
        NetworkPlayerInfo infoReal = null;
        if (mcTmp.thePlayer != null && mcTmp.getNetHandler() != null) {
            infoReal = mcTmp.getNetHandler().getPlayerInfo(mcTmp.thePlayer.getUniqueID());
        }

        java.util.List<Field> candidatosString = new java.util.ArrayList<>();

        // 1. Vasculha NetworkPlayerInfo baseando-se nos TIPOS das variáveis
        for (Field f : NetworkPlayerInfo.class.getDeclaredFields()) {
            if (f.getType() == ResourceLocation.class) {
                if (campoSkin == null) {
                    campoSkin = f; // 1º ResourceLocation: Skin
                } else if (campoCapa == null) {
                    campoCapa = f; // 2º ResourceLocation: Capa
                }
            } else if (f.getType() == String.class) {
                f.setAccessible(true);
                candidatosString.add(f); // guarda TODOS os candidatos, não só o 1º
            } else if (f.getType() == boolean.class) {
                if (campoTexturesLoaded == null) {
                    campoTexturesLoaded = f; // Único boolean: playerTexturesLoaded
                }
            }
        }

        // 1b. Entre os campos String candidatos, o campo skinType certo é o
        // único que JÁ contém literalmente "default" ou "slim" num
        // NetworkPlayerInfo real (o jogo preenche isso a partir do pacote de
        // player-list antes da gente mexer em qualquer coisa). Testar por
        // VALOR em vez de confiar no primeiro String que aparecer evita
        // pegar o campo errado caso essa build do Forge tenha algum outro
        // campo String além do skinType.
        for (Field f : candidatosString) {
            try {
                Object valor = (infoReal != null) ? f.get(infoReal) : null;
                if ("default".equals(valor) || "slim".equals(valor)) {
                    campoSkinType = f;
                    break;
                }
            } catch (Exception ignored) {}
        }
        if (campoSkinType == null && !candidatosString.isEmpty()) {
            campoSkinType = candidatosString.get(0); // fallback: melhor que nada
        }

        // 2. Vasculha o AbstractClientPlayer atrás da variável do NetworkPlayerInfo
        for (Field f : AbstractClientPlayer.class.getDeclaredFields()) {
            if (NetworkPlayerInfo.class.isAssignableFrom(f.getType())) {
                campoPlayerInfoCache = f;
                break;
            }
        }

        SkinManagerMod.LOGGER.info("[SkinApplier] Campos mapeados -> skin:{} capa:{} skinType:{} ({} candidatos String) texturesLoaded:{} playerInfoCache:{}",
                campoSkin != null ? campoSkin.getName() : "NULO",
                campoCapa != null ? campoCapa.getName() : "NULO",
                campoSkinType != null ? campoSkinType.getName() : "NULO",
                candidatosString.size(),
                campoTexturesLoaded != null ? campoTexturesLoaded.getName() : "NULO",
                campoPlayerInfoCache != null ? campoPlayerInfoCache.getName() : "NULO");

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

    /**
     * Reaplica a skin ATUAL com um modelo (slim/default) diferente,
     * reprocessando a imagem crua guardada (SkinManagerMod.imagemBaseAtual).
     * Necessário porque a compensação do desalinhamento do braço Slim
     * depende do modelo escolhido — só trocar o ResourceLocation/flag sem
     * reprocessar deixava a textura antiga (sem compensação, ou compensada
     * pro modelo errado) aplicada quando você só trocava Steve/Alex sem
     * reescolher a skin (local ou online).
     */
    public static boolean reaplicarComNovoModelo(boolean slim) {
        if (SkinManagerMod.imagemBaseAtual == null || SkinManagerMod.chaveTexturaAtual == null) {
            // Sem imagem crua guardada (ex.: skin de uma versão antiga do
            // mod antes dessa mudança) — cai no comportamento antigo.
            return aplicarSkinEModelo(SkinManagerMod.skinAtual, slim);
        }

        BufferedImage imgFinal = slim
                ? compensarDesalinhamentoSlim(SkinManagerMod.imagemBaseAtual)
                : SkinManagerMod.imagemBaseAtual;

        DynamicTexture texturaDinamica = new DynamicTexture(imgFinal);
        ResourceLocation novoRes = Minecraft.getMinecraft().getTextureManager()
                .getDynamicTextureLocation(SkinManagerMod.chaveTexturaAtual, texturaDinamica);

        SkinManagerMod.skinAtual = novoRes;
        return aplicarSkinEModelo(novoRes, slim);
    }

    public static boolean aplicarSkinEModelo(ResourceLocation tex, boolean slim) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return false;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info != null) {
            try {
                inicializarCampos();

                if (campoSkin != null) campoSkin.set(info, tex);
                if (campoSkinType != null) {
                    campoSkinType.set(info, slim ? "slim" : "default");
                    Object valorDepois = campoSkinType.get(info);
                    SkinManagerMod.LOGGER.info("[SkinApplier] skinType setado pra '{}' -> valor lido de volta: '{}'",
                            slim ? "slim" : "default", valorDepois);
                }
                if (campoTexturesLoaded != null) campoTexturesLoaded.set(info, true);
                if (campoPlayerInfoCache != null) campoPlayerInfoCache.set(mc.thePlayer, null);

                // NÃO precisa recriar/injetar RenderPlayer manualmente: para
                // qualquer AbstractClientPlayer (jogador local, outro jogador,
                // ou o boneco de preview do menu), o RenderManager NÃO usa o
                // Map<Class,Render> genérico. Ele consulta getSkinType() do
                // jogador TODO FRAME e escolhe, ao vivo, entre dois RenderPlayer
                // já prontos guardados no seu próprio Map<String,RenderPlayer>
                // interno (chaves "default" e "slim"). Como campoSkinType acima
                // já foi atualizado no NetworkPlayerInfo, o braço muda sozinho
                // no próximo frame — não existe cache de geometria pra limpar.
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
    
    public static void resetarSkinOriginal() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
        if (info != null) {
            try {
                inicializarCampos(); 

                // 1. Zera as variáveis globais do seu mod
                SkinManagerMod.skinAtual = null;
                SkinManagerMod.isSlimAtual = false;

                // 2. Apaga a skin customizada da memória
                if (campoSkin != null) campoSkin.set(info, null);
                
                // 3. Força o modelo original (Steve) caso estivesse com a Alex
                if (campoSkinType != null) campoSkinType.set(info, "default");
                
                // 4. Diz para o Minecraft baixar a skin verdadeira
                if (campoTexturesLoaded != null) campoTexturesLoaded.set(info, false);
                
                // 5. Destrói o cache para aplicar na mesma hora — o braço volta
                // ao modelo default sozinho no próximo frame (ver nota em
                // aplicarSkinEModelo sobre o skinMap do RenderManager)
                if (campoPlayerInfoCache != null) campoPlayerInfoCache.set(mc.thePlayer, null);

            } catch (Exception e) {
                SkinManagerMod.LOGGER.error("Erro ao resetar skin!", e);
            }
        }
    }

    // ==========================================================================
    // COMPENSAÇÃO EXPERIMENTAL DO DESALINHAMENTO DO BRAÇO SLIM (bug da Mojang)
    // ==========================================================================
    // O ModelPlayer da própria Mojang usa a MESMA origem de textura pro braço
    // direito no modelo Slim (40,16) que usa no Classic — só muda a largura da
    // caixa (3px em vez de 4px). Só que o Minecraft calcula automaticamente
    // onde cada face da caixa fica na textura com base na LARGURA da caixa, e
    // isso desloca 1 pixel pra esquerda onde as faces de FORA e de TRÁS do
    // braço são lidas:
    //   - Classic: face de fora lê colunas 48-51, face de trás lê 52-55
    //   - Slim:    face de fora lê colunas 47-50, face de trás lê 51-53
    // Ou seja, no modelo Slim, a face de fora "rouba" a última coluna da face
    // da FRENTE (coluna 47) e a face de trás "rouba" a última coluna da face
    // de fora (coluna 51). É esse pixel roubado que aparece como "pedaço de
    // outra parte da skin" embaixo da mão.
    //
    // Esse método tenta compensar isso: pega a textura já processada e força
    // as colunas 47 e 51 (na base e na camada externa/jaqueta do braço
    // direito) a mostrarem o mesmo conteúdo que já está nas colunas 48 e 52 —
    // ou seja, "empurra" a face de fora/trás pro lugar certo antes do jogo ler.
    //
    // AVISO: eu derivei essa matemática lendo o código-fonte do ModelPlayer,
    // mas não tenho como renderizar o jogo aqui pra conferir visualmente. Por
    // enquanto só trata o braço DIREITO (o mais visível/comentado). Testa e
    // me diz se melhorou, piorou, ou não mudou nada — se piorar, é só trocar
    // COMPENSAR_DESALINHAMENTO_SLIM pra false que desliga sem precisar reverter
    // o resto do arquivo.
    public static boolean COMPENSAR_DESALINHAMENTO_SLIM = true;

    public static BufferedImage compensarDesalinhamentoSlim(BufferedImage origem) {
        if (!COMPENSAR_DESALINHAMENTO_SLIM || origem == null || origem.getWidth() < 64 || origem.getHeight() < 32) {
            return origem;
        }

        BufferedImage copia = new BufferedImage(origem.getWidth(), origem.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copia.createGraphics();
        g.drawImage(origem, 0, 0, null);
        g.dispose();

        // Braço direito: base em (40,16), camada externa/jaqueta em (40,32)
        corrigirBraco(copia, 40, 16);
        if (copia.getHeight() >= 48) corrigirBraco(copia, 40, 32);

        // Braço esquerdo (só existe em skins 64x64): base em (32,48),
        // camada externa/jaqueta em (48,48) — antes eu só tratava o braço
        // direito; conferindo o arquivo .png real que você mandou, o
        // esquerdo tem exatamente o mesmo vazamento (coluna 39 puxando o
        // ciano da face de cima), por isso a correção anterior não bastou.
        if (copia.getWidth() >= 64 && copia.getHeight() >= 64) {
            corrigirBraco(copia, 32, 48);
            corrigirBraco(copia, 48, 48);
        }

        return copia;
    }

    /**
     * Corrige o vazamento de 1px do modelo Slim pra um braço cuja origem de
     * textura é (u,v) — profundidade da caixa sempre 4. A face de BAIXO e a
     * de FORA (ambas na coluna u+7 no modelo Slim) roubam a última coluna
     * da face de CIMA/FRENTE (u+8); a face de TRÁS (coluna u+11) rouba a
     * última coluna da face de FORA (u+12).
     */
    private static void corrigirBraco(BufferedImage img, int u, int v) {
        empurrarColuna(img, u + 7, u + 8, v, 4);       // face de baixo
        empurrarColuna(img, u + 7, u + 8, v + 4, 12);  // face de fora
        empurrarColuna(img, u + 11, u + 12, v + 4, 12); // face de trás
    }

    private static void empurrarColuna(BufferedImage img, int colDestino, int colOrigem, int linhaInicial, int altura) {
        if (colDestino >= img.getWidth() || colOrigem >= img.getWidth()) return;
        for (int y = linhaInicial; y < linhaInicial + altura && y < img.getHeight(); y++) {
            img.setRGB(colDestino, y, img.getRGB(colOrigem, y));
        }
    }
}