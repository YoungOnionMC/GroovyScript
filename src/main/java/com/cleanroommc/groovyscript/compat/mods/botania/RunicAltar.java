package com.cleanroommc.groovyscript.compat.mods.botania;

import com.cleanroommc.groovyscript.api.GroovyBlacklist;
import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import com.cleanroommc.groovyscript.helper.ingredient.OreDictIngredient;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.IFluidBlock;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipePetals;
import vazkii.botania.api.recipe.RecipeRuneAltar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RunicAltar extends VirtualizedRegistry<RecipeRuneAltar> {

    public RunicAltar() {
        super("RunicAltar", "runicaltar");
    }

    public RecipeBuilder recipeBuilder() { return new RecipeBuilder(); }

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(recipe -> BotaniaAPI.runeAltarRecipes.remove(recipe));
        restoreFromBackup().forEach(recipe -> {
            if(!BotaniaAPI.runeAltarRecipes.contains(recipe))
                BotaniaAPI.registerRuneAltarRecipe(recipe.getOutput(), recipe.getManaUsage(), recipe.getInputs());
        });
    }

    public void add(RecipeRuneAltar recipe) {
        if(recipe != null) {
            addScripted(recipe);
            BotaniaAPI.runeAltarRecipes.add(recipe);
        }
    }

    public RecipeRuneAltar add(ItemStack result, int mana, Object... inputs) {
        RecipeRuneAltar recipe = new RecipeRuneAltar(result, mana, inputs);
        add(recipe);
        return recipe;
    }

    public boolean remove(RecipeRuneAltar recipe) {
        Iterator recipeIter = BotaniaAPI.runeAltarRecipes.iterator();

        Object r;
        do {
            if(!recipeIter.hasNext())
                return false;
            r = recipeIter.next();
        } while(!(r instanceof RecipeRuneAltar) || !((RecipeRuneAltar)r).getOutput().isItemEqual(recipe.getOutput()));

        recipeIter.remove();

        addBackup(recipe);
        return true;
    }

    public void removeByOutput(ItemStack output) {
        if(IngredientHelper.isEmpty(output)) {
            GroovyLog.msg("Error removing Botania Runic Altar recipe")
                    .add("Output must not be empty.")
                    .error()
                    .post();
        }
        Object r;
        List<RecipeRuneAltar> recipes = new ArrayList<>();
        Iterator recipeIter = BotaniaAPI.runeAltarRecipes.iterator();
        while(recipeIter.hasNext()) {
            r = recipeIter.next();
            if((r instanceof RecipeRuneAltar) && ((RecipeRuneAltar)r).getOutput().isItemEqual(output))
                recipes.add((RecipeRuneAltar)r);
        }
        if(recipes.isEmpty()) {
            GroovyLog.msg("Error removing Botania Runic Altar recipe")
                    .add("No recipes found for %s", output)
                    .error()
                    .post();
            return;
        }

        recipes.forEach(recipe -> {
            this.addBackup(recipe);
            BotaniaAPI.runeAltarRecipes.remove(recipe);
        });
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<RecipeRuneAltar> {

        private int mana;

        public RecipeBuilder setMana(int mana) {
            this.mana = mana;
            return this;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding Runic Altar recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 16, 1, 1);
        }

        @Override
        public @Nullable RecipeRuneAltar register() {
            if(!validate()) return null;
            List<Object> inputs = new ArrayList<>();

            for(Object item : input) {
                if (item instanceof ItemStack)
                    inputs.add(item);
                else if(item instanceof OreDictIngredient)
                    inputs.add(((OreDictIngredient)item).getOreDict());
            }

            RecipeRuneAltar recipe = new RecipeRuneAltar(output.get(0), mana, inputs.toArray());
            ModSupport.BOTANIA.get().runicAltar.add(recipe);
            return recipe;
        }
    }
}
