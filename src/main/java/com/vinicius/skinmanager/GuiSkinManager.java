package com.vinicius.skinmanager;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuiSkinManager extends GuiScreen {

    public static final String FIELD_LOCATION_SKIN     = "locationSkin";
    public static final String FIELD_LOCATION_SKIN_OBF = "field_178873_i";
    public static final String FIELD_SKIN_TYPE         = "skinType";
    public static final String FIELD_SKIN_TYPE_OBF     = "field_178872_h";
    public static final String FIELD_TEXTURES_LOADED   = "playerTexturesLoaded";
    public static final String FIELD_TEXTURES_LOADED_OBF = "field_181060_b";
    public static final String FIELD_PLAYER_INFO       = "playerInfo";
    public static final String FIELD_PLAYER_INFO_OBF   = "field_175157_a";
    public static final String FIELD_LOCATION_CAPE     = "locationCape";
    public static final String FIELD_LOCATION_CAPE_OBF = "field_178874_j";

    private GuiTextField campoNick;
    private GuiButton botaoBuscarNick, botaoAplicarSkin, botaoModelo, botaoResetar;
    private GuiButton botaoCapaToggle, botaoFechar;
    private ListaDeSkins lista;

    public static ResourceLocation previewSkin = null;
    public static boolean previewSlim = false;
    public static boolean previewIsOnline = false;
    public static String previewNickOnline = "";
    public static File previewArquivoLocal = null;

    public static final String[] ARQUIVOS_CAPAS = {
            "2011", "2012", "2013", "2015", "2016",
            "capa1", "capa2", "capa3", "capa4", "capa5",
            "capa6", "capa7", "capa8", "capa9", "capa10",
            "capa11", "capa12", "capa13", "capa14"
    };

    public static boolean previewCapaAtivada = false;
    public static int previewCapaIndex = 0;

    private boolean isLocalSlim = false;
    private GuiPreviewPlayer cloneJogador;

    private boolean buscandoOnline = false;
    private String statusBusca = ""; 

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        SkinApplier.carregarEstadoCapaMenu();

        int colEsquerda = this.width / 2;
        int centroDireita = (this.width / 4) * 3;

        // --- CORREÇÃO BUG 1 E 3: SEMPRE RESETA A VISUALIZAÇÃO PARA A SKIN ATUAL DO JOGADOR ---
        if (SkinManagerMod.skinAtual != null) {
            previewSkin = SkinManagerMod.skinAtual;
            previewSlim = SkinManagerMod.isSlimAtual;
        } else if (this.mc.thePlayer != null) {
            previewSkin = this.mc.thePlayer.getLocationSkin();
            previewSlim = "slim".equals(this.mc.thePlayer.getSkinType());
        }
        isLocalSlim = previewSlim;
        previewIsOnline = false;
        previewNickOnline = "";
        previewArquivoLocal = null;
        // -------------------------------------------------------------------------------------

        this.campoNick = new GuiTextField(0, this.fontRendererObj, 10, 25, colEsquerda - 90, 20);
        this.campoNick.setMaxStringLength(16);

        this.botaoBuscarNick = new GuiButton(1, colEsquerda - 75, 24, 65, 20, "Buscar");
        this.lista = new ListaDeSkins(this.mc, colEsquerda, this.height, 60, this.height - 90, 24);
        this.botaoCapaToggle = new GuiButton(4, 10, this.height - 55, colEsquerda - 20, 20, previewCapaAtivada ? "Capa: ATIVADA" : "Capa: DESATIVADA");

        this.botaoModelo = new GuiButton(3, centroDireita - 75, this.height - 80, 150, 20, previewSlim ? "Modelo: Alex" : "Modelo: Steve");
        this.botaoAplicarSkin = new GuiButton(2, centroDireita - 75, this.height - 55, 150, 20, "Aplicar Skin e Sair");
        this.botaoResetar = new GuiButton(7, centroDireita - 75, this.height - 30, 150, 20, "Resetar (Original)");
        
        this.botaoFechar = new GuiButton(6, this.width - 30, 10, 20, 20, "X");

        this.buttonList.add(this.botaoBuscarNick);
        this.buttonList.add(this.botaoAplicarSkin);
        this.buttonList.add(this.botaoModelo);
        this.buttonList.add(this.botaoResetar);
        this.buttonList.add(this.botaoCapaToggle);
        this.buttonList.add(this.botaoFechar);

        cloneJogador = new GuiPreviewPlayer(this.mc.theWorld, new GameProfile(UUID.randomUUID(), "Clone"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.lista.drawScreen(mouseX, mouseY, partialTicks);

        int centroDireita = (this.width / 4) * 3;
       
        int bonecoY = this.height / 2 + 48; 

        if (this.botaoModelo != null) {
            this.botaoModelo.displayString = previewSlim ? "Modelo: Alex" : "Modelo: Steve";
        }

        if (buscandoOnline) {
            long tempo = System.currentTimeMillis() / 500;
            String pontos = tempo % 2 == 0 ? "." : "..";
            this.drawCenteredString(this.fontRendererObj, EnumChatFormatting.YELLOW + "Buscando" + pontos, centroDireita, bonecoY - 60, 0xFFFFFF);
        } else if (!statusBusca.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj, statusBusca, centroDireita, bonecoY - 60, 0xFFFFFF);
        }

        if (previewSkin != null && !buscandoOnline) {
            cloneJogador.customSkin = previewSkin;
            cloneJogador.isSlim = previewSlim;
            cloneJogador.hasCape = previewCapaAtivada;
            cloneJogador.capeIndex = previewCapaIndex;

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            renderizarClone(centroDireita, bonecoY, 75, (float) centroDireita - mouseX, (float) (bonecoY - 110) - mouseY, cloneJogador);
        }

        this.drawString(this.fontRendererObj, "Buscar Nick Online:", 10, 12, 0xA0A0A0);
        this.drawCenteredString(this.fontRendererObj, "Visualizacao 3D", centroDireita, 15, 0x00FF00);
        this.campoNick.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderizarClone(int x, int y, int scale, float mX, float mY, GuiPreviewPlayer clone) {
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(515);
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 50.0F);
        GlStateManager.scale((float) (-scale), (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);

        if (mY > 30) mY = 30;
        if (mY < -30) mY = -30;
        clone.renderYawOffset = (float) Math.atan(mX / 40.0F) * 20.0F;
        clone.rotationYaw = (float) Math.atan(mX / 40.0F) * 40.0F;
        clone.rotationPitch = -((float) Math.atan(mY / 40.0F)) * 20.0F;
        clone.rotationYawHead = clone.rotationYaw;

        RenderManager rm = Minecraft.getMinecraft().getRenderManager();
        rm.setPlayerViewY(180.0F);
        rm.setRenderShadow(false);
        rm.renderEntityWithPosYaw(clone, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);

        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.disableDepth();
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {

        if (b.id == 1) {
            String nick = this.campoNick.getText().trim();
            if (nick.isEmpty()) return;
            if (!nick.matches("[a-zA-Z0-9_]{3,16}")) {
                statusBusca = EnumChatFormatting.RED + "Nick invalido! Use 3-16 letras, numeros ou _.";
                return;
            }
            if (buscandoOnline) return;

            buscandoOnline = true;
            statusBusca = "";
            this.botaoBuscarNick.enabled = false;

            SkinFetcher.buscarSkinPreview(nick,
                    () -> {
                        buscandoOnline = false;
                        this.botaoBuscarNick.enabled = true;
                        statusBusca = "";
                    },
                    () -> {
                        buscandoOnline = false;
                        this.botaoBuscarNick.enabled = true;
                        statusBusca = EnumChatFormatting.RED + "Nick nao encontrado ou sem conexao.";
                    }
            );

        } else if (b.id == 2) {
            if (previewIsOnline && !previewNickOnline.isEmpty() && previewSkin != null) {
                SkinManagerMod.skinAtual = previewSkin;
                SkinManagerMod.isSlimAtual = previewSlim;
                SkinApplier.aplicarSkinEModelo(previewSkin, previewSlim);
                ConfigManager.salvarUltimaSkin("ONLINE:" + previewNickOnline, previewSlim);
                ChatHelper.enviar(ChatHelper.VERDE + "Skin de " + ChatHelper.AMARELO + previewNickOnline + ChatHelper.VERDE + " aplicada!");
            } else if (!previewIsOnline && previewArquivoLocal != null) {
                LocalSkinLoader.carregarSkinLocal(previewArquivoLocal, previewSlim);
                ChatHelper.enviar(ChatHelper.VERDE + "Skin local " + ChatHelper.AMARELO + previewArquivoLocal.getName() + ChatHelper.VERDE + " aplicada!");
            }
            this.mc.displayGuiScreen(null);

        } else if (b.id == 3) {
            previewSlim = !previewSlim;
            this.isLocalSlim = previewSlim;

        } else if (b.id == 4) {
            previewCapaAtivada = !previewCapaAtivada;
            b.displayString = previewCapaAtivada ? "Capa: ATIVADA" : "Capa: DESATIVADA";
            SkinApplier.salvarCapa(previewCapaAtivada, previewCapaIndex);
            SkinApplier.aplicarCapaNoJogo(previewCapaAtivada, previewCapaIndex);

        } else if (b.id == 7) {
        	String meuNick = Minecraft.getMinecraft().getSession().getUsername();
            // Apaga o bloco de notas para não puxar a skin alterada de novo
            ConfigManager.limparConfig();
            SkinManagerMod.skinAtual = null; 
            
            // Busca a skin do seu próprio nick
            // false (não salvar no txt), false (não mandar o ChatHelper de "Aplicada" para não duplicar mensagem)
            SkinFetcher.buscarSkinNaMojang(meuNick, null, false, false);
            
            ChatHelper.enviar(ChatHelper.VERDE + "Skin resetada para a original da conta (" + meuNick + ")!");
            this.mc.displayGuiScreen(null);

        } else if (b.id == 6) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (this.campoNick.isFocused()) this.campoNick.textboxKeyTyped(c, k);
        if (!this.campoNick.isFocused() && k == Keyboard.KEY_K) this.mc.displayGuiScreen(null);
        super.keyTyped(c, k);
    }

    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public void handleMouseInput() throws IOException { super.handleMouseInput(); this.lista.handleMouseInput(); }
    @Override protected void mouseClicked(int x, int y, int b) throws IOException { super.mouseClicked(x, y, b); this.campoNick.mouseClicked(x, y, b); }

    class SkinCache { File arquivo; ResourceLocation texRosto, texCorpo; public SkinCache(File f) { this.arquivo = f; } }

    class ListaDeSkins extends GuiSlot {
        private final List<SkinCache> cache = new ArrayList<>();
        public ListaDeSkins(Minecraft mc, int w, int h, int t, int b, int s) {
            super(mc, w, h, t, b, s);
            File p = new File(mc.mcDataDir, "minhas_skins");
            if (!p.exists()) p.mkdirs();
            File[] files = p.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) cache.add(new SkinCache(f));
                new Thread(() -> {
                    for (SkinCache sc : cache) {
                        try {
                            BufferedImage raw = ImageIO.read(sc.arquivo);
                            final BufferedImage img = (raw.getHeight() == 32) ? new ImageBufferDownload().parseUserSkin(raw) : raw;
                            mc.addScheduledTask(() -> {
                                sc.texRosto = mc.getTextureManager().getDynamicTextureLocation("r_" + sc.arquivo.getName(), new DynamicTexture(cortar(img)));
                                sc.texCorpo = mc.getTextureManager().getDynamicTextureLocation("c_" + sc.arquivo.getName(), new DynamicTexture(img));
                            });
                        } catch (Exception e) {}
                    }
                }).start();
            }
        }

        private BufferedImage cortar(BufferedImage i) {
            BufferedImage r = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = r.createGraphics();
            g.drawImage(i, 0, 0, 8, 8, 8, 8, 16, 16, null);
            g.drawImage(i, 0, 0, 8, 8, 40, 8, 48, 16, null);
            g.dispose();
            return r;
        }

        @Override protected int getScrollBarX() { return this.width - 15; }
        @Override public int getListWidth() { return this.width - 35; }
        @Override protected int getSize() { return cache.size(); }

        @Override protected void elementClicked(int i, boolean b, int x, int y) {
            SkinCache s = cache.get(i);
            if (s != null && s.texCorpo != null) {
                GuiSkinManager.previewSkin = s.texCorpo;
                GuiSkinManager.previewSlim = GuiSkinManager.this.isLocalSlim;
                GuiSkinManager.previewIsOnline = false;
                GuiSkinManager.previewArquivoLocal = s.arquivo;
                statusBusca = "";
            }
        }

        @Override protected boolean isSelected(int i) {
            return !GuiSkinManager.previewIsOnline
                    && GuiSkinManager.previewArquivoLocal != null
                    && GuiSkinManager.previewArquivoLocal.equals(cache.get(i).arquivo);
        }

        @Override protected void drawBackground() {}

        @Override protected void drawSlot(int id, int x, int y, int h, int mx, int my) {
            SkinCache s = cache.get(id);
            if (s.texRosto != null) {
                mc.getTextureManager().bindTexture(s.texRosto);
                net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, 8, 8, 16, 16, 8, 8);
            }
            GuiSkinManager.this.drawString(mc.fontRendererObj, s.arquivo.getName(), x + 22, y + 4, 0xFFFFFF);
        }
    }
}