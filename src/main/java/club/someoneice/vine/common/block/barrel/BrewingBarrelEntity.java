package club.someoneice.vine.common.block.barrel;

import club.someoneice.vine.common.container.ContainerBarrel;
import club.someoneice.vine.common.item.Wine;
import club.someoneice.vine.core.Data;
import club.someoneice.vine.core.TagHelper;
import club.someoneice.vine.init.TileInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BrewingBarrelEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    private final SimpleContainer itemList = new SimpleContainer(2);
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new InvWrapper(itemList));
    public final ContainerData data;

    public boolean hasWater, hasWine, isFinish;
    private int progress;
    private int time;

    public BrewingBarrelEntity(BlockPos pos, BlockState state) {
        super(TileInit.BrewingBarrel.get(), pos, state);
        this.hasWater = false;
        this.hasWine = false;
        this.isFinish = false;
        this.progress = 0;
        this.time = 0;

        this.data = new ContainerData() {

            @Override
            public int get(int index) {
                if (index == 0)
                    return BrewingBarrelEntity.this.getWineProgress();
                else return -1;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0)
                    BrewingBarrelEntity.this.progress = value;
            }

            // 資訊數量
            @Override
            public int getCount() {
                return 1;
            }
        };
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        hasWater = tag.getBoolean("hasWater");
        hasWine = tag.getBoolean("hasWine");
        isFinish = tag.getBoolean("isFinish");
        progress = tag.getInt("progress");
        time = tag.getInt("time");
        itemList.fromTag((ListTag) tag.get("contents"));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putBoolean("hasWater", hasWater);
        tag.putBoolean("hasWine", hasWine);
        tag.putBoolean("isFinish", isFinish);
        tag.putInt("progress", progress);
        tag.putInt("time", time);
        tag.put("contents", itemList.createTag());

        super.saveAdditional(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        tag.putBoolean("hasWater", hasWater);
        tag.putBoolean("hasWine", hasWine);
        tag.putBoolean("isFinish", isFinish);
        tag.putInt("progress", progress);
        tag.putInt("time", time);
        tag.put("contents", itemList.createTag());

        return tag;
    }

    public ListTag createItemListTag() {
        return itemList.createTag();
    }

    public boolean isItemListEmpty() {
        return itemList.isEmpty();
    }

    public void loadItemListFromTag(ListTag tag) {
        itemList.fromTag(tag);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return handler.cast();
        return super.getCapability(cap, side);
    }

    public void setWater() {
        this.hasWater = true;
    }

    public int getWineProgress() {
        return this.progress;
    }

    public boolean setWine(ItemStack item) {
        if (!item.is(Items.MILK_BUCKET) && item.getCount() < 16) return false;
        Wine wine = null;
        if (Data.wineItemMap.containsKey(item.getItem())) wine = Data.wineItemMap.get(item.getItem());
        else {
            for (var i : Data.wineTagList.keySet()) {
                if (item.is(i)) wine = Data.wineTagList.get(i);
            }

            if (wine == null) return false;
        }

        if (item.is(Items.MILK_BUCKET) || item.is(TagHelper.Milk)) this.setWater();
        final ItemStack finalWine = new ItemStack(wine.wineBottle.get(), 8);
        final ItemStack finalItem = item.copy();
        finalItem.setCount(16);
        this.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
            it.insertItem(0, finalItem, false);
            it.insertItem(1, finalWine, false);
        });

        item.shrink(16);
        this.hasWine = true;
        return true;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, BrewingBarrelEntity entity) {
        if (entity.hasWater && entity.hasWine && !entity.isFinish) {
            entity.time++;
            if (entity.time >= 20 * 30) {
                entity.progress++;
                entity.time = 0;
            }

            if (entity.progress >= 100) entity.isFinish = true;
        }

        if (entity.isFinish) {
            entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
                it.getStackInSlot(0).setCount(0);
            });
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tksrwine.brewing_barrel");
    }

    @Override
    public int[] getSlotsForFace(Direction p_19238_) {
        return new int[]{0, 1};
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack item) {
        return false;
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_19235_, ItemStack p_19236_, @Nullable Direction p_19237_) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int p_19239_, ItemStack p_19240_, Direction p_19241_) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.itemList.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.itemList.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int s1, int s2) {
        return this.itemList.removeItem(s1, s2);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.itemList.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        this.itemList.setItem(slot, item);
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D, this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    @Override
    public void clearContent() {
        this.itemList.clearContent();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerBarrel(id, inv, this, this.data);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void init() {
        this.isFinish = false;
        this.hasWater = false;
        this.hasWine = false;
        this.time = 0;
        this.progress = 0;
    }
}
