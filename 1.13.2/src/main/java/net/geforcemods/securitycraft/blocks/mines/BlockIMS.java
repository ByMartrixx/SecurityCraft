package net.geforcemods.securitycraft.blocks.mines;

import java.util.Random;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.blocks.BlockOwnable;
import net.geforcemods.securitycraft.gui.GuiHandler;
import net.geforcemods.securitycraft.tileentity.TileEntityIMS;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Particles;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;

public class BlockIMS extends BlockOwnable {

	public static final PropertyInteger MINES = PropertyInteger.create("mines", 0, 4);

	public BlockIMS(Material material) {
		super(SoundType.METAL, Block.Properties.create(material).hardnessAndResistance(!ConfigHandler.ableToBreakMines ? -1F : 0.7F, 6000000.0F));
	}

	@Override
	public boolean isNormalCube(IBlockState state){
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state){
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public VoxelShape getShape(IBlockState state, IBlockReader source, BlockPos pos)
	{
		return VoxelShapes.create(new AxisAlignedBB(0F, 0F, 0F, 1F, 0.45F, 1F));
	}

	@Override
	public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if(!world.isRemote)
		{
			if(((IOwnable) world.getTileEntity(pos)).getOwner().isOwner(player))
			{
				ItemStack held = player.getHeldItem(hand);
				int mines = state.get(MINES);

				if(held.getItem() == Item.getItemFromBlock(SCContent.bouncingBetty) && mines < 4)
				{
					if(!player.isCreative())
						held.shrink(1);

					world.setBlockState(pos, state.with(MINES, mines + 1));
					((TileEntityIMS)world.getTileEntity(pos)).setBombsRemaining(mines + 1);
				}
				else
					player.openGui(SecurityCraft.instance, GuiHandler.IMS_GUI_ID, world, pos.getX(), pos.getY(), pos.getZ());
				return true;
			}
		}

		return true;
	}

	/**
	 * A randomly called display update to be able to add particles or other items for display
	 */
	@Override
	public void randomTick(IBlockState state, World world, BlockPos pos, Random random){
		if(world.getTileEntity(pos) != null && world.getTileEntity(pos) instanceof TileEntityIMS && ((TileEntityIMS) world.getTileEntity(pos)).getBombsRemaining() == 0){
			double x = pos.getX() + 0.5F + (random.nextFloat() - 0.5F) * 0.2D;
			double y = pos.getY() + 0.4F + (random.nextFloat() - 0.5F) * 0.2D;
			double z = pos.getZ() + 0.5F + (random.nextFloat() - 0.5F) * 0.2D;
			double magicNumber1 = 0.2199999988079071D;
			double magicNumber2 = 0.27000001072883606D;

			world.addParticle(Particles.SMOKE, x - magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.addParticle(Particles.SMOKE, x + magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.addParticle(Particles.SMOKE, x, y + magicNumber1, z - magicNumber2, 0.0D, 0.0D, 0.0D);
			world.addParticle(Particles.SMOKE, x, y + magicNumber1, z + magicNumber2, 0.0D, 0.0D, 0.0D);
			world.addParticle(Particles.SMOKE, x, y, z, 0.0D, 0.0D, 0.0D);

			world.addParticle(Particles.FLAME, x - magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
			world.addParticle(Particles.FLAME, x + magicNumber2, y + magicNumber1, z, 0.0D, 0.0D, 0.0D);
		}
	}

	@Override
	public void getDrops(IBlockState state, NonNullList<ItemStack> drops, World world, BlockPos pos, int fortune)
	{
		int mines = state.get(MINES);

		if(mines != 0)
			drops.add(new ItemStack(SCContent.bouncingBetty, mines));
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
	{
		return getDefaultState().withProperty(MINES, 4);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState().withProperty(MINES, meta);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		return (state.getValue(MINES).intValue());
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, new IProperty[] {MINES});
	}

	@Override
	public TileEntity createNewTileEntity(IBlockReader world) {
		return new TileEntityIMS();
	}

}