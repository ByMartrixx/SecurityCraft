package net.geforcemods.securitycraft.blocks;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.IOwnable;
import net.geforcemods.securitycraft.gui.GuiHandler;
import net.geforcemods.securitycraft.tileentity.TileEntityKeypadFurnace;
import net.geforcemods.securitycraft.util.BlockUtils;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockKeypadFurnace extends BlockOwnable implements IPasswordConvertible {

	public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
	public static final PropertyBool OPEN = PropertyBool.create("open");

	public BlockKeypadFurnace(Material materialIn) {
		super(materialIn);
		setSoundType(SoundType.METAL);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	/**
	 * Is this block (a) opaque and (b) a full 1m cube?  This determines whether or not to render the shared face of two
	 * adjacent blocks and also whether the player can attach torches, redstone wire, etc to this block.
	 */
	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	/**
	 * If this block doesn't render as an ordinary block it will return False (examples: signs, buttons, stairs, etc)
	 */
	@Override
	public boolean isNormalCube(IBlockState state)
	{
		return false;
	}

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{
		TileEntity tileentity = worldIn.getTileEntity(pos);

		if (tileentity instanceof IInventory)
		{
			InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory)tileentity);
			worldIn.updateComparatorOutputLevel(pos, this);
		}

		super.breakBlock(worldIn, pos, state);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
		if(!worldIn.isRemote)
		{
			if(!PlayerUtils.isHoldingItem(playerIn, SCContent.codebreaker))
				((TileEntityKeypadFurnace) worldIn.getTileEntity(pos)).openPasswordGUI(playerIn);
		}

		return true;
	}

	public static void activate(World par1World, BlockPos pos, EntityPlayer player){
		if(!BlockUtils.getBlockPropertyAsBoolean(par1World, pos, BlockKeypadFurnace.OPEN))
			BlockUtils.setBlockProperty(par1World, pos, BlockKeypadFurnace.OPEN, true, false);

		par1World.playEvent((EntityPlayer)null, 1006, pos, 0);
		player.openGui(SecurityCraft.instance, GuiHandler.KEYPAD_FURNACE_GUI_ID, par1World, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand)
	{
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite()).withProperty(OPEN, false);
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		if(meta <= 5)
			return getDefaultState().withProperty(FACING, EnumFacing.values()[meta].getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : EnumFacing.values()[meta]).withProperty(OPEN, false);
		else
			return getDefaultState().withProperty(FACING, EnumFacing.values()[meta - 6]).withProperty(OPEN, true);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		if(state.getValue(OPEN).booleanValue())
			return (state.getValue(FACING).getIndex() + 6);
		else
			return state.getValue(FACING).getIndex();
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this, new IProperty[] {FACING, OPEN});
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new TileEntityKeypadFurnace();
	}

	@Override
	public Block getOriginalBlock()
	{
		return Blocks.FURNACE;
	}

	@Override
	public boolean convert(EntityPlayer player, World world, BlockPos pos)
	{
		EnumFacing enumfacing = world.getBlockState(pos).getValue(FACING);
		TileEntityFurnace furnace = (TileEntityFurnace)world.getTileEntity(pos);
		NBTTagCompound tag = furnace.writeToNBT(new NBTTagCompound());

		furnace.clear();
		world.setBlockState(pos, SCContent.keypadFurnace.getDefaultState().withProperty(FACING, enumfacing).withProperty(OPEN, false));
		((IOwnable) world.getTileEntity(pos)).getOwner().set(player.getName(), player.getUniqueID().toString());
		((TileEntityKeypadFurnace)world.getTileEntity(pos)).readFromNBT(tag);
		return true;
	}
}
