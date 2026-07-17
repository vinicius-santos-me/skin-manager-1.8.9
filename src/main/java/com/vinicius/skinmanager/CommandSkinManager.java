package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSkinManager extends CommandBase {

    // --- MAGIA CONTRA CLIQUES INFINITOS (Tokens de uso único) ---
    private static final Set<String> tokensAtivos = new HashSet<>();

    @Override public String getCommandName() { return "skinmanager"; }
    @Override public String getCommandUsage(ICommandSender sender) { return "/skinmanager [nick] ou /skinmanager details"; }
    @Override public int getRequiredPermissionLevel() { return 0; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            SkinManagerMod.abrirMenuNoProximoTick = true;

        } else if (args.length == 1) {
            String argumento = args[0];

            if (argumento.equalsIgnoreCase("details")) {
                new Thread(() -> {
                    try { Thread.sleep(100); } catch (Exception e) {}
                    Minecraft.getMinecraft().addScheduledTask(() ->
                            Minecraft.getMinecraft().displayGuiScreen(new GuiCapeManager())
                    );
                }).start();
            } else {
                if (!argumento.matches("[a-zA-Z0-9_]{3,16}")) {
                    ChatHelper.enviar(ChatHelper.VERMELHO + "Nick invalido! Use 3-16 letras, numeros ou _.");
                    return;
                }
                
                // Gera um Token único para essa pergunta no chat
                String token = UUID.randomUUID().toString().substring(0, 6);
                tokensAtivos.add(token);

                IChatComponent msg = new ChatComponentText(ChatHelper.AMARELO + "Deseja salvar a skin de " + argumento + " na sua lista de skins locais? ");
                
                // O comando agora carrega o Token escondido
                IChatComponent btnSim = new ChatComponentText(EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "[SIM]");
                btnSim.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skinmanager _sim " + argumento + " " + token));
                btnSim.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.GREEN + "Aplica a skin e salva na pasta.")));
                
                IChatComponent espaco = new ChatComponentText("  ");
                
                IChatComponent btnNao = new ChatComponentText(EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "[NAO]");
                btnNao.getChatStyle().setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/skinmanager _nao " + argumento + " " + token));
                btnNao.getChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(EnumChatFormatting.RED + "Apenas aplica a skin no boneco.")));
                
                msg.appendSibling(btnSim).appendSibling(espaco).appendSibling(btnNao);
                sender.addChatMessage(msg);
            }
            
        // --- AÇÕES DOS BOTÕES (Agora com validação de 3 argumentos) ---
        } else if (args.length == 3) {
            String acao = args[0];
            String nick = args[1];
            String token = args[2];

            // Verifica se o botão já foi clicado antes
            if (!tokensAtivos.contains(token)) {
                ChatHelper.enviar(ChatHelper.VERMELHO + "Esta acao ja expirou ou ja foi selecionada!");
                return;
            }
            
            // Destrói o token para impedir que a pessoa clique no SIM ou no NAO de novo
            tokensAtivos.remove(token);

            if (acao.equalsIgnoreCase("_sim")) {
                ChatHelper.enviar(ChatHelper.CINZA + "Aplicando e salvando na lista a skin de " + ChatHelper.AMARELO + nick + ChatHelper.CINZA + "...");
                SkinFetcher.buscarSkinNaMojang(nick, null, true, true);
                SkinFetcher.baixarSkinParaHD(nick);
                
            } else if (acao.equalsIgnoreCase("_nao")) {
                ChatHelper.enviar(ChatHelper.CINZA + "Aplicando skin de " + ChatHelper.AMARELO + nick + ChatHelper.CINZA + "...");
                SkinFetcher.buscarSkinNaMojang(nick, null, true, true);
            }
            
        } else {
            ChatHelper.enviar(ChatHelper.VERMELHO + "Uso: " + ChatHelper.AMARELO + "/skinmanager" + ChatHelper.VERMELHO + ", " + ChatHelper.AMARELO + "/skinmanager details" + ChatHelper.VERMELHO + " ou " + ChatHelper.AMARELO + "/skinmanager <nick>");
        }
    }
}