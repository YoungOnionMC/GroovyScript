package com.cleanroommc.groovyscript.compat.mods.botania;


import com.cleanroommc.groovyscript.api.GroovyBlacklist;
import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.IFluidBlock;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipePetals;
import vazkii.botania.api.recipe.RecipePureDaisy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class PureDaisy extends VirtualizedRegistry<RecipePureDaisy> {

    public PureDaisy() {
        super("PureDaisy", "puredaisy");
    }

    public RecipeBuilder recipeBuilder() { return new RecipeBuilder(); }

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(recipe -> BotaniaAPI.pureDaisyRecipes.remove(recipe));
        restoreFromBackup().forEach(recipe -> {
            if(!BotaniaAPI.pureDaisyRecipes.contains(recipe))
                BotaniaAPI.registerPureDaisyRecipe(recipe.getInput(), recipe.getOutputState(), recipe.getTime());
        });
    }

    public void add(RecipePureDaisy recipe) {
        if(recipe != null) {
            addScripted(recipe);
            BotaniaAPI.pureDaisyRecipes.add(recipe);
        }
    }

    public RecipePureDaisy add(ItemStack result, IBlockState input) {
        return add(result, input, 0);
    }

    public RecipePureDaisy add(ItemStack result, IBlockState input, int time) {
        RecipePureDaisy recipe = new RecipePureDaisy(result, input, time);
        add(recipe);
        return recipe;
    }

    public boolean remove(RecipePureDaisy recipe) {
        Iterator recipeIter = BotaniaAPI.pureDaisyRecipes.iterator();

        Object r;
        do {
            if(!recipeIter.hasNext())
                return false;
            r = recipeIter.next();
        } while(!(r instanceof RecipePureDaisy) || !((RecipePureDaisy)r).getOutputState().equals(recipe.getOutputState()));

        recipeIter.remove();

        addBackup(recipe);
        return true;
    }

    public void removeByOutput(IBlockState outputState) {
        if(outputState == null) {
            GroovyLog.msg("Error removing Botania Pure Daisy Recipe")
                    .add("Output must be a valid block state.")
                    .error()
                    .post();
        }
        Object r;
        List<RecipePureDaisy> recipes = new ArrayList<>();
        Iterator recipeIter = BotaniaAPI.pureDaisyRecipes.iterator();
        while(recipeIter.hasNext()) {
            r = recipeIter.next();
            if((r instanceof RecipePureDaisy) && ((RecipePureDaisy)r).getOutputState().equals(outputState))
                recipes.add((RecipePureDaisy)r);
        }
        if(recipes.isEmpty()) {
            GroovyLog.msg("Error removing Botania Pure Daisy Recipe")
                    .add("No recipes found for %s", outputState)
                    .error()
                    .post();
            return;
        }

        recipes.forEach(recipe -> {
            this.addBackup(recipe);
            BotaniaAPI.pureDaisyRecipes.remove(recipe);
        });
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<RecipePureDaisy> {

        private IBlockState inputBlockState;
        private IFluidBlock inputFluidBlock;
        private ItemStack inputItem;
        private IBlockState outputState;
        private int time;

        @Override
        public String getErrorMsg() {
            return "Error adding Petal recipe";
        }

        public RecipeBuilder inputBlock(IBlockState inputBlock) {
            inputBlockState = inputBlock;
            return this;
        }

        public RecipeBuilder inputFluidBlock(IFluidBlock inputBlock) {
            inputFluidBlock = inputBlock;
            return this;
        }

        public RecipeBuilder inputItem(ItemStack item) {
            inputItem = item;
            return this;
        }

        public RecipeBuilder outputBlock(IBlockState outputBlock) {
            outputState = outputBlock;
            return this;
        }

        public RecipeBuilder time(int time) {
            this.time = time;
            return this;
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 1, 1, 1);
        }

        @Override
        public @Nullable RecipePureDaisy register() {
            if(!validate()) return null;
            RecipePureDaisy recipe = null;
            if(inputBlockState != null)
                recipe = new RecipePureDaisy(inputBlockState, outputState, time);
            else if(inputFluidBlock != null)
                recipe = new RecipePureDaisy(inputFluidBlock, outputState, time);
            else if(inputItem != null)
                recipe = new RecipePureDaisy(inputItem, outputState, time);
            ModSupport.BOTANIA.get().pureDaisy.add(recipe);
            return recipe;
        }
    }
}
