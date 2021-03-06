package net.geforcemods.securitycraft.tileentity;

import net.geforcemods.securitycraft.ConfigHandler;
import net.geforcemods.securitycraft.SCContent;
import net.geforcemods.securitycraft.api.CustomizableTileEntity;
import net.geforcemods.securitycraft.api.Option;
import net.geforcemods.securitycraft.blocks.mines.IMSBlock;
import net.geforcemods.securitycraft.containers.GenericTEContainer;
import net.geforcemods.securitycraft.entity.IMSBombEntity;
import net.geforcemods.securitycraft.misc.ModuleType;
import net.geforcemods.securitycraft.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.Box;

import java.util.Iterator;
import java.util.List;

public class IMSTileEntity extends CustomizableTileEntity implements NamedScreenHandlerFactory {

	/** Number of bombs remaining in storage. **/
	private int bombsRemaining = 4;
	/** The targeting option currently selected for this IMS. PLAYERS = players, PLAYERS_AND_MOBS = hostile mobs & players, MOBS = hostile mobs.**/
	private IMSTargetingMode targetingOption = IMSTargetingMode.PLAYERS_AND_MOBS;
	private boolean updateBombCount = false;

	public IMSTileEntity()
	{
		super(SCContent.teTypeIms);
	}

	@Override
	public void tick(){
		super.tick();

		if(!world.isClient && updateBombCount){
			int mineCount = BlockUtils.getBlockProperty(world, pos, IMSBlock.MINES);

			if(!(mineCount - 1 < 0 || mineCount > 4))
				BlockUtils.setBlockProperty(world, pos, IMSBlock.MINES, BlockUtils.getBlockProperty(world, pos, IMSBlock.MINES) - 1);

			updateBombCount = false;
		}

		if(world.getTime() % 80L == 0L)
			launchMine();
	}

	/**
	 * Create a bounding box around the IMS, and fire a mine if a mob or player is found.
	 */
	private void launchMine() {
		boolean launchedMine = false;

		if(bombsRemaining > 0){
			double range = ConfigHandler.CONFIG.imsRange;

			Box area = BlockUtils.fromBounds(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1).expand(range, range, range);
			List<?> players = world.getEntitiesByClass(PlayerEntity.class, area, e -> !EntityUtils.isInvisible(e));
			List<?> mobs = world.getEntitiesByClass(HostileEntity.class, area, e -> !EntityUtils.isInvisible(e));
			Iterator<?> playerIterator = players.iterator();
			Iterator<?> mobIterator = mobs.iterator();

			// Targets players and mobs
			while(targetingOption == IMSTargetingMode.PLAYERS_AND_MOBS && mobIterator.hasNext()){
				LivingEntity entity = (LivingEntity) mobIterator.next();
				int launchHeight = getLaunchHeight();

				if(PlayerUtils.isPlayerMountedOnCamera(entity))
					continue;

				if(hasModule(ModuleType.WHITELIST) && ModuleUtils.getPlayersFromModule(world, pos, ModuleType.WHITELIST).contains(entity.getName().getString().toLowerCase()))
					continue;

				double targetX = entity.getX() - (pos.getX() + 0.5D);
				double targetY = entity.getBoundingBox().minY + entity.getHeight() / 2.0F - (pos.getY() + 1.25D);
				double targetZ = entity.getZ() - (pos.getZ() + 0.5D);

				this.spawnMine(entity, targetX, targetY, targetZ, launchHeight);

				if(!world.isClient)
					world.playSound((PlayerEntity) null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);

				bombsRemaining--;
				launchedMine = true;
				updateBombCount = true;
				break;
			}

			// Targets only hostile mobs
			while(!launchedMine && targetingOption == IMSTargetingMode.MOBS && mobIterator.hasNext()){
				HostileEntity entity = (HostileEntity) mobIterator.next();
				int launchHeight = getLaunchHeight();

				if(hasModule(ModuleType.WHITELIST) && ModuleUtils.getPlayersFromModule(world, pos, ModuleType.WHITELIST).contains(entity.getName().getString().toLowerCase()))
					continue;

				double targetX = entity.getX() - (pos.getX() + 0.5D);
				double targetY = entity.getBoundingBox().minY + entity.getHeight() / 2.0F - (pos.getY() + 1.25D);
				double targetZ = entity.getZ() - (pos.getZ() + 0.5D);

				this.spawnMine(entity, targetX, targetY, targetZ, launchHeight);

				if(!world.isClient)
					world.playSound((PlayerEntity) null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);

				bombsRemaining--;
				launchedMine = true;
				updateBombCount = true;
				break;
			}

			// Targets only other players
			while(!launchedMine && targetingOption == IMSTargetingMode.PLAYERS && playerIterator.hasNext()){
				PlayerEntity entity = (PlayerEntity) playerIterator.next();
				int launchHeight = getLaunchHeight();

				if((entity != null && getOwner().isOwner((entity))) || PlayerUtils.isPlayerMountedOnCamera(entity))
					continue;
				if(WorldUtils.isPathObstructed(entity, world, pos.getX() + 0.5D, pos.getY() + (((launchHeight - 1) / 3) + 0.5D), pos.getZ() + 0.5D, entity.getX(), entity.getY() + entity.getStandingEyeHeight(), entity.getZ()))
					continue;
				if(hasModule(ModuleType.WHITELIST) && ModuleUtils.getPlayersFromModule(world, pos, ModuleType.WHITELIST).contains(entity.getName().getString()))
					continue;

				double targetX = entity.getX() - (pos.getX() + 0.5D);
				double targetY = entity.getBoundingBox().minY + entity.getHeight() / 2.0F - (pos.getY() + 1.25D);
				double targetZ = entity.getZ() - (pos.getZ() + 0.5D);

				this.spawnMine(entity, targetX, targetY, targetZ, launchHeight);

				if(!world.isClient)
					world.playSound((PlayerEntity) null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.0F);

				bombsRemaining--;
				updateBombCount = true;
				break;
			}
		}
	}

