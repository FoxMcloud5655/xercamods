package xerca.xercapaint.common.item;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HangingEntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.NonnullDefault;
import xerca.xercapaint.client.CanvasItemRenderer;
import xerca.xercapaint.client.ClientStuff;
import xerca.xercapaint.common.CanvasType;
import xerca.xercapaint.common.entity.Entities;
import xerca.xercapaint.common.entity.EntityCanvas;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;

@NonnullDefault
public class ItemCanvas extends HangingEntityItem {
    private CanvasType canvasType;

    ItemCanvas(String name, CanvasType canvasType) {
        super(Entities.CANVAS, new Properties().tab(Items.paintTab).stacksTo(1).setISTER(() -> new Callable<ItemStackTileEntityRenderer>()
        {
            @Nullable
            CanvasItemRenderer r = null;

            @Override
            public ItemStackTileEntityRenderer call() {
                if(r == null)
                    r = new CanvasItemRenderer();
                return r;
            }
        }));

        this.setRegistryName(name);
        this.canvasType = canvasType;
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, @Nonnull Hand hand) {
        if(worldIn.isClientSide){
            ClientStuff.showCanvasGui(playerIn);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getItemInHand(hand));
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockPos pos = blockpos.relative(direction);
        PlayerEntity playerentity = context.getPlayer();
        ItemStack itemstack = context.getItemInHand();
        if (playerentity != null && !this.mayPlace(playerentity, direction, itemstack, pos)) {
            if(playerentity.level.isClientSide){
                ClientStuff.showCanvasGui(playerentity);
            }
        } else {
            World world = context.getLevel();

            CompoundNBT tag = itemstack.getTag();
            if(tag == null || !tag.contains("pixels") || !tag.contains("name")){
                if(world.isClientSide && playerentity != null){
                    ClientStuff.showCanvasGui(playerentity);
                }
                return ActionResultType.SUCCESS;
            }

            int rotation = 0;
            if(direction.getAxis() == Direction.Axis.Y){
                double xDiff = blockpos.getX() - playerentity.getX();
                double zDiff = blockpos.getZ() - playerentity.getZ();
                if(Math.abs(xDiff) > Math.abs(zDiff)){
                    if(xDiff > 0){
                        rotation = 1;
                    }else{
                        rotation = 3;
                    }
                }else{
                    if(zDiff > 0){
                        rotation = 2;
                    }else{
                        rotation = 0;
                    }
                }
                if(direction == Direction.DOWN && Math.abs(xDiff) < Math.abs(zDiff)){
                    rotation += 2;
                }
            }

            if (!world.isClientSide) {
                EntityCanvas entityCanvas = new EntityCanvas(world, tag, pos, direction, canvasType, rotation);

                if (entityCanvas.survives()) {
                    entityCanvas.playPlacementSound();
                    world.addFreshEntity(entityCanvas);
                    itemstack.shrink(1);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    public static boolean hasTitle(@Nonnull ItemStack stack){
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            if(tag != null){
                String s = tag.getString("title");
                return !StringUtil.isNullOrEmpty(s);
            }
        }
        return false;
    }

    public static ITextComponent getFullLabel(@Nonnull ItemStack stack){
//        TextComponent label = new TextComponent("");
        String labelString = "";
        int generation = 0;
        ITextComponent title = getCustomTitle(stack);
        if(title != null){
            labelString += (title.getString() + " ");
        }
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            String s = tag.getString("author");

            if (!StringUtil.isNullOrEmpty(s)) {
                labelString += (new TranslationTextComponent("canvas.byAuthor", s)).getString() + " ";
            }

            generation = tag.getInt("generation");
//            if(generation > 0){
//                labelString += (new TranslatableComponent("canvas.generation." + (generation - 1))).getString();
//            }
        }
        StringTextComponent label = new StringTextComponent(labelString);
        if(generation == 1){
            label.withStyle(TextFormatting.YELLOW);
        }
        else if(generation >= 3){
            label.withStyle(TextFormatting.GRAY);
        }
        return label;
    }

    @Nullable
    public static ITextComponent getCustomTitle(@Nonnull ItemStack stack){
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            if(tag != null){
                String s = tag.getString("title");
                if (!StringUtil.isNullOrEmpty(s)) {
                    return new StringTextComponent(s);
                }
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public ITextComponent getName(@Nonnull ItemStack stack) {
        ITextComponent comp = getCustomTitle(stack);
        if(comp != null){
            return comp;
        }
        return super.getName(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.hasTag()) {
            CompoundNBT tag = stack.getTag();
            String s = tag.getString("author");

            if (!StringUtils.isNullOrEmpty(s)) {
                tooltip.add(new TranslationTextComponent("canvas.byAuthor", s));
            }

            int generation = tag.getInt("generation");
            // generation = 0 means empty, 1 means original, more means copy
            if(generation > 0){
                tooltip.add((new TranslationTextComponent("canvas.generation." + (generation - 1))).withStyle(TextFormatting.GRAY));
            }
        }else{
            tooltip.add(new TranslationTextComponent("canvas.empty").withStyle(TextFormatting.GRAY));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isFoil(ItemStack stack) {
        if(stack.hasTag()){
            CompoundNBT tag = stack.getTag();
            if(tag != null) {
                int generation = tag.getInt("generation");
                return generation > 0;
            }
        }
        return false;
    }

    public int getWidth() {
        return CanvasType.getWidth(canvasType);
    }

    public int getHeight() {
        return CanvasType.getHeight(canvasType);
    }

    public CanvasType getCanvasType() {
        return canvasType;
    }

    protected boolean mayPlace(PlayerEntity playerIn, Direction directionIn, ItemStack itemStackIn, BlockPos posIn) {
        if(canvasType == CanvasType.SMALL){
            return !World.isOutsideBuildHeight(posIn) && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
        }
        else{
            return !directionIn.getAxis().isVertical() && playerIn.mayUseItemAt(posIn, directionIn, itemStackIn);
        }
    }
}
