package net.geforcemods.securitycraft.api;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.text.Text;

/**
 * Implementing this interface in your {@link BlockEntity} allows
 * it to store a custom name that gets set whenever someone right-
 * clicks the Block it's in with a renamed name tag. You can then
 * use the name in things like chat messages or GUIs.
 *
 * @author Geforce
 */
public interface INameable {

	/**
	 * @return The TileEntity's custom name.
	 */
	public Text getCustomSCName();

	/**
	 * Set the TileEntity's new name. It might be best
	 * to sync the new name to the client-side after setting it here
	 * if you're going to be using it in chat messages or GUIs.
	 *
	 * @param customName The new name
	 */
	public void setCustomSCName(Text customName);

	/**
	 * @return Does this TileEntity currently have a custom name?
	 */
	public boolean hasCustomSCName();

	/**
	 * @return Can this TileEntity be renamed?
	 */
	public boolean canBeNamed();

}
