package com.vinicius.skinmanager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;

public class KeybindHandler {

    private final KeyBinding openMenuKey;

    public KeybindHandler() {
        openMenuKey = new KeyBinding("Abrir Menu de Skins", Keyboard.KEY_K, "Skin Manager");
        ClientRegistry.registerKeyBinding(openMenuKey);
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        if (openMenuKey.isPressed()) {
            // Chamando o nome exato da sua tela
            Minecraft.getMinecraft().displayGuiScreen(new GuiSkinManager());
        }
    }
}