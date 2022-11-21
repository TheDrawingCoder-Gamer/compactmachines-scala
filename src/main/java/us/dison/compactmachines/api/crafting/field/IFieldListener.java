package us.dison.compactmachines.api.crafting.field;

import javax.annotation.Nullable;
import us.dison.compactmachines.api.crafting.IMiniaturizationRecipe;
public interface IFieldListener {
	default void onRecipeChanged(IMiniaturizationField field, @Nullable IMiniaturizationRecipe recipe) {}
	default void onFieldActivated(IMiniaturizationField field) {}
	default void onRecipeCompleted(IMiniaturizationField field, IMiniaturizationRecipe recipe) {}
	default void onRecipeMatched(IMiniaturizationField field, IMiniaturizationRecipe recipe) {}
	default void onRecipeCleared(IMiniaturizationField field) {}

}
