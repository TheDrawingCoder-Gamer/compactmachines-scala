package us.dison.compactmachines.api.crafting.catalyst;

import net.minecraft.item.ItemStack;
import java.util.Set;

public interface ICatalystMatcher {
	boolean matches(ItemStack stack);
	Set<ItemStack> getPossible();
	CatalystType<?> getType();
}
