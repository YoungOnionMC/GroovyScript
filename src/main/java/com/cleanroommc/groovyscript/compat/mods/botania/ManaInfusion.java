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
import vazkii.botania.api.recipe.RecipeManaInfusion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ManaInfusion extends VirtualizedRegistry<RecipeManaInfusion> {

    public ManaInfusion() { super("ManaInfusion", "manainfusion"); }

    public RecipeBuilder recipeBuilder() { return new RecipeBuilder(); }

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(recipe -> BotaniaAPI.manaInfusionRecipes.remove(recipe));
        restoreFromBackup().forEach(recipe -> {
            if(!BotaniaAPI.manaInfusionRecipes.contains(recipe)) {
                if(recipe.isAlchemy())
                    BotaniaAPI.registerManaAlchemyRecipe(recipe.getOutput(), recipe.getInput(), recipe.getManaToConsume());
                else if(recipe.isConjuration())
                    BotaniaAPI.registerManaConjurationRecipe(recipe.getOutput(), recipe.getInput(), recipe.getManaToConsume());
                else
                    BotaniaAPI.registerManaInfusionRecipe(recipe.getOutput(), recipe.getInput(), recipe.getManaToConsume());
            }
        });
    }

    public void add(RecipeManaInfusion recipe, boolean alchemy, boolean conjuration) {
        if(recipe != null) {
            addScripted(recipe);
            if(alchemy) {
                recipe.setCatalyst(RecipeManaInfusion.alchemyState);
                BotaniaAPI.manaInfusionRecipes.add(recipe);
            } else if(conjuration) {
                recipe.setCatalyst(RecipeManaInfusion.conjurationState);
                BotaniaAPI.manaInfusionRecipes.add(recipe);
            } else
                BotaniaAPI.manaInfusionRecipes.add(recipe);
        }
    }

    public boolean remove(RecipeManaInfusion recipe) {
        Iterator recipeIter = BotaniaAPI.manaInfusionRecipes.iterator();

        Object r;
        do {
            if(!recipeIter.hasNext())
                return false;
            r = recipeIter.next();
        } while(!(r instanceof RecipeManaInfusion) || !((RecipeManaInfusion)r).getOutput().isItemEqual(recipe.getOutput()));

        recipeIter.remove();

        addBackup(recipe);
        return true;
    }

    public void removeByOutput(ItemStack output) {
        if(IngredientHelper.isEmpty(output)) {
            GroovyLog.msg("Error removing Botania Mana Infusion recipe")
                    .add("Output must not be empty.")
                    .error()
                    .post();
        }
        Object r;
        List<RecipeManaInfusion> recipes = new ArrayList<>();
        Iterator recipeIter = BotaniaAPI.manaInfusionRecipes.iterator();
        while(recipeIter.hasNext()) {
            r = recipeIter.next();
            if((r instanceof RecipeManaInfusion) && ((RecipeManaInfusion)r).getOutput().isItemEqual(output))
                recipes.add((RecipeManaInfusion)r);
        }
        if(recipes.isEmpty()) {
            GroovyLog.msg("Error removing Botania Mana Infusion recipe")
                    .add("No recipes found for %s", output)
                    .error()
                    .post();
            return;
        }

        recipes.forEach(recipe -> {
            this.addBackup(recipe);
            BotaniaAPI.manaInfusionRecipes.remove(recipe);
        });
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<RecipeManaInfusion> {
        private int mana;
        private boolean conjuration = false;
        private boolean alchemy = false;


        public RecipeBuilder setMana(int mana) {
            this.mana = mana;
            return this;
        }

        public RecipeBuilder setConjuration() {
            if(this.alchemy) {
                GroovyLog.msg("Mana recipe already using alchemy")
                        .warn()
                        .post();
                return this;
            }
            this.conjuration = true;
            return this;
        }

        public RecipeBuilder setAlchemy() {
            if(this.conjuration) {
                GroovyLog.msg("Mana recipe already using conjuration")
                        .warn()
                        .post();
                return this;
            }
            this.alchemy = true;
            return this;
        }

        @Override
        public String getErrorMsg() {
            return "Error adding mana infusion recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, 1, 1, 1);
        }

        @Override
        public @Nullable RecipeManaInfusion register() {
            if(!validate()) return null;
            RecipeManaInfusion recipe = new RecipeManaInfusion(output.get(0), input.get(0), mana);
            ModSupport.BOTANIA.get().manaInfusion.add(recipe, alchemy, conjuration);
            return recipe;
        }
    }
}
