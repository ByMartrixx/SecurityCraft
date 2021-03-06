package net.geforcemods.securitycraft.tileentity;

import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.SecurityCraft;
import net.geforcemods.securitycraft.api.CustomizableTileEntity;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.api.OwnableTileEntity;
import net.geforcemods.securitycraft.blocks.BlockPocketManagerBlock;
import net.geforcemods.securitycraft.blocks.BlockPocketWallBlock;
import net.geforcemods.securitycraft.blocks.reinforced.ReinforcedRotatedPillarBlock;
import net.geforcemods.securitycraft.containers.GenericTEContainer;
import net.geforcemods.securitycraft.misc.ModuleType;
//import net.geforcemods.securitycraft.network.server.AssembleBlockPocket;
//import net.geforcemods.securitycraft.network.server.ToggleBlockPocketManager;
import net.geforcemods.securitycraft.util.ClientUtils;
import net.geforcemods.securitycraft.util.IBlockPocket;
import net.geforcemods.securitycraft.util.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

import java.util.ArrayList;
import java.util.List;

public class BlockPocketManagerTileEntity extends CustomizableTileEntity implements NamedScreenHandlerFactory
{
	public static final int RENDER_DISTANCE = 100;

	public boolean enabled = false;
	public boolean showOutline = false;
	public int size = 5;
	private List<BlockPos> blocks = new ArrayList<>();
	private List<BlockPos> walls = new ArrayList<>();
	private List<BlockPos> floor = new ArrayList<>();

	public BlockPocketManagerTileEntity()
	{
		super(SCContent.teTypeBlockPocketManager);
	}