	/**
	 * Spawn a mine at the correct position on the IMS model.
	 */
	private void spawnMine(PlayerEntity target, double x, double y, double z, int launchHeight){
		double addToX = bombsRemaining == 4 || bombsRemaining == 3 ? 1.2D : 0.55D;
		double addToZ = bombsRemaining == 4 || bombsRemaining == 2 ? 1.2D : 0.6D;

		world.spawnEntity(new IMSBombEntity(world, target, pos.getX() + addToX, pos.getY(), pos.getZ() + addToZ, x, y, z, launchHeight));
	}

	/**
	 * Spawn a mine at the correct position on the IMS model.
	 */
	private void spawnMine(LivingEntity target, double x, double y, double z, int launchHeight){
		double addToX = bombsRemaining == 4 || bombsRemaining == 3 ? 1.2D : 0.55D;
		double addToZ = bombsRemaining == 4 || bombsRemaining == 2 ? 1.2D : 0.6D;

		world.spawnEntity(new IMSBombEntity(world, target, pos.getX() + addToX, pos.getY(), pos.getZ() + addToZ, x, y, z, launchHeight));
	}

	/**
	 * Returns the amount of ticks the {@link IMSBombEntity} should float in the air before firing at an entity.
	 */
	private int getLaunchHeight() {
		int height;

		for(height = 1; height <= 9; height++)
		{
			BlockState state = getWorld().getBlockState(getPos().up(height));

			if(state == null || state.isAir())
				continue;
			else
				break;
		}

		return height * 3;
	}

	/**
	 * Writes a tile entity to NBT.
	 * @return
	 */
	@Override
	public CompoundTag toTag(CompoundTag tag){
		super.toTag(tag);

		tag.putInt("bombsRemaining", bombsRemaining);
		tag.putInt("targetingOption", targetingOption.modeIndex);
		tag.putBoolean("updateBombCount", updateBombCount);
		return tag;
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void fromTag(BlockState state, CompoundTag tag){
		super.fromTag(state, tag);

		if (tag.contains("bombsRemaining"))
			bombsRemaining = tag.getInt("bombsRemaining");

		if (tag.contains("targetingOption"))
			targetingOption = IMSTargetingMode.values()[tag.getInt("targetingOption")];

		if (tag.contains("updateBombCount"))
			updateBombCount = tag.getBoolean("updateBombCount");
	}

	public void setBombsRemaining(int bombsRemaining) {
		this.bombsRemaining = bombsRemaining;
	}

	public IMSTargetingMode getTargetingOption() {
		return targetingOption;
	}

	public void setTargetingOption(IMSTargetingMode targetingOption) {
		this.targetingOption = targetingOption;
	}

	@Override
	public ModuleType[] acceptedModules() {
		return new ModuleType[]{ModuleType.WHITELIST};
	}

	@Override
	public Option<?>[] customOptions() {
		return null;
	}

	@Override
	public ScreenHandler createMenu(int windowId, PlayerInventory inv, PlayerEntity player)
	{
		return new GenericTEContainer(SCContent.cTypeIMS, windowId, world, pos);
	}

	@Override
	public Text getDisplayName()
	{
		return new TranslatableText(SCContent.IMS.getTranslationKey());
	}

	public static enum IMSTargetingMode {

		PLAYERS(0),
		PLAYERS_AND_MOBS(1),
		MOBS(2);

		public final int modeIndex;

		private IMSTargetingMode(int index){
			modeIndex = index;
		}
	}
}
