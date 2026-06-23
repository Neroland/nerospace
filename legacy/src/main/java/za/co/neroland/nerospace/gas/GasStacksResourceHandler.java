package za.co.neroland.nerospace.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.neoforge.transfer.ResourceStacksResourceHandler;
import net.neoforged.neoforge.transfer.resource.ResourceStack;

/**
 * A slot-based gas store (the gas analogue of {@code FluidStacksResourceHandler}): fixed per-slot
 * capacity in millibuckets, transactional insert/extract via the inherited transfer surface, ValueIO
 * persistence via the inherited {@code serialize}/{@code deserialize}.
 */
public class GasStacksResourceHandler extends ResourceStacksResourceHandler<GasResource> {

    private static final Codec<ResourceStack<GasResource>> STACK_CODEC = RecordCodecBuilder.create(i -> i.group(
            GasResource.CODEC.fieldOf("gas").forGetter(ResourceStack::resource),
            Codec.INT.fieldOf("amount").forGetter(ResourceStack::amount))
            .apply(i, (gas, amount) -> new ResourceStack<>(gas, amount)));

    protected final int capacity;

    public GasStacksResourceHandler(int slots, int capacity) {
        super(slots, GasResource.EMPTY, STACK_CODEC);
        this.capacity = capacity;
    }

    @Override
    protected int getCapacity(int index, GasResource resource) {
        return this.capacity;
    }
}
