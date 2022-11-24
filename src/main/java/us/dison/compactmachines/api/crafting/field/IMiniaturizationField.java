package us.dison.compactmachines.api.crafting.field;

import java.util.Optional;
import java.util.stream.Stream;
import us.dison.compactmachines.api.crafting.EnumCraftingState; 
import us.dison.compactmachines.api.crafting.IMiniaturizationRecipe;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.Structure;
// import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.Box;

public interface IMiniaturizationField {
	default void dispose() {}
	Box getBounds();
	FieldSize getFieldSize();
	BlockPos getCenter();
	void setCenter(BlockPos center);
	void setFieldSize(FieldSize size);

	int getProgress();

	void setProgress(int progress);

	default Stream<BlockPos> getProjectorPositions() {
		return Stream.empty();
	}
	Optional<? extends IMiniaturizationRecipe> getCurrentRecipe();
	
	void setRecipe(Identifier id); 
		
	Identifier getRecipeId(); 
	Structure getMatchedBlocks(); 
	void clearRecipe();
	EnumCraftingState getCraftingState();
	void setCraftingState(EnumCraftingState state);

	default void tick() {}

	boolean isLoaded();

	default void checkLoaded() {}

	void fieldContentsChanged();

	void setWorld(World world);

	void registerListener(IFieldListener listener); 
	
	NbtCompound serverData();
	default NbtCompound clientData() {
		NbtCompound data = new NbtCompound();
		data.putLong("center", getCenter().asLong());
		data.putString("size", getFieldSize().name());
		data.putString("state", getCraftingState().name());

		Optional<? extends IMiniaturizationRecipe> currentRecipe = getCurrentRecipe();
		currentRecipe.ifPresent( r -> {
			NbtCompound recipe = new NbtCompound();
			recipe.putString("id", r.getRecipeId().toString());
			recipe.putInt("progress", getProgress());

			data.put("recipe", recipe);
		});
		return data; 
	}

	default void loadClientData(NbtCompound nbt) {
		this.setCenter(BlockPos.fromLong(nbt.getLong("center")));
		this.setFieldSize(FieldSize.valueOf(nbt.getString("size")));
		this.setCraftingState(EnumCraftingState.valueOf(nbt.getString("state")));

		if (nbt.contains("recipe")) {
			NbtCompound recipe = nbt.getCompound("recipe");
			this.setRecipe(new Identifier(recipe.getString("id")));
			this.setProgress(recipe.getInt("progress"));
		}
	}

	default void handleDestabilize() {}

	void enable();
	void disable();
	void checkRedstone();

	boolean enabled();

	void cleanForDisposal();
}
