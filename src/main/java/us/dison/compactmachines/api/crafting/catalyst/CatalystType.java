package us.dison.compactmachines.api.crafting.catalyst;

import com.mojang.serialization.Codec; 

public interface CatalystType<M extends ICatalystMatcher> {
	Codec<M> getCodec();
}
