package com.vinicius.skinmanager;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class GuiPreviewPlayer extends EntityOtherPlayerMP {

    public ResourceLocation customSkin;
    public boolean isSlim;
    public boolean hasCape = false;
    public int capeIndex = 0;

    public GuiPreviewPlayer(World worldIn, GameProfile profileIn) {
        super(worldIn, profileIn);
    }

    @Override
    public ResourceLocation getLocationSkin() { return customSkin; }

    @Override
    public String getSkinType() { return isSlim ? "slim" : "default"; }

    @Override
    public ResourceLocation getLocationCape() {
        if (hasCape) {
            return new ResourceLocation("skinmanager", "textures/cape/" + GuiSkinManager.ARQUIVOS_CAPAS[capeIndex] + ".png");
        }
        return null;
    }

    @Override
    public boolean isWearing(EnumPlayerModelParts p) {
        if (p == EnumPlayerModelParts.CAPE) return hasCape;
        return true;
    }

    @Override
    public boolean hasPlayerInfo() { return false; }
}