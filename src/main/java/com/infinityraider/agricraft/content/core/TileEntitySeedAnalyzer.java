package com.infinityraider.agricraft.content.core;

import com.infinityraider.agricraft.AgriCraft;
import com.infinityraider.agricraft.api.v1.items.IAgriJournalItem;
import com.infinityraider.agricraft.api.v1.items.IAgriSeedItem;
import com.infinityraider.agricraft.api.v1.plant.IAgriPlant;
import com.infinityraider.infinitylib.block.tile.TileEntityBase;
import com.infinityraider.infinitylib.utility.inventory.IInventoryItemHandler;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TileEntitySeedAnalyzer extends TileEntityBase implements ISidedInventory, IInventoryItemHandler {

    private final AutoSyncedField<ItemStack> seed;
    private final AutoSyncedField<ItemStack> journal;
    private final LazyOptional<TileEntitySeedAnalyzer> capability;

    private static final int[] SLOTS = new int[]{0,1};

    public TileEntitySeedAnalyzer() {
        super(AgriCraft.instance.getModTileRegistry().seed_analyzer);
        this.seed = createField(ItemStack.EMPTY, ItemStack::write, ItemStack::read);
        this.journal = createField(ItemStack.EMPTY, ItemStack::write, ItemStack::read);
        this.capability = LazyOptional.of(() -> this);
    }

    @Nonnull
    public ItemStack getSeed() {
        return this.seed.get();
    }

    @Nonnull
    public ItemStack getJournal() {
        return this.journal.get();
    }

    @Override
    protected void writeTileNBT(@Nonnull CompoundNBT tag) {
        // NOOP (everything is handled by auto synced fields)
    }

    @Override
    protected void readTileNBT(@Nonnull BlockState state, @Nonnull CompoundNBT tag) {
        // NOOP (everything is handled by auto synced fields)
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, @Nullable Direction direction) {
        switch (index) {
            case 0:
                if(this.getSeed().isEmpty()) {
                    return stack.isEmpty() || stack.getItem() instanceof IAgriSeedItem;
                } else {
                    if(ItemStack.areItemsEqual(stack, this.getSeed()) && ItemStack.areItemStackTagsEqual(stack, this.getSeed())) {
                        return this.getSeed().getCount() + stack.getCount() <= stack.getMaxStackSize();
                    } else {
                        return false;
                    }
                }
            case 1:
                if(this.getJournal().isEmpty()) {
                    return stack.isEmpty() || stack.getItem() instanceof IAgriJournalItem;
                } else {
                    return stack.isEmpty();
                }
            default: return false;
        }
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        if(stack.isEmpty()) {
            // Doesn't make sense extracting empty stacks
            return false;
        }
        switch(index) {
            case 0:
                if(this.getSeed().isEmpty() || this.getSeed().getCount() < stack.getCount()) {
                    return false;
                }
                return ItemStack.areItemsEqual(stack, this.getSeed()) && ItemStack.areItemStackTagsEqual(stack, this.getSeed());
            case 1:
                // Do not allow automated extraction of the journal
            default: return false;
        }
    }

    @Override
    public int getSizeInventory() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.getSeed().isEmpty() && this.getJournal().isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        switch (index) {
            case 0: return this.getSeed();
            case 1: return this.getJournal();
            default: return ItemStack.EMPTY;
        }
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = ItemStack.EMPTY;
        if(count <= 0) {
            return stack;
        }
        switch (index) {
            case 0:
                stack = this.getSeed().copy();
                if(stack.getCount() > count) {
                    stack.setCount(count);
                    ItemStack seed = this.getSeed();
                    seed.setCount(seed.getCount() - count);
                    this.seed.set(seed);
                } else {
                    this.seed.set(ItemStack.EMPTY);
                }
                return stack;
            case 1:
                stack = this.getJournal().copy();
                this.journal.set(ItemStack.EMPTY);
                return stack;
            default:
                return stack;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = ItemStack.EMPTY;
        switch (index) {
            case 0:
                stack = this.getSeed().copy();
                this.seed.set(ItemStack.EMPTY);
                return stack;
            case 1:
                stack = this.getJournal().copy();
                this.journal.set(ItemStack.EMPTY);
                return stack;
            default:
                return stack;
        }
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if(this.isItemValidForSlot(index, stack)) {
            switch (index) {
                case 0:
                    if(stack.getItem() instanceof IAgriSeedItem) {
                        if (this.getSeed().isEmpty() && !this.getJournal().isEmpty()) {
                            // should always be the case, but it's still modded minecraft
                            if (this.getJournal().getItem() instanceof IAgriJournalItem) {
                                // Add seed to journal if not yet discovered
                                IAgriJournalItem journal = (IAgriJournalItem) this.getJournal().getItem();
                                IAgriPlant plant = ((IAgriSeedItem) stack.getItem()).getPlant(stack);
                                if(!journal.isPlantDiscovered(this.getJournal(), plant)) {
                                    ItemStack newJournal = this.getJournal();
                                    journal.addEntry(newJournal, plant);
                                    this.journal.set(newJournal);
                                }
                            }
                        }
                        this.seed.set(stack);
                    }
                    break;
                case 1:
                    this.journal.set(stack);
                    break;
            }
        }
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return this.canInsertItem(index, stack, null);
    }

    @Override
    public void clear() {
        this.seed.set(ItemStack.EMPTY);
        this.journal.set(ItemStack.EMPTY);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.removed && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return this.capability.cast();
        }
        return super.getCapability(capability, facing);
    }
}