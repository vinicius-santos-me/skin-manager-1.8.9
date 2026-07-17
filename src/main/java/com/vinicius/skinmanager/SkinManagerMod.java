package com.vinicius.skinmanager;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = SkinManagerMod.MODID, version = SkinManagerMod.VERSION, clientSideOnly = true)
public class SkinManagerMod {
    public static final String MODID = "skinmanager";
    public static final String VERSION = "1.0";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static ResourceLocation skinAtual = null;
    public static boolean isSlimAtual = false;
    public static boolean abrirMenuNoProximoTick = false;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("Inicializando o SkinManager Mod...");
        MinecraftForge.EVENT_BUS.register(new KeybindHandler());
        MinecraftForge.EVENT_BUS.register(new PersistenceHandler());
        MinecraftForge.EVENT_BUS.register(new SkinEventHandler());
        ClientCommandHandler.instance.registerCommand(new CommandSkinManager());
    }
}