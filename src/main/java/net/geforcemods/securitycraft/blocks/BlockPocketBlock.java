package net.geforcemods.securitycraft.blocks;

import net.geforcemods.securitycraft.blocks.reinforced.BaseReinforcedBlock;
import net.geforcemods.securitycraft.tileentity.BlockPocketTileEntity;
import net.geforcemods.securitycraft.util.IBlockPocket;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.BlockView;

import java.util.function.Supplier;

public class BlockPocketBlock extends BaseReinforcedBlock implements IBlockPocket, BlockEntityProvider
{
	public BlockPocketBlock(Settings settings, Supplier<Block> vB)
	{
		super(settings, vB);
	}

	@Override
	public BlockEntity createBlockEntity(BlockView world)
	{
		return new BlockPocketTileEntity();
	}
}
