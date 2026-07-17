package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.util.ResourceLocation;
import java.io.IOException;

public class GuiCapeManager extends GuiScreen {

    private ListaDeCapas listaCapas;

    @Override
    public void initGui() {
        super.initGui();
        this.listaCapas = new ListaDeCapas(this.mc, this.width, this.height, 32, this.height - 32, 40);
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height - 28, 200, 20, "Aplicar e Fechar"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.listaCapas.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, "Selecione sua Capa", this.width / 2, 10, 0x00FF00);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.listaCapas.handleMouseInput();
    }

    class ListaDeCapas extends GuiSlot {
        public ListaDeCapas(Minecraft mc, int w, int h, int t, int b, int s) {
            super(mc, w, h, t, b, s);
        }

        @Override protected int getSize() {
            return GuiSkinManager.ARQUIVOS_CAPAS.length;
        }

        @Override protected void elementClicked(int i, boolean b, int x, int y) {
            GuiSkinManager.previewCapaIndex = i;
            SkinApplier.salvarCapa(GuiSkinManager.previewCapaAtivada, i);
            SkinApplier.aplicarCapaNoJogo(GuiSkinManager.previewCapaAtivada, i);
        }

        @Override protected boolean isSelected(int i) {
            return GuiSkinManager.previewCapaIndex == i;
        }

        @Override protected void drawBackground() {}

        // --- CORREÇÃO DO ARRASTAR DA BARRA DE CAPAS ---
        @Override protected int getScrollBarX() {
            return this.width / 2 + 130;
        }

        @Override protected void drawSlot(int id, int x, int y, int h, int mx, int my) {
            ResourceLocation tex = new ResourceLocation("skinmanager", "textures/cape/" + GuiSkinManager.ARQUIVOS_CAPAS[id] + ".png");
            mc.getTextureManager().bindTexture(tex);

            // Desenha e centraliza a capa perfeitamente
            int drawX = x + (this.getListWidth() / 2) - 10;
            net.minecraft.client.gui.Gui.drawScaledCustomSizeModalRect(drawX, y + 2, 1, 1, 10, 16, 20, 32, 64, 32);
        }
    }
}