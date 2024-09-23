/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.durabilityviewer.itemindicator;

import de.guntram.mcmod.durabilityviewer.handler.ConfigurationHandler;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

/**
 *
 * @author gbl
 */
public class TREnergyIndicator implements ItemIndicator {

    private final ItemStack stack;
    private final double maxEnergy;
    public TREnergyIndicator(ItemStack stack) {
        this.stack = stack;
        maxEnergy = getMaxEnergy();
    }

    @Override
    public String getDisplayValue() {
        long energy = getEnergy();

        if (ConfigurationHandler.getShowPercentValues() && maxEnergy > 0) {
            return String.format("§o%.1f%%", energy / maxEnergy * 100);
        }

        if (energy > 10_000_000) {
            return "§o"+((int)(energy/1000))+"M";
        } else if (energy > 10_000) {
            return "§o"+((int)(energy/1000))+"k";
        } else {
            return "§o"+(int)energy;
        }
    }

    @Override
    public int getDisplayColor() {
        long energy = getEnergy();

        if (energy > maxEnergy * 0.2) {
            return color_green;
        } else if (energy > maxEnergy * 0.1) {
            return color_yellow;
        } else {
            return color_red;
        }
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public ItemStack getItemStack() {
        return stack;
    }

    private long getEnergy(){
        EnergyStorage storage = ContainerItemContext.withConstant(stack).find(EnergyStorage.ITEM);
        return storage != null ? storage.getAmount() : 0;
    }

    private long getMaxEnergy(){
        EnergyStorage storage = ContainerItemContext.withConstant(stack).find(EnergyStorage.ITEM);
        return storage != null ? storage.getCapacity() : 0;
    }
}
