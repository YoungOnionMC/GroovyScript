package com.cleanroommc.groovyscript.core.mixin.botania;

import com.cleanroommc.groovyscript.compat.mods.botania.TerraPlate;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.Cancellable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import vazkii.botania.api.internal.VanillaPacketDispatcher;
import vazkii.botania.api.mana.IManaPool;
import vazkii.botania.api.mana.spark.ISparkEntity;
import vazkii.botania.api.mana.spark.SparkHelper;
import vazkii.botania.common.block.tile.TileTerraPlate;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.network.PacketBotaniaEffect;
import vazkii.botania.common.network.PacketHandler;

import java.util.List;

import static vazkii.botania.common.block.tile.TileTerraPlate.MAX_MANA;

@Mixin(value = TileTerraPlate.class, remap = false)
public abstract class TileTerraPlateMixin {

    @Shadow abstract boolean hasValidPlatform();

    @Shadow public abstract ISparkEntity getAttachedSpark();

    @Shadow public abstract void recieveMana(int mana);

    @Shadow abstract List<EntityItem> getItems();

    @Shadow private int mana;

    private TileTerraPlate getThis() {
        return (TileTerraPlate) (Object) this;
    }

    private TerraPlate.RecipeTerraPlate findValidRecipe(List<EntityItem> items) {
        for(TerraPlate.RecipeTerraPlate recipe : TerraPlate.terraPlateRecipes) {
            if(recipe.matches(items)) {
                return recipe;
            }
        }
        return null;
    }

    @Inject(method="areIncomingTranfersDone()Z", at=@At("HEAD"), cancellable = true)
    public void areIncomingTransfersDone(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(findValidRecipe(this.getItems()) == null);
        ci.cancel();
    }

    @Inject(method="canRecieveManaFromBursts()Z", at=@At("HEAD"), cancellable = true)
    public void canRecieveManaFromBursts(CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(findValidRecipe(this.getItems()) != null);
        ci.cancel();
    }

    @Inject(method = "update()V", at = @At("HEAD"), cancellable = true)
    private void updateInject(CallbackInfo ci) {
        if(getThis().getWorld().isRemote)
            return;

        boolean removeMana = true;

        if(this.hasValidPlatform()) {
            List<EntityItem> items = this.getItems();
            TerraPlate.RecipeTerraPlate recipe = findValidRecipe(items);
            if(recipe != null) {
                removeMana = false;
                ISparkEntity spark = this.getAttachedSpark();
                if(spark != null) {
                    List<ISparkEntity> sparkEntities = SparkHelper.getSparksAround(getThis().getWorld(), getThis().getPos().getX() + 0.5, getThis().getPos().getY() + 0.5, getThis().getPos().getZ() + 0.5);
                    for(ISparkEntity otherSpark : sparkEntities) {
                        if(spark == otherSpark)
                            continue;

                        if(otherSpark.getAttachedTile() != null && otherSpark.getAttachedTile() instanceof IManaPool)
                            otherSpark.registerTransfer(spark);
                    }
                }
                if(this.mana > 0) {
                    VanillaPacketDispatcher.dispatchTEToNearbyPlayers(getThis().getWorld(), getThis().getPos());
                    PacketHandler.sendToNearby(getThis().getWorld(), getThis().getPos(),
                            new PacketBotaniaEffect(PacketBotaniaEffect.EffectType.TERRA_PLATE, getThis().getPos().getX(), getThis().getPos().getY(), getThis().getPos().getZ()));
                }

                if(this.mana >= MAX_MANA) {
                    EntityItem item = items.get(0);
                    for(EntityItem otherItem : items)
                        if(otherItem != item)
                            otherItem.setDead();
                        else item.setItem(recipe.getOutput());
                    getThis().getWorld().playSound(null, item.posX, item.posY, item.posZ, ModSounds.terrasteelCraft, SoundCategory.BLOCKS, 1, 1);
                    this.mana = 0;
                    getThis().getWorld().updateComparatorOutputLevel(getThis().getPos(), getThis().getWorld().getBlockState(getThis().getPos()).getBlock());
                    VanillaPacketDispatcher.dispatchTEToNearbyPlayers(getThis().getWorld(), getThis().getPos());
                }
            }
        }

        if(removeMana)
            this.recieveMana(-1000);

        ci.cancel();
    }
}
