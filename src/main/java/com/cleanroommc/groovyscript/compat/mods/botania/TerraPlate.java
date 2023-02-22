package com.cleanroommc.groovyscript.compat.mods.botania;

import com.cleanroommc.groovyscript.api.GroovyBlacklist;
import com.cleanroommc.groovyscript.api.GroovyLog;
import com.cleanroommc.groovyscript.compat.mods.ModSupport;
import com.cleanroommc.groovyscript.helper.ingredient.IngredientHelper;
import com.cleanroommc.groovyscript.helper.recipe.AbstractRecipeBuilder;
import com.cleanroommc.groovyscript.registry.VirtualizedRegistry;
import com.google.common.collect.ImmutableList;
import net.minecraft.command.CommandKill;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.recipe.RecipeElvenTrade;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.item.ModItems;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



public class TerraPlate extends VirtualizedRegistry<TerraPlate.RecipeTerraPlate> {

    public static List<RecipeTerraPlate> terraPlateRecipes = new ArrayList<>();

    {
        Item manaResource = ModItems.manaResource;
        manaResource.setDamage(new ItemStack(manaResource), 0);
        ItemStack ingot = new ItemStack(manaResource, 1, 0);
        ItemStack pearl = new ItemStack(manaResource, 1, 1);
        ItemStack diamond = new ItemStack(manaResource, 1, 2);
        terraPlateRecipes.add(new RecipeTerraPlate(new ItemStack(manaResource, 1, 4), ingot, pearl, diamond));
    }

    public TerraPlate() { super("TerraPlate", "terraplate"); }

    public RecipeBuilder recipeBuilder() { return new RecipeBuilder(); }

    @Override
    @GroovyBlacklist
    @ApiStatus.Internal
    public void onReload() {
        removeScripted().forEach(recipe -> terraPlateRecipes.remove(recipe));
        restoreFromBackup().forEach(recipe -> {
            if(terraPlateRecipes.contains(recipe)) {
                RecipeTerraPlate r = new RecipeTerraPlate(recipe.getOutput(), recipe.getInputs());
                terraPlateRecipes.add(r);
            }
        });
    }

    public void add(RecipeTerraPlate recipe) {
        if(recipe != null) {
            addScripted(recipe);
            terraPlateRecipes.add(recipe);
        }
    }

    public RecipeTerraPlate add(ItemStack result, Object... inputs) {
        RecipeTerraPlate r = new RecipeTerraPlate(result, inputs);
        add(r);
        return r;
    }

    public void removeByOutput(ItemStack output) {
        if(IngredientHelper.isEmpty(output)) {
            GroovyLog.msg("Error removing Botania Elven Trade recipe")
                    .add("Output must not be empty.")
                    .error()
                    .post();
        }
        Object r;
        List<RecipeTerraPlate> recipes = new ArrayList<>();
        Iterator recipeIter = terraPlateRecipes.iterator();
        while(recipeIter.hasNext()) {
            boolean recipeFound = false;

            r = recipeIter.next();

            if(r instanceof RecipeTerraPlate && ((RecipeTerraPlate) r).getOutput().isItemEqual(output))
                        recipeFound = true;

            if(recipeFound)
                recipes.add((RecipeTerraPlate)r);
        }

        if(recipes.isEmpty()) {
            GroovyLog.msg("Error removing Botania Terra Plate recipe")
                    .add("No recipes found for {}", output)
                    .error()
                    .post();
            return;
        }

        recipes.forEach(recipe -> {
            this.addBackup(recipe);
            terraPlateRecipes.remove(recipe);
        });
    }

    public static class RecipeTerraPlate {
        private final ItemStack output;
        private final List<Object> inputs;

        public RecipeTerraPlate(ItemStack output, Object... inputs) {
            this.output = output;
            ImmutableList.Builder<Object> inputsToSet = ImmutableList.builder();
            for(Object obj : inputs) {
                if(obj instanceof String || obj instanceof ItemStack)
                    inputsToSet.add(obj);
                else throw new IllegalArgumentException("Invalid input");
            }

            this.inputs = inputsToSet.build();
        }

        public boolean matches(List<EntityItem> items) {
            List<Object> inputsMissing = new ArrayList<>(inputs);

            for(int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i).getItem();
                if(stack.isEmpty())
                    break;

                int stackIndex = -1, oredictIndex = -1;

                for(int j = 0; j < inputsMissing.size(); j++) {
                    Object input = inputsMissing.get(j);
                    if(input instanceof String) {
                        boolean found = false;
                        for(ItemStack ostack : OreDictionary.getOres((String) input, false)) {
                            if(OreDictionary.itemMatches(ostack, stack, false)) {
                                oredictIndex = j;
                                found = true;
                                break;
                            }
                        }


                        if(found)
                            break;
                    } else if(input instanceof ItemStack && compareStacks((ItemStack) input, stack)) {
                        stackIndex = j;
                        break;
                    }
                }

                if(stackIndex != -1)
                    inputsMissing.remove(stackIndex);
                else if(oredictIndex != -1)
                    inputsMissing.remove(oredictIndex);
                else return false;
            }

            return inputsMissing.isEmpty();
        }

        private boolean compareStacks(ItemStack recipe, ItemStack supplied) {
            return recipe.getItem() == supplied.getItem() && recipe.getItemDamage() == supplied.getItemDamage() && ItemNBTHelper.matchTag(recipe.getTagCompound(), supplied.getTagCompound());
        }

        public List<Object> getInputs() {
            return inputs;
        }

        public ItemStack getOutput() {
            return output;
        }
    }

    public static class RecipeBuilder extends AbstractRecipeBuilder<RecipeTerraPlate> {
        @Override
        public String getErrorMsg() {
            return "Error adding elven trade recipe";
        }

        @Override
        public void validate(GroovyLog.Msg msg) {
            validateItems(msg, 1, Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        }

        @Override
        public @Nullable RecipeTerraPlate register() {
            if(!validate()) return null;
            RecipeTerraPlate recipe = new RecipeTerraPlate(output.get(0), input.toArray());
            ModSupport.BOTANIA.get().terraPlate.add(recipe);
            return recipe;
        }
    }
}
