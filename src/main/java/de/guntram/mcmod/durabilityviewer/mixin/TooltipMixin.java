package de.guntram.mcmod.durabilityviewer.mixin;

import de.guntram.mcmod.durabilityviewer.handler.ConfigurationHandler;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import org.apache.commons.compress.compressors.lzw.LZWInputStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class TooltipMixin {
    
    @Shadow public abstract boolean isEmpty();
    @Shadow public abstract boolean isDamaged();
    @Shadow public abstract int getMaxDamage();
    @Shadow public abstract int getDamage();
    @Shadow public abstract ComponentMap getComponents();
    
//    @Inject(method="getTooltip(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/client/util/ITooltipFlag;)Ljava/util/List",
    /*@Inject(method="getTooltip",
            at=@At("RETURN"), cancellable = true)
    private void getTooltipdone(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        List<Text> list = cir.getReturnValue();
        if (!type.isAdvanced() && !this.isEmpty()) {
            if (this.isDamaged()) {
                Text toolTip = Text.literal(I18n.translate("tooltip.durability",
                        (this.getMaxDamage()- this.getDamage())+
                        " / "+
                        this.getMaxDamage()))
                .formatted(ConfigurationHandler.getTooltipColor());
                if (!list.contains(toolTip)) {
                    list.add(toolTip);
                }
            }
        }

        if (Screen.hasAltDown()) {
            ComponentMap cMap = this.getComponents();
            if (cMap != null) {
                addNbtCompound("", list, cMap);
            }
        }
        cir.setReturnValue(list);
    }*/
    @Unique
    private void addNbtCompound(String prefix, List<Text>list, ComponentMap cMap) {
        for(Component<?> t : cMap){
            if(t.value() instanceof Integer) list.add(Text.literal(prefix + Registries.DATA_COMPONENT_TYPE.getEntry(t.type())+": §3"+t.value()));
            else if(t.value() instanceof Double) list.add(Text.literal(prefix + Registries.DATA_COMPONENT_TYPE.getEntry(t.type())+": §6"+t.value()));
            else if(t.value() instanceof Text txt) list.add(Text.literal(prefix + Registries.DATA_COMPONENT_TYPE.getEntry(t.type())+": §8"+txt.getLiteralString()));
            else if(t.value() instanceof List<?> ls) list.add(Text.literal(prefix + Registries.DATA_COMPONENT_TYPE.getEntry(t.type())+": §9"+ls.size()));


        }
        /*


        TreeSet<String> sortedKeys = new TreeSet(cMap);
        for (String key: sortedKeys) {
            NbtElement elem=tag.get(key);
            switch(elem.getType()) {
                case 2: list.add(Text.literal(prefix+key+": §2"+tag.getShort(key))); break;
                case 3: list.add(Text.literal(prefix+key+": §3"+tag.getInt(key))); break;
                case 6: list.add(Text.literal(prefix+key+": §6"+tag.getDouble(key))); break;
                case 8: list.add(Text.literal(prefix+key+": §8"+tag.getString(key))); break;
                case 9: list.add(Text.literal(prefix+key+": §9List, "+((NbtList)elem).size()+" items")); break;
                case 10:list.add(Text.literal(prefix+key+": §aCompound"));
                        if (Screen.hasShiftDown()) {
                            //Recursively render list
                            addNbtCompound(prefix+"    ", list, (NbtCompound)elem);
                        }
                        break;
                default:
                    list.add(Text.literal(prefix+key+": Type "+elem.getType())); break;
            }
        }*/
    }
}
