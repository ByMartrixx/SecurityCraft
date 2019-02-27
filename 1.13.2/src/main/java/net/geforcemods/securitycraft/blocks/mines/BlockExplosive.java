package net.geforcemods.securitycraft.blocks.mines;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.IExplosive;
import net.geforcemods.securitycraft.blocks.BlockOwnable;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public abstract class BlockExplosive extends BlockOwnable implements IExplosive {

	public BlockExplosive(SoundType soundType, Material material) {
		super(Block.Properties.create(material).sound(soundType).hardnessAndResistance(!ConfigHandler.ableToBreakMines ? -1F : 1F, 6000000.0F));
	}

	@Override
	public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if(!world.isRemote){
			if(player.inventory.getCurrentItem().isEmpty() && explodesWhenInteractedWith() && isActive(world, pos)) {
				explode(world, pos);
				return false;
			}

			if(PlayerUtils.isHoldingItem(player, SCContent.remoteAccessMine))
				return false;

			if(isActive(world, pos) && isDefusable() && PlayerUtils.isHoldingItem(player, SCContent.wireCutters)) {
				defuseMine(world, pos);
				player.inventory.getCurrentItem().damageItem(1, player);
				return false;
			}

			if(!isActive(world, pos) && PlayerUtils.isHoldingItem(player, Items.FLINT_AND_STEEL)) {
				activateMine(world, pos);
				return false;
			}

			if(explodesWhenInteractedWith() && isActive(world, pos))
				explode(world, pos);

			return false;
		}

		return false;
	}

	/**
	 * @return If the mine should explode when right-clicked?
	 */
	public boolean explodesWhenInteractedWith() {
		return true;
	}

	@Override
	public abstract void explode(World world, BlockPos pos);

	@Override
	public boolean isDefusable(){
		return true;
	}

}