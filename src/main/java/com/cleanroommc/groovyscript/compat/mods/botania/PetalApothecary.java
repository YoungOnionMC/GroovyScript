package com.cleanroommc.groovyscript.compat.mods.botania;

import com.cleanroommc.groovyscript.api.GroovyBlacklist;
import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipePetals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PetalApothecary extends VirtualizedRegistry<RecipePetals> {

    public PetalApothecary() {
        super("PetalApothecary", "petalapothecary");
    }

    public RecipeBuilder recipeBuilder() { return new RecipeBuilder(); }

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(recipe -> BotaniaAPI.petalRecipes.remove(recipe));
        restoreFromBackup().forEach(recipe -> {
            if(!BotaniaAPI.petalRecipes.contains(recipe))
                BotaniaAPI.registerPetalRecipe(recipe.getOutput(), recipe.getInputs());
        });
    }

    public void add(RecipePetals recipe) {
        if(recipe != null) {
            addScripted(recipe);
            BotaniaAPI.petalRecipes.add(recipe);
        }
    }

    public RecipePetals add(ItemStack result, Object... inputs) {
        RecipePetals recipe = new RecipePetals(result, inputs);
        add(recipe);
        return recipe;
    }

    public boolean remove(RecipePetals recipe) {
        Iterator recipeIter = BotaniaAPI.petalRecipes.iterator();

        Object r;
        do {
            if(!recipeIter.hasNext())
                return false;
            r = recipeIter.next();
        } while(!(r instanceof RecipePetals) || !((RecipePetals)r).getOutput().isItemEqual(recipe.getOutput()));

        recipeIter.remove();

        addBackup(recipe);
        return true;
    }

    public void removeByOutput(ItemStack output) {
        if(IngredientHelper.isEmpty(output)) {
            GroovyLog.msg("Error removing Botania Petal Apothecary Recipe")
                    .add("Output must not be empty.")
                    .error()
                    .post();
        }
        Object r;
        List<RecipePetals> recipes = new ArrayList<>();
        Iterator recipeIter = BotaniaAPI.petalRecipes.iterator();
        while(recipeIter.hasNext()) {
            r = recipeIter.next();
            if((r instanceof RecipePetals) && ((RecipePetals)r).getOutput().isItemEqual(output))
                recipes.add((RecipePetals)r);
        }
        if(recipes.isEmpty()) {
            GroovyLog.msg("Error removing Botania Petal Apothecary Recipe")
                    .add("No recipes found for %s", output)
                    .error()
                    .post();
            return;
        }

        recipes.forEach(recipe -> {
            this.addBackup(recipe);
            BotaniaAPI.petalRecipes.remove(recipe);
        });
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<RecipePetals> {

        @Override
        public String getErrorMsg() {
            return "Error adding Petal recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 16, 1, 1);
        }

        @Override
        public @Nullable RecipePetals register() {
            if(!validate()) return null;
            RecipePetals recipe = new RecipePetals(output.get(0), input.toArray());
            ModSupport.BOTANIA.get().petalApothecary.add(recipe);
            return recipe;
        }
    }


}
