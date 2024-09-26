package de.guntram.mcmod.durabilityviewer.client.gui;

import com.google.common.collect.Ordering;
import com.mojang.blaze3d.systems.RenderSystem;
import de.guntram.mcmod.durabilityviewer.handler.ConfigurationHandler;
import de.guntram.mcmod.durabilityviewer.itemindicator.InventorySlotsIndicator;
import de.guntram.mcmod.durabilityviewer.itemindicator.ItemCountIndicator;
import de.guntram.mcmod.durabilityviewer.itemindicator.ItemDamageIndicator;
import de.guntram.mcmod.durabilityviewer.itemindicator.ItemIndicator;
import de.guntram.mcmod.durabilityviewer.itemindicator.TREnergyIndicator;
import de.guntram.mcmod.durabilityviewer.sound.ColytraBreakingWarner;
import de.guntram.mcmod.durabilityviewer.sound.ItemBreakingWarner;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4fStack;
import team.reborn.energy.api.EnergyStorageUtil;


public class GuiItemDurability
{
    
    private static final Logger LOGGER = LogManager.getLogger();
    private final MinecraftClient minecraft;
    private static boolean visible;
    private final TextRenderer fontRenderer;
    
    private long lastWarningTime;
    private ItemStack lastWarningItem;
    
    private static final int iconWidth=16;
    private static final int iconHeight=16;
    private static final int spacing=2;
    
    private static boolean haveTrinketsApi = false;
    private static boolean haveTRCore = false;
    
    private ItemBreakingWarner mainHandWarner, offHandWarner, helmetWarner, chestWarner, pantsWarner, bootsWarner;
    private ItemBreakingWarner colytraWarner;
    private ItemBreakingWarner[] trinketWarners;
    
    public static void toggleVisibility() {
        visible=!visible;
    }
    
    public GuiItemDurability() {
        minecraft = MinecraftClient.getInstance();
        fontRenderer = minecraft.textRenderer;
        visible=true;
        
        mainHandWarner=new ItemBreakingWarner();
        offHandWarner=new ItemBreakingWarner();
        helmetWarner=new ItemBreakingWarner();
        chestWarner=new ItemBreakingWarner();
        pantsWarner=new ItemBreakingWarner();
        bootsWarner=new ItemBreakingWarner();
        colytraWarner=new ColytraBreakingWarner();
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            LOGGER.info("Using trinkets in DurabilityViewer");
            int slotCount = getTrinketSlotCount(minecraft.player);
            haveTrinketsApi = (slotCount > 0);
            trinketWarners = new ItemBreakingWarner[slotCount];
            for (int i=0; i<trinketWarners.length; i++) {
                trinketWarners[i]=new ItemBreakingWarner();
            }
        } else {
            LOGGER.info("DurabilityViewer did not find Trinkets API");
            trinketWarners = new ItemBreakingWarner[0];
        }

        if (FabricLoader.getInstance().isModLoaded("techreborn") ) {
            haveTRCore = true;
            LOGGER.info("Using TechReborn in DurabilityViewer");
        } else {
            LOGGER.info("DurabilityViewer did not find Tech Reborn");
        }
    }

    private int getSimilarCount(final ItemStack comparator){
        int count = 0;
        if(minecraft.player==null || minecraft.player.getInventory()==null || comparator == null) return count;
        for (final ItemStack stack : minecraft.player.getInventory().main){
            if(stack==null) continue;
            if(ItemStack.areItemsAndComponentsEqual(stack,comparator)) count+= stack.getCount();
        }
        final ItemStack stack = minecraft.player.getOffHandStack();
        if(ItemStack.areItemsAndComponentsEqual(stack,comparator)) count+=stack.getCount();
    return count;
    }
    
    private ItemStack getFirstProjectileStack(RangedWeaponItem item) {
        Predicate<ItemStack> validProjectiles = item.getProjectiles();
        if (validProjectiles.test(minecraft.player.getOffHandStack())) {
            return minecraft.player.getOffHandStack();
        }
        if (validProjectiles.test(minecraft.player.getMainHandStack())) {
            return minecraft.player.getMainHandStack();
        }
        int size=minecraft.player.getInventory().size();
        for (int i = 0; i < size; ++i) {
            final ItemStack itemstack = minecraft.player.getInventory().getStack(i);
            if (validProjectiles.test(itemstack)) {
                return itemstack;
            }
        }
        return null;
    }


    private class RenderSize {
        int width;
        int height;
        
        RenderSize(int w, int h) {
            width=w; height=h;
        }
    }
    
    private enum RenderPos {
        left, over, right
    }

    public void onRenderGameOverlayPost(DrawContext context, float partialTicks) {

        PlayerEntity player = minecraft.player;
        ItemStack needToWarn=null;
        
        ItemIndicator mainHand, offHand;
        mainHand = indicatorType(player, EquipmentSlot.MAINHAND);
        offHand  = indicatorType(player, EquipmentSlot.OFFHAND);
        
        ItemStack chestItem = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemIndicator colytra = null;
        //TODO Temporarily disabled as colytra is not updated.
        /*if (chestItem != null && chestItem.getNbt()!= null && chestItem.getNbt().contains("colytra:ElytraUpgrade")) {
            colytra = new ColytraDamageIndicator(chestItem);
        }*/
        
        ItemIndicator boots = indicatorType(player,EquipmentSlot.FEET);
        ItemIndicator leggings = indicatorType(player,EquipmentSlot.LEGS);
        ItemIndicator chestplate = indicatorType(player,EquipmentSlot.CHEST);
        ItemIndicator helmet = indicatorType(player,EquipmentSlot.HEAD);
        ItemIndicator projectiles = null;
        ItemIndicator invSlots = (ConfigurationHandler.getShowChestIcon() ? new InventorySlotsIndicator(minecraft.player.getInventory()) : null);

        if (mainHandWarner.checkBreaks(player.getEquippedStack(EquipmentSlot.MAINHAND))) needToWarn = player.getEquippedStack(EquipmentSlot.MAINHAND);
        if (offHandWarner.checkBreaks(player.getEquippedStack(EquipmentSlot.OFFHAND))) needToWarn = player.getEquippedStack(EquipmentSlot.OFFHAND);
        if (bootsWarner.checkBreaks(player.getEquippedStack(EquipmentSlot.FEET))) needToWarn = player.getEquippedStack(EquipmentSlot.FEET);
        if (pantsWarner.checkBreaks(player.getEquippedStack(EquipmentSlot.LEGS))) needToWarn = player.getEquippedStack(EquipmentSlot.LEGS);
        if (chestWarner.checkBreaks(chestItem)) needToWarn = chestItem;
        if (helmetWarner.checkBreaks(player.getEquippedStack(EquipmentSlot.HEAD))) needToWarn = player.getEquippedStack(EquipmentSlot.HEAD);
        if (colytraWarner.checkBreaks(chestItem)) needToWarn = chestItem;

        ItemIndicator[] trinkets;
        if (haveTrinketsApi) {
            List<ItemStack> equipped = getTrinkets(player);
            
            trinkets = new ItemIndicator[equipped.size()];
            if (trinkets.length > trinketWarners.length) {
                // Apparently this can happen when joining a server that defines 
                // more trinkets than the client?
                trinketWarners = new ItemBreakingWarner[trinkets.length];
                for (int i=0; i<trinketWarners.length; i++) {
                    trinketWarners[i]= new ItemBreakingWarner();
                }
            }
            LOGGER.debug("know about "+trinkets.length+" trinkets, invSize is "+equipped.size()+", have "+trinketWarners.length+" warners");
            for (int i=0; i<trinkets.length; i++) {
                trinkets[i]=new ItemDamageIndicator(equipped.get(i), ConfigurationHandler.getShowAllTrinkets());
                if (needToWarn == null && trinketWarners[i].checkBreaks(equipped.get(i))) {
                    needToWarn = equipped.get(i);
                }
                LOGGER.debug("trinket position "+i+" has item "+equipped.get(i).getItem().toString());
            }
        } else {
            trinkets = new ItemIndicator[0];            
        }
        
        if (needToWarn!= null) {
            if ((ConfigurationHandler.getWarnMode() & 1) == 1) {
                ItemBreakingWarner.playWarningSound();
            }
            lastWarningTime = System.currentTimeMillis();
            lastWarningItem = needToWarn;
        }
        
        long timeSinceLastWarning = System.currentTimeMillis() - lastWarningTime;
        if (timeSinceLastWarning < 1000 && (ConfigurationHandler.getWarnMode() & 2) == 2) {
            renderItemBreakingOverlay(context, lastWarningItem, timeSinceLastWarning);
        }
        
        // Moved this check to down here, in order to play the 
        // warning sound / do the visible 
        if (!visible
        // ||  minecraft.player.abilities.creativeMode
        ||  minecraft.getDebugHud().shouldShowDebugHud()) {
            return;
        }

        if (mainHand.getItemStack().getItem() instanceof RangedWeaponItem rwi)
            projectiles = new ItemCountIndicator(getFirstProjectileStack(rwi),
                    getSimilarCount(getFirstProjectileStack(rwi)));
        else if (offHand.getItemStack().getItem() instanceof RangedWeaponItem rwi){
            projectiles = new ItemCountIndicator(getFirstProjectileStack(rwi),
                    getSimilarCount(getFirstProjectileStack(rwi)));
        }

        Window mainWindow = MinecraftClient.getInstance().getWindow();
        RenderSize armorSize, toolsSize, trinketsSize;
        if (ConfigurationHandler.getArmorAroundHotbar()) {
            armorSize = new RenderSize(0, 0);
        } else {
            armorSize=this.renderItems(context, 0, 0, false, RenderPos.left, 0, boots, leggings, colytra, chestplate, helmet);
        }
        toolsSize=this.renderItems(context, 0, 0, false, RenderPos.right, 0, invSlots, mainHand, offHand, projectiles);
        trinketsSize = this.renderItems(context, 0, 0, false, RenderPos.left, 0, trinkets);
        
        int totalHeight=(toolsSize.height > armorSize.height ? toolsSize.height : armorSize.height);
        if (trinketsSize.height > totalHeight) { totalHeight = trinketsSize.height; }
        if (trinketsSize.width == 0 && trinkets.length > 0 && ConfigurationHandler.getShowAllTrinkets()) {
            trinketsSize.width = iconWidth+spacing * 2;
        }
        int xposArmor, xposTools, xposTrinkets, ypos, xpos;

        switch (ConfigurationHandler.getCorner()) {
            case TOP_LEFT:      
                xposArmor=5;
                xposTools=5+armorSize.width;
                xposTrinkets=5+armorSize.width+trinketsSize.width;
                ypos=5;
                break;
            case TOP_RIGHT:
                xposArmor=mainWindow.getScaledWidth()-5-armorSize.width;
                xposTools=xposArmor-toolsSize.width;
                xposTrinkets=xposTools-trinketsSize.width;
                ypos=60;   // below buff/debuff effects
                break;
            case BOTTOM_LEFT:
                xposArmor=5;
                xposTools=5+armorSize.width;
                xposTrinkets=5+armorSize.width+trinketsSize.width;
                ypos=mainWindow.getScaledHeight()-5-totalHeight;
                break;
            case BOTTOM_RIGHT:
                xposArmor=mainWindow.getScaledWidth()-5-armorSize.width;
                xposTools=mainWindow.getScaledWidth()-5-armorSize.width-toolsSize.width;
                xposTrinkets=xposTools-trinketsSize.width;
                ypos=mainWindow.getScaledHeight()-5-totalHeight;
                break;
            default:
                return;
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        if (ConfigurationHandler.getArmorAroundHotbar()) {
            int leftOffset = -120;
            int rightOffset = 100;
            if (!player.getEquippedStack(EquipmentSlot.OFFHAND).isEmpty()) {
                if (minecraft.options.getMainArm().getValue() == Arm.RIGHT) {
                    leftOffset -= 20;
                } else {
                    rightOffset += 20;
                }
            }
            int helmetTextWidth = fontRenderer.getWidth(helmet.getDisplayValue());
            int chestTextWidth = fontRenderer.getWidth(chestplate.getDisplayValue());
            this.renderItems(context, mainWindow.getScaledWidth()/2+leftOffset - helmetTextWidth, mainWindow.getScaledHeight()-iconHeight*2-2, true, RenderPos.left, helmetTextWidth+iconWidth+spacing, helmet);
            this.renderItems(context, mainWindow.getScaledWidth()/2+leftOffset - chestTextWidth, mainWindow.getScaledHeight()-iconHeight-2, true, RenderPos.left, chestTextWidth+iconWidth+spacing, chestplate);
            if (colytra != null) {
                int colytraTextWidth = fontRenderer.getWidth(colytra.getDisplayValue());
                this.renderItems(context, mainWindow.getScaledWidth()/2+leftOffset - chestTextWidth - colytraTextWidth - iconWidth, mainWindow.getScaledHeight()-iconHeight-2, true, RenderPos.left, colytraTextWidth+iconWidth+spacing, colytra);
            }
            this.renderItems(context, mainWindow.getScaledWidth()/2+rightOffset, mainWindow.getScaledHeight()-iconHeight*2-2, true, RenderPos.right, armorSize.width, leggings);
            this.renderItems(context, mainWindow.getScaledWidth()/2+rightOffset, mainWindow.getScaledHeight()-iconHeight-2, true, RenderPos.right, armorSize.width, boots);
            if (ConfigurationHandler.getCorner().isRight()) {
                xposTools += armorSize.width;
            } else {
                xposTools -= armorSize.width;
            }
        } else {
            this.renderItems(context, xposArmor, ypos, true, ConfigurationHandler.getCorner().isLeft() ? RenderPos.left : RenderPos.right, armorSize.width, helmet, chestplate, colytra, leggings, boots);
        }
        this.renderItems(context, xposTools, ypos, true, ConfigurationHandler.getCorner().isRight() ? RenderPos.right : RenderPos.left, toolsSize.width, invSlots, mainHand, offHand, projectiles);
        this.renderItems(context, xposTrinkets, ypos, true, ConfigurationHandler.getCorner().isRight() ? RenderPos.right : RenderPos.left, trinketsSize.width, trinkets);
    }
    
    private ItemIndicator indicatorType(PlayerEntity player, EquipmentSlot slot) {
        ItemStack stack = player.getEquippedStack(slot);

        if (haveTRCore && EnergyStorageUtil.isEnergyStorage(stack))
            return new TREnergyIndicator(stack);
        else if (stack.contains(DataComponentTypes.DAMAGE) || stack.contains(DataComponentTypes.MAX_DAMAGE)) {
            return new ItemDamageIndicator(stack);
        }
        return new ItemCountIndicator(stack,getSimilarCount(stack));
    }
    
    private void renderItemBreakingOverlay(DrawContext context, ItemStack itemStack, long timeDelta) {
        Window mainWindow = MinecraftClient.getInstance().getWindow();
        float alpha = 1.0f-((float)timeDelta/1000.0f);
        float xWarn = mainWindow.getScaledWidth()/2;
        float yWarn = mainWindow.getScaledHeight()/2;
        float scale = 5.0f;
        
        context.fill(0, 0, mainWindow.getScaledWidth(), mainWindow.getScaledHeight(),
                0xff0000+ ((int)(alpha*128)<<24));

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.scale(scale, scale, scale);
        RenderSystem.applyModelViewMatrix();

        context.drawItem(itemStack, (int)((xWarn)/scale-8), (int)((yWarn)/scale-8));

        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    public void afterRenderStatusEffects(DrawContext context, float partialTicks) {
        if (ConfigurationHandler.showEffectDuration()) {
            // a lot of this is copied from net/minecraft/client/gui/GuiIngame.java
            Window mainWindow = MinecraftClient.getInstance().getWindow();
            Collection<StatusEffectInstance> collection = minecraft.player.getStatusEffects();
            int posGood=0, posBad=0;
            for (StatusEffectInstance potioneffect : Ordering.natural().reverse().sortedCopy(collection)) {
                if (potioneffect.shouldShowIcon()) {
                    StatusEffect potion = potioneffect.getEffectType().value();
                    int xpos = mainWindow.getScaledWidth();
                    int ypos;
                    if (potion.isBeneficial()) {     // isBeneficial
                        posGood+=25; xpos-=posGood; ypos=15;
                    } else {
                        posBad+=25;  xpos-=posBad;  ypos=41;
                    }
                    if(potioneffect.isInfinite())
                        context.drawTextWithShadow(fontRenderer,Text.translatable("effect.duration.infinite") , xpos+2, ypos, ItemIndicator.color_yellow);
                    else {
                        int duration = potioneffect.getDuration();
                        String show;
                        if (duration > 1200)
                            show = (duration / 1200) + "m";
                        else
                            show = (duration / 20) + "s";
                        context.drawTextWithShadow(fontRenderer, show, xpos + 2, ypos, ItemIndicator.color_yellow);
                    }
                }
            }
        }
    }
    
    private RenderSize renderItems(DrawContext context, int xpos, int ypos, boolean reallyDraw, RenderPos numberPos, int maxWidth, ItemIndicator... items) {
        RenderSize result=new RenderSize(0, 0);
        
        for (ItemIndicator item: items) {
            if (item != null && !item.isEmpty()) {
                    String displayString=item.getDisplayValue();
                    int width=fontRenderer.getWidth(displayString);
                    if (width>result.width)
                        result.width=width;
                    if (reallyDraw) {
                        int color=item.getDisplayColor();
                        context.drawItem(item.getItemStack(), numberPos == RenderPos.left ? xpos+maxWidth-iconWidth-spacing : xpos, ypos+result.height);
                        context.drawTextWithShadow(fontRenderer, displayString, numberPos != RenderPos.right ? xpos : xpos+iconWidth+spacing, ypos+result.height+fontRenderer.fontHeight/2 + (numberPos==RenderPos.over ? 10  : 0), color);
                    }
                result.height+=16;
            }
        }
        if (result.width!=0)
            result.width+=iconWidth+spacing*2;
        return result;
    }
    
    public int getTrinketSlotCount(LivingEntity player) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        return component.map(trinketComponent -> trinketComponent.getEquipped(prdct -> true).size()).orElse(0);
    }
    
    public List<ItemStack> getTrinkets(LivingEntity player) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent(player);
        return component.map(trinketComponent -> trinketComponent.getEquipped(prdct -> true).stream().map(Pair::getRight).toList()).orElse(null);
    }
}
