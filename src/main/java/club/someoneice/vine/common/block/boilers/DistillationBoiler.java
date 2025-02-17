package club.someoneice.vine.common.block.boilers;

import club.someoneice.vine.common.item.Wine;
import club.someoneice.vine.core.Data;
import club.someoneice.vine.init.ItemInit;
import club.someoneice.vine.init.TileInit;
import club.someoneice.vine.util.Utilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class DistillationBoiler extends BaseEntityBlock {
    private static final IntegerProperty AMOUNT = IntegerProperty.create("amount", 1, 3);
    private static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public DistillationBoiler() {
        super(Properties.copy(Blocks.IRON_BLOCK).noOcclusion());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity entity, ItemStack item) {
        var tag = item.getOrCreateTag();
        if (tag.contains("contents")) {
            BlockEntity blockentity = world.getBlockEntity(pos);
            if (blockentity instanceof DistillationBoilerEntity db)
                db.loadItemListFromTag((ListTag) tag.get("contents"));
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof DistillationBoilerEntity entity) {
            if (player.isShiftKeyDown()) {
                NetworkHooks.openScreen(((ServerPlayer) player), entity, pos);
                return InteractionResult.sidedSuccess(true);
            } else {
                var item = player.getMainHandItem();

                if (!entity.isFinish) {
                    if (item.is(Items.WATER_BUCKET) && !entity.hasWater) {
                        entity.setWater();
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F + (world.random.nextFloat() - world.random.nextFloat()) * 0.4F);
                        item.shrink(1);
                        Utilities.addItem2PlayerOrDrop(player, Items.BUCKET.getDefaultInstance());
                        return InteractionResult.SUCCESS;
                    } else if (!entity.hasWine && entity.setWine(item)) return InteractionResult.SUCCESS;
                } else {
                    if (item.is(ItemInit.WineBottle.get())) {
                        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
                            if (it.getStackInSlot(1).getCount() < 4) return;
                            if (it.getStackInSlot(1).getCount() - 4 == 0) entity.init();
                            var itm = it.extractItem(1, 4, false);
                            Utilities.addItem2PlayerOrDrop(player, Data.wineMap.get(((Wine.WineItem) itm.getItem()).name).wineBottle.get().getDefaultInstance());
                        });
                        item.shrink(1);

                        return InteractionResult.SUCCESS;
                    } else if (item.is(ItemInit.Cup.get())) {
                        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
                            if (it.getStackInSlot(1).getCount() - 1 == 0) entity.init();
                            var itm = it.extractItem(1, 1, false);
                            Utilities.addItem2PlayerOrDrop(player, Data.wineMap.get(((Wine.WineItem) itm.getItem()).name).cup.get().getDefaultInstance());
                        });
                        item.shrink(1);

                        return InteractionResult.SUCCESS;
                    } else if (item.is(Items.GLASS_BOTTLE)) {
                        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
                            if (it.getStackInSlot(1).getCount() - 1 == 0) entity.init();
                            var itm = it.extractItem(1, 1, false);
                            Utilities.addItem2PlayerOrDrop(player, Data.wineMap.get(((Wine.WineItem) itm.getItem()).name).bottle.get().getDefaultInstance());
                        });
                        item.shrink(1);

                        return InteractionResult.SUCCESS;
                    } else if (item.is(ItemInit.GlassBottle.get())) {
                        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
                            var itm = it.extractItem(1, 1, false);
                            Utilities.addItem2PlayerOrDrop(player, Data.wineMap.get(((Wine.WineItem) itm.getItem()).name).glass.get().getDefaultInstance());
                            if (it.getStackInSlot(1) == ItemStack.EMPTY) entity.init();
                        });
                        item.shrink(1);

                        return InteractionResult.SUCCESS;
                    } else if (item.is(Items.BUCKET)) {
                        entity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(it -> {
                            if (it.getStackInSlot(1).getCount() < 4) return;
                            if (it.getStackInSlot(1).getCount() - 4 == 0) entity.init();

                            var itm = it.extractItem(1, 4, false);
                            Utilities.addItem2PlayerOrDrop(player, Data.wineMap.get(((Wine.WineItem) itm.getItem()).name).bucket.get().getDefaultInstance());
                        });
                        item.shrink(1);

                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return InteractionResult.FAIL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DistillationBoilerEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> tileentity) {
        return createTickerHelper(tileentity, TileInit.DistillationBoiler.get(), DistillationBoilerEntity::tick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AMOUNT, FACING);
    }

    @javax.annotation.Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        var ret = ItemInit.DistillationBoilerItem.get().getDefaultInstance();
        BlockEntity blockentity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockentity instanceof DistillationBoilerEntity db) {
            if (!db.isItemListEmpty()) {
                var tag = ret.getOrCreateTag();
                tag.put("contents", db.createItemListTag());
            }
        }
        return Collections.singletonList(ret);
    }
}
