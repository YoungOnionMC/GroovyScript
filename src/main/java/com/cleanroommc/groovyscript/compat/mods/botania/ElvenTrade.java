package com.cleanroommc.groovyscript.compat.mods.botania;

import com.cleanroommc.groovyscript.api.GroovyBlacklist;
import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipeElvenTrade;
import vazkii.botania.api.recipe.RecipeManaInfusion;
import vazkii.botania.api.recipe.RecipePetals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ElvenTrade extends VirtualizedRegistry<RecipeElvenTrade> {
    public ElvenTrade() { super("ElvenTrade", "elventrade"); }

    public ElvenTrade.RecipeBuilder recipeBuilder() { return new ElvenTrade.RecipeBuilder(); }

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(recipe -> BotaniaAPI.elvenTradeRecipes.remove(recipe));
        restoreFromBackup().forEach(recipe -> {
            if(!BotaniaAPI.elvenTradeRecipes.contains(recipe)) {
                BotaniaAPI.registerElvenTradeRecipe(recipe.getOutputs().toArray(new ItemStack[0]), recipe.getInputs());
            }
        });
    }

    public void add(RecipeElvenTrade recipe) {

        if(recipe != null) {
            addScripted(recipe);
            BotaniaAPI.elvenTradeRecipes.add(recipe);
        }
    }

    public RecipeElvenTrade add(ItemStack result, Object... inputs) {
        return add(new ItemStack[]{result}, inputs);
    }

    public RecipeElvenTrade add(ItemStack[] result, Object... inputs) {
        RecipeElvenTrade recipe = new RecipeElvenTrade(result, inputs);
        add(recipe);
        return recipe;
    }

    public void removeByOutput(ItemStack... output) {
        if(IngredientHelper.isEmpty(output)) {
            GroovyLog.msg("Error removing Botania Elven Trade recipe")
                    .add("Output must not be empty.")
                    .error()
                    .post();
        }
        Object r;
        List<RecipeElvenTrade> recipes = new ArrayList<>();
        Iterator recipeIter = BotaniaAPI.elvenTradeRecipes.iterator();
        while(recipeIter.hasNext()) {
            boolean recipeFound = false;

            r = recipeIter.next();

            if(r instanceof RecipeElvenTrade) {
                if(((RecipeElvenTrade)r).getOutputs().size() != output.length)
                    break;
                for(int i = 0; i < output.length; i++) {
                    if (((RecipeElvenTrade) r).getOutputs().get(i).isItemEqual(output[i]))
                        recipeFound = true;
                }
            }
            if(recipeFound)
                recipes.add((RecipeElvenTrade)r);
        }

        if(recipes.isEmpty()) {
            GroovyLog.msg("Error removing Botania Elven Trade recipe")
                    .add("No recipes found for {}", output)
                    .error()
                    .post();
            return;
        }

        recipes.forEach(recipe -> {
            this.addBackup(recipe);
            BotaniaAPI.elvenTradeRecipes.remove(recipe);
        });
    }



    public static class RecipeBuilder extends AbstractRecipeBuilder<RecipeElvenTrade> {

        @Override
        public String getErrorMsg() {
            return "Error adding elven trade recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        }

        @Override
        public @Nullable RecipeElvenTrade register() {
            if(!validate()) return null;
            RecipeElvenTrade recipe = new RecipeElvenTrade(output.toArray(new ItemStack[0]), input.toArray());
            ModSupport.BOTANIA.get().elvenTrade.add(recipe);
            return recipe;
        }
    }

}