	/**
	 * Enables the block pocket
	 * @return The feedback message. null if none should be sent.
	 */
	public TranslatableText enableMultiblock()
	{
		if(!enabled) //multiblock detection
		{
//			if(world.isClient) // TODO
//				SecurityCraft.channel.sendToServer(new ToggleBlockPocketManager(this, true, size));

			List<BlockPos> blocks = new ArrayList<>();
			List<BlockPos> sides = new ArrayList<>();
			List<BlockPos> floor = new ArrayList<>();
			final Direction managerFacing = world.getBlockState(pos).get(BlockPocketManagerBlock.FACING);
			final Direction left = managerFacing.rotateYClockwise();
			final Direction right = left.getOpposite();
			final Direction back = left.rotateYClockwise();
			final BlockPos startingPos;
			final int lowest = 0;
			final int highest = size - 1;
			BlockPos pos = getPos().toImmutable();
			int xi = lowest;
			int yi = lowest;
			int zi = lowest;

			while(world.getBlockState(pos = pos.offset(left)).getBlock() instanceof IBlockPocket) //find the bottom left corner
				;

			pos = pos.offset(right); //pos got offset one too far (which made the while loop above stop) so it needs to be corrected
			startingPos = pos.toImmutable();

			//looping through cube level by level
			while(yi < size)
			{
				while(zi < size)
				{
					while(xi < size)
					{
						//skip the blocks in the middle
						if(xi > lowest && yi > lowest && zi > lowest && xi < highest && yi < highest && zi < highest)
						{
							xi++;
							continue;
						}

						BlockPos currentPos = pos.offset(right, xi);
						BlockState currentState = world.getBlockState(currentPos);

						if(currentState.getBlock() instanceof BlockPocketManagerBlock && !currentPos.equals(getPos()))
							return new TranslatableText("messages.securitycraft:blockpocket.multipleManagers");

						//checking the lowest and highest level of the cube
						if((yi == lowest && !currentPos.equals(getPos())) || yi == highest) //if (y level is lowest AND it's not the block pocket manager's position) OR (y level is highest)
						{
							//checking the corners
							if(((xi == lowest && zi == lowest) || (xi == lowest && zi == highest) || (xi == highest && zi == lowest) || (xi == highest && zi == highest)))
							{
								if(currentState.getBlock() != SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ)
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ.getTranslationKey()));
							}
							//checking the sides parallel to the block pocket manager
							else if((zi == lowest || zi == highest) && xi > lowest && xi < highest)
							{
								Axis typeToCheckFor = managerFacing == Direction.NORTH || managerFacing == Direction.SOUTH ? Axis.X : Axis.Z;

								if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR || currentState.get(Properties.AXIS) != typeToCheckFor)
								{
									if(currentState.getBlock() == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
										return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock.rotation", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.getTranslationKey()));
								}
							}
							//checking the sides orthogonal to the block pocket manager
							else if((xi == lowest || xi == highest) && zi > lowest && zi < highest)
							{
								Axis typeToCheckFor = managerFacing == Direction.NORTH || managerFacing == Direction.SOUTH ? Axis.Z : Axis.X;

								if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR || currentState.get(Properties.AXIS) != typeToCheckFor)
								{
									if (currentState.getBlock() == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
										return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock.rotation", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.getTranslationKey()));
								}
							}
							//checking the middle plane
							else if(xi > lowest && zi > lowest && xi < highest && zi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock))
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.BLOCK_POCKET_WALL.getTranslationKey()));

								floor.add(currentPos);
								sides.add(currentPos);
							}
						}
						//checking the corner edges
						else if(yi != lowest && yi != highest && ((xi == lowest && zi == lowest) || (xi == lowest && zi == highest) || (xi == highest && zi == lowest) || (xi == highest && zi == highest)))
						{
							if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR || currentState.get(Properties.AXIS) != Axis.Y)
							{
								if (currentState.getBlock() == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock.rotation", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));
								return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.getTranslationKey()));
							}
						}
						//checking the walls
						else if(yi > lowest && yi < highest)
						{
							//checking the walls parallel to the block pocket manager
							if((zi == lowest || zi == highest) && xi > lowest && xi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock))
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.BLOCK_POCKET_WALL.getTranslationKey()));

								sides.add(currentPos);
							}
							//checking the walls orthogonal to the block pocket manager
							else if((xi == lowest || xi == highest) && zi > lowest && zi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock))
									return new TranslatableText("messages.securitycraft:blockpocket.invalidBlock", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()), new TranslatableText(SCContent.BLOCK_POCKET_WALL.getTranslationKey()));

								sides.add(currentPos);
							}
						}

						OwnableTileEntity te = (OwnableTileEntity)world.getBlockEntity(currentPos);

						if(!getOwner().owns(te))
							return new TranslatableText("messages.securitycraft:blockpocket.unowned", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));
						else
							blocks.add(currentPos);

						xi++;
					}

					xi = 0;
					zi++;
					pos = startingPos.up(yi).offset(back, zi);
				}

				zi = 0;
				yi++;
				pos = startingPos.up(yi);
			}

			this.blocks = blocks;
			this.walls = sides;
			this.floor = floor;
			enabled = true;

			for(BlockPos blockPos : blocks)
			{
				BlockEntity te = world.getBlockEntity(blockPos);

				if(te instanceof BlockPocketTileEntity)
					((BlockPocketTileEntity)te).setManager(this);
			}

			for(BlockPos blockPos : floor)
			{
				world.setBlockState(blockPos, world.getBlockState(blockPos).with(BlockPocketWallBlock.SOLID, true));
			}

			setWalls(!hasModule(ModuleType.DISGUISE));
			return new TranslatableText("messages.securitycraft:blockpocket.activated");
		}

		return null;
	}

	/**
	 * Auto-assembles the Block Pocket for a player.
	 * First it makes sure that the space isn't occupied, then it checks the inventory of the player for the required items, then it places the blocks.
	 * @param player The player that opened the screen, used to check if the player is in creative or not
	 * @return The feedback message. null if none should be sent.
	 */
	public TranslatableText autoAssembleMultiblock(PlayerEntity player)
	{
		if(!enabled) //multiblock assembling in three steps
		{
//			if(world.isClient) // TODO
//				SecurityCraft.channel.sendToServer(new AssembleBlockPocket(this, size));

			final Direction managerFacing = world.getBlockState(pos).get(BlockPocketManagerBlock.FACING);
			final Direction left = managerFacing.rotateYClockwise();
			final Direction right = left.getOpposite();
			final Direction back = left.rotateYClockwise();
			final BlockPos startingPos;
			final int lowest = 0;
			final int half = (size - 1) / 2;
			final int highest = size - 1;
			BlockPos pos = getPos().toImmutable();
			int xi = lowest;
			int yi = lowest;
			int zi = lowest;
			int wallsNeeded = 0;
			int pillarsNeeded = 0;
			int chiseledNeeded = 0;

			pos = pos.offset(right, -half);
			startingPos = pos.toImmutable();

			//Step 1: looping through cube level by level to make sure the space where the BP should go to isn't occupied
			while(yi < size)
			{
				while(zi < size)
				{
					while(xi < size)
					{
						//skip the blocks in the middle
						if(xi > lowest && yi > lowest && zi > lowest && xi < highest && yi < highest && zi < highest)
						{
							xi++;
							continue;
						}

						BlockPos currentPos = pos.offset(right, xi);
						BlockState currentState = world.getBlockState(currentPos);

						currentState.getMaterial().isReplaceable();

						//checking the lowest and highest level of the cube
						if((yi == lowest && !currentPos.equals(getPos())) || yi == highest) //if (y level is lowest AND it's not the block pocket manager's position) OR (y level is highest)
						{
							//checking the corners
							if(((xi == lowest && zi == lowest) || (xi == lowest && zi == highest) || (xi == highest && zi == lowest) || (xi == highest && zi == highest)))
							{
								if(currentState.getBlock() != SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ && !(currentState.getMaterial().isReplaceable()))
									return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

								if(currentState.getMaterial().isReplaceable())
									chiseledNeeded++;
							}
							//checking the sides parallel to the block pocket manager
							else if((zi == lowest || zi == highest) && xi > lowest && xi < highest)
							{
								Axis typeToCheckFor = managerFacing == Direction.NORTH || managerFacing == Direction.SOUTH ? Axis.X : Axis.Z;

								if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR && !(currentState.getMaterial().isReplaceable()) || (currentState.getBlock() == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR && currentState.get(Properties.AXIS) != typeToCheckFor))
									return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

								if(currentState.getMaterial().isReplaceable())
									pillarsNeeded++;
							}
							//checking the sides orthogonal to the block pocket manager
							else if((xi == lowest || xi == highest) && zi > lowest && zi < highest)
							{
								Axis typeToCheckFor = managerFacing == Direction.NORTH || managerFacing == Direction.SOUTH ? Axis.Z : Axis.X;

								if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR && !(currentState.getMaterial().isReplaceable()) || (currentState.getBlock() == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR && currentState.get(Properties.AXIS) != typeToCheckFor))
									return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

								if(currentState.getMaterial().isReplaceable())
									pillarsNeeded++;
							}
							//checking the middle plane
							else if(xi > lowest && zi > lowest && xi < highest && zi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock) && !(currentState.getMaterial().isReplaceable()))
									return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

								if(currentState.getMaterial().isReplaceable())
									wallsNeeded++;
							}
						}
						//checking the corner edges
						else if(yi != lowest && yi != highest && ((xi == lowest && zi == lowest) || (xi == lowest && zi == highest) || (xi == highest && zi == lowest) || (xi == highest && zi == highest)))
						{
							if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR && !(currentState.getMaterial().isReplaceable()) || (currentState.getBlock() == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR && currentState.get(Properties.AXIS) != Axis.Y))
								return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

							if(currentState.getMaterial().isReplaceable())
								pillarsNeeded++;
						}
						//checking the walls
						else if(yi > lowest && yi < highest)
						{
							//checking the walls parallel to the block pocket manager
							if((zi == lowest || zi == highest) && xi > lowest && xi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock) && !(currentState.getMaterial().isReplaceable()))
									return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

								if(currentState.getMaterial().isReplaceable())
									wallsNeeded++;
							}
							//checking the walls orthogonal to the block pocket manager
							else if((xi == lowest || xi == highest) && zi > lowest && zi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock) && !(currentState.getMaterial().isReplaceable()))
									return new TranslatableText("messages.securitycraft:blockpocket.blockInWay", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));

								if(currentState.getMaterial().isReplaceable())
									wallsNeeded++;
							}
						}

						if(world.getBlockEntity(currentPos) instanceof OwnableTileEntity)
						{
							OwnableTileEntity te = (OwnableTileEntity)world.getBlockEntity(currentPos);

							if(!getOwner().owns(te))
								return new TranslatableText("messages.securitycraft:blockpocket.unowned", currentPos, new TranslatableText(currentState.getBlock().asItem().getTranslationKey()));
						}

						xi++;
					}

					xi = 0;
					zi++;
					pos = startingPos.up(yi).offset(back, zi);
				}

				zi = 0;
				yi++;
				pos = startingPos.up(yi);
			} //if the code comes to this place, the space is either clear or occupied by blocks that would have been placed either way, or existing blocks can be replaced (like grass)

			if(chiseledNeeded + pillarsNeeded + wallsNeeded == 0) //this applies when no blocks are missing, so when the BP is already in place
				return new TranslatableText("messages.securitycraft:blockpocket.alreadyAssembled");

			//Step 2: if the player isn't in creative, it is checked if they have enough items to build the BP. If so, they're removed
			if(!player.isCreative())
			{
				int chiseledFound = 0;
				int pillarsFound = 0;
				int wallsFound = 0;
				DefaultedList<ItemStack> inventory = player.inventory.main;

				for(int i = 1; i <= inventory.size(); i++)
				{
					ItemStack stackToCheck = inventory.get(i - 1);

					if(!stackToCheck.isEmpty() && stackToCheck.getItem() instanceof BlockItem)
					{
						Block block = ((BlockItem)stackToCheck.getItem()).getBlock();

						if(block instanceof ShulkerBoxBlock && stackToCheck.hasTag()) //there has to be a check for shulker boxes, otherwise the huge BPs that take 4000 blocks to build couldn't be auto-assembled due to lack of inventory space
						{
							DefaultedList<ItemStack> contents = DefaultedList.<ItemStack>ofSize(27, ItemStack.EMPTY);

							Inventories.fromTag(stackToCheck.getTag().getCompound("BlockEntityTag"), contents);

							for(ItemStack boxStack : contents)
							{
								if(!(boxStack.getItem() instanceof BlockItem))
									continue;

								block = ((BlockItem)boxStack.getItem()).getBlock();

								if(block == SCContent.BLOCK_POCKET_WALL)
									wallsFound += boxStack.getCount();
								else if(block == SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ)
									chiseledFound += boxStack.getCount();
								else if(block == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
									pillarsFound += boxStack.getCount();
							}
						}
						else if(block == SCContent.BLOCK_POCKET_WALL)
							wallsFound += stackToCheck.getCount();
						else if(block == SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ)
							chiseledFound += stackToCheck.getCount();
						else if(block == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
							pillarsFound += stackToCheck.getCount();
					}
				}

				if(chiseledNeeded > chiseledFound || pillarsNeeded > pillarsFound || wallsNeeded > wallsFound)
					return new TranslatableText("messages.securitycraft:blockpocket.notEnoughItems");

				for(int i = 1; i <= inventory.size(); i++) //actually take out the items that are used for assembling the BP
				{
					ItemStack stackToCheck = inventory.get(i - 1);

					if(!stackToCheck.isEmpty() && stackToCheck.getItem() instanceof BlockItem)
					{
						Block block = ((BlockItem)stackToCheck.getItem()).getBlock();
						int count = stackToCheck.getCount();

						if(block instanceof ShulkerBoxBlock && stackToCheck.hasTag())
						{
							CompoundTag stackTag = stackToCheck.getTag();
							CompoundTag blockEntityTag = stackTag.getCompound("BlockEntityTag");
							DefaultedList<ItemStack> contents = DefaultedList.<ItemStack>ofSize(27, ItemStack.EMPTY);

							Inventories.fromTag(blockEntityTag, contents);

							for(int j = 0; j < contents.size(); j++)
							{
								ItemStack boxStack = contents.get(j);

								if(!(boxStack.getItem() instanceof BlockItem))
									continue;

								block = ((BlockItem)boxStack.getItem()).getBlock();
								count = boxStack.getCount();

								if(block == SCContent.BLOCK_POCKET_WALL)
								{
									if(count <= wallsNeeded)
									{
										contents.set(j, ItemStack.EMPTY);
										wallsNeeded -= count;
									}
									else
									{
										while(wallsNeeded != 0)
										{
											boxStack.decrement(1);
											wallsNeeded--;
										}
									}
								}
								else if(block == SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ)
								{
									if(count <= chiseledNeeded)
									{
										contents.set(j, ItemStack.EMPTY);
										chiseledNeeded -= count;
									}
									else
									{
										while(chiseledNeeded != 0)
										{
											boxStack.decrement(1);
											chiseledNeeded--;
										}
									}
								}
								else if(block == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
								{
									if(count <= pillarsNeeded)
									{
										contents.set(j, ItemStack.EMPTY);
										pillarsNeeded -= count;
									}
									else
									{
										while(pillarsNeeded != 0)
										{
											boxStack.decrement(1);
											pillarsNeeded--;
										}
									}
								}
							}

							Inventories.toTag(blockEntityTag, contents);
							stackTag.put("BlockEntityTag", blockEntityTag);
							stackToCheck.setTag(stackTag);
						} //shulker box end
						else if(block == SCContent.BLOCK_POCKET_WALL)
						{
							if(count <= wallsNeeded)
							{
								inventory.set(i - 1, ItemStack.EMPTY);
								wallsNeeded -= count;
							}
							else
							{
								while(wallsNeeded != 0)
								{
									stackToCheck.decrement(1);
									wallsNeeded--;
								}

								inventory.set(i - 1, stackToCheck);
							}
						}
						else if(block == SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ)
						{
							if(count <= chiseledNeeded)
							{
								inventory.set(i - 1, ItemStack.EMPTY);
								chiseledNeeded -= count;
							}
							else
							{
								while(chiseledNeeded != 0)
								{
									stackToCheck.decrement(1);
									chiseledNeeded--;
								}

								inventory.set(i - 1, stackToCheck);
							}
						}
						else if(block == SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
						{
							if(count <= pillarsNeeded)
							{
								inventory.set(i - 1, ItemStack.EMPTY);
								pillarsNeeded -= count;
							}
							else
							{
								while(pillarsNeeded != 0)
								{
									stackToCheck.decrement(1);
									pillarsNeeded--;
								}

								inventory.set(i - 1, stackToCheck);
							}
						}
					}
				}
			}

			pos = getPos().toImmutable().offset(right, -half);
			xi = lowest;
			yi = lowest;
			zi = lowest;

			while(yi < size) //Step 3: placing the blocks and giving them the right owner
			{
				while(zi < size)
				{
					while(xi < size)
					{
						//skip the blocks in the middle again
						if(xi > lowest && yi > lowest && zi > lowest && xi < highest && yi < highest && zi < highest)
						{
							xi++;
							continue;
						}

						BlockPos currentPos = pos.offset(right, xi);
						BlockState currentState = world.getBlockState(currentPos);

						if(currentState.getBlock() instanceof BlockPocketManagerBlock && !currentPos.equals(getPos()))
							return new TranslatableText("messages.securitycraft:blockpocket.multipleManagers");

						//placing the lowest and highest level of the cube
						if((yi == lowest && !currentPos.equals(getPos())) || yi == highest) //if (y level is lowest AND it's not the block pocket manager's position) OR (y level is highest)
						{
							//placing the corners
							if(((xi == lowest && zi == lowest) || (xi == lowest && zi == highest) || (xi == highest && zi == lowest) || (xi == highest && zi == highest)))
							{
								if(currentState.getBlock() != SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ)
									world.setBlockState(currentPos, SCContent.REINFORCED_CHISELED_CRYSTAL_QUARTZ.getDefaultState());
							}
							//placing the sides parallel to the block pocket manager
							else if((zi == lowest || zi == highest) && xi > lowest && xi < highest)
							{
								Axis typeToPlace = managerFacing == Direction.NORTH || managerFacing == Direction.SOUTH ? Axis.X : Axis.Z;

								if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
									world.setBlockState(currentPos, SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.getDefaultState().with(ReinforcedRotatedPillarBlock.AXIS, typeToPlace));
							}
							//placing the sides orthogonal to the block pocket manager
							else if((xi == lowest || xi == highest) && zi > lowest && zi < highest)
							{
								Axis typeToPlace = managerFacing == Direction.NORTH || managerFacing == Direction.SOUTH ? Axis.Z : Axis.X;

								if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
									world.setBlockState(currentPos, SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.getDefaultState().with(ReinforcedRotatedPillarBlock.AXIS, typeToPlace));
							}
							//placing the middle plane
							else if(xi > lowest && zi > lowest && xi < highest && zi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock))
									world.setBlockState(currentPos, SCContent.BLOCK_POCKET_WALL.getDefaultState());
							}
						}
						//placing the corner edges
						else if(yi != lowest && yi != highest && ((xi == lowest && zi == lowest) || (xi == lowest && zi == highest) || (xi == highest && zi == lowest) || (xi == highest && zi == highest)))
						{
							if(currentState.getBlock() != SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR)
								world.setBlockState(currentPos, SCContent.REINFORCED_CRYSTAL_QUARTZ_PILLAR.getDefaultState().with(ReinforcedRotatedPillarBlock.AXIS, Axis.Y));
						}
						//placing the walls
						else if(yi > lowest && yi < highest)
						{
							//checking the walls parallel to the block pocket manager
							if((zi == lowest || zi == highest) && xi > lowest && xi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock))
									world.setBlockState(currentPos, SCContent.BLOCK_POCKET_WALL.getDefaultState());
							}
							//checking the walls orthogonal to the block pocket manager
							else if((xi == lowest || xi == highest) && zi > lowest && zi < highest)
							{
								if(!(currentState.getBlock() instanceof BlockPocketWallBlock))
									world.setBlockState(currentPos, SCContent.BLOCK_POCKET_WALL.getDefaultState());
							}
						}

						//assigning the owner
						if(world.getBlockEntity(currentPos) instanceof OwnableTileEntity)
						{
							OwnableTileEntity te = (OwnableTileEntity)world.getBlockEntity(currentPos);

							te.getOwner().set(getOwner());
						}

						xi++;
					}

					xi = 0;
					zi++;
					pos = startingPos.up(yi).offset(back, zi);
				}

				zi = 0;
				yi++;
				pos = startingPos.up(yi);
			}

			return new TranslatableText("messages.securitycraft:blockpocket.assembled");
		}

		return null;
	}

	public void disableMultiblock()
	{
		if(enabled)
		{
			if(world.isClient)
			{
//				SecurityCraft.channel.sendToServer(new ToggleBlockPocketManager(this, false, size)); // TODO
//				PlayerUtils.sendMessageToPlayer(SecurityCraft.proxy.getClientPlayer(), ClientUtils.localize(SCContent.BLOCK_POCKET_MANAGER.getTranslationKey()), ClientUtils.localize("messages.securitycraft:blockpocket.deactivated"), Formatting.DARK_AQUA);
			}

			enabled = false;

			for(BlockPos pos : blocks)
			{
				BlockEntity te = world.getBlockEntity(pos);

				if(te instanceof BlockPocketTileEntity)
					((BlockPocketTileEntity)te).removeManager();
			}

			for(BlockPos pos : floor)
			{
				BlockState state = world.getBlockState(pos);

				if(state.contains(BlockPocketWallBlock.SOLID))
					world.setBlockState(pos, state.with(BlockPocketWallBlock.SOLID, false));
			}

			if(hasModule(ModuleType.DISGUISE))
				setWalls(true);

			blocks.clear();
			walls.clear();
			floor.clear();
		}
	}

	public void toggleOutline()
	{
		showOutline = !showOutline;
	}

	public void setWalls(boolean seeThrough)
	{
		for(BlockPos pos : walls)
		{
			BlockState state = world.getBlockState(pos);

			if(state.getBlock() instanceof BlockPocketWallBlock)
				world.setBlockState(pos, state.with(BlockPocketWallBlock.SEE_THROUGH, seeThrough));
		}
	}

	@Override
	public void onTileEntityDestroyed()
	{
		super.onTileEntityDestroyed();
		if (world.getBlockState(pos).getBlock() != SCContent.BLOCK_POCKET_MANAGER)
			disableMultiblock();
	}

	@Override
	public void onModuleInserted(ItemStack stack, ModuleType module)
	{
		super.onModuleInserted(stack, module);

		if(enabled && module == ModuleType.DISGUISE)
			setWalls(false);
	}

	@Override
	public void onModuleRemoved(ItemStack stack, ModuleType module)
	{
		super.onModuleRemoved(stack, module);

		if(enabled && module == ModuleType.DISGUISE)
			setWalls(true);
	}

	@Override
	public CompoundTag toTag(CompoundTag tag)
	{
		tag.putBoolean("BlockPocketEnabled", enabled);
		tag.putBoolean("ShowOutline", showOutline);
		tag.putInt("Size", size);

		for(int i = 0; i < blocks.size(); i++)
		{
			tag.putLong("BlocksList" + i, blocks.get(i).asLong());
		}

		for(int i = 0; i < walls.size(); i++)
		{
			tag.putLong("WallsList" + i, walls.get(i).asLong());
		}

		for(int i = 0; i < floor.size(); i++)
		{
			tag.putLong("FloorList" + i, floor.get(i).asLong());
		}

		return super.toTag(tag);
	}

	@Override
	public void fromTag(BlockState state, CompoundTag tag)
	{
		int i = 0;

		super.fromTag(state, tag);
		enabled = tag.getBoolean("BlockPocketEnabled");
		showOutline = tag.getBoolean("ShowOutline");
		size = tag.getInt("Size");

		while(tag.contains("BlocksList" + i))
		{
			blocks.add(BlockPos.fromLong(tag.getLong("BlocksList" + i)));
			i++;
		}

		i = 0;

		while(tag.contains("WallsList" + i))
		{
			walls.add(BlockPos.fromLong(tag.getLong("WallsList" + i)));
			i++;
		}

		i = 0;

		while(tag.contains("FloorList" + i))
		{
			floor.add(BlockPos.fromLong(tag.getLong("FloorList" + i)));
			i++;
		}
	}

	@Override
	public ModuleType[] acceptedModules()
	{
		return new ModuleType[] {
				ModuleType.DISGUISE,
				ModuleType.WHITELIST
		};
	}

	@Override
	public Option<?>[] customOptions()
	{
		return null;
	}

	@Override
	public ScreenHandler createMenu(int windowId, PlayerInventory inv, PlayerEntity player)
	{
		return new GenericTEContainer(SCContent.cTypeBlockPocketManager, windowId, world, pos);
	}

	@Override
	public Text getDisplayName()
	{
		return new TranslatableText(SCContent.BLOCK_POCKET_MANAGER.getTranslationKey());
	}

//	@Override // Forge method
//	public Box getRenderBoundingBox()
//	{
//		return new Box(getPos()).expand(RENDER_DISTANCE);
//	}
}
