package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class ChatHelper {

    // Prefixo: [SkinManager] em cinza/dourado
    private static final String PREFIXO =
            EnumChatFormatting.DARK_GRAY + "[" +
                    EnumChatFormatting.GOLD + "SkinManager" +
                    EnumChatFormatting.DARK_GRAY + "] " +
                    EnumChatFormatting.RESET;

    public static final String VERDE    = EnumChatFormatting.GREEN.toString();
    public static final String AMARELO  = EnumChatFormatting.YELLOW.toString();
    public static final String VERMELHO = EnumChatFormatting.RED.toString();
    public static final String CINZA    = EnumChatFormatting.GRAY.toString();

    public static void enviar(String mensagem) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(PREFIXO + mensagem));
        }
    }
}