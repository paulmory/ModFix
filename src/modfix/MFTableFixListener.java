/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package modfix;

import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.Packets;
import com.comphenix.protocol.events.ConnectionSide;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

//CraftingTablesFix
public class MFTableFixListener implements Listener {

	private Main main;
	private ModFixConfig config;
	
	MFTableFixListener(Main main, ModFixConfig config) {
		this.main = main;
		this.config = config;
		initCloseInventoryFixListener();
	}
	
	
	private HashMap<Block, String> protectblocks = new HashMap<Block, String>();
	private HashMap<String, Block> backreference = new HashMap<String, Block>();
	
	//allow only one player to interact with table at a time
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void OnPlayerIneractTable(PlayerInteractEvent e)
	{
		if (!config.enableTablesFix) {return;}
		
		
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK)
		{
			Player pl = e.getPlayer();
			Block binteract = e.getClickedBlock();
			String checkid = getIDstring(binteract);
			if (config.IntTablesIDs.contains(checkid))
			{
				if (protectblocks.get(binteract) == null)
				{//Put block to list of protected blocks
					protectblocks.put(binteract, pl.getName());
					backreference.put(pl.getName(), binteract);
					return;
				}
				//If it's the same player let him open this (in case we lost something and block is still protected (this is really bad if this happened))
				if (pl.getName().equals(protectblocks.get(binteract))) {return;}

				//We reached here, well, sorry player, but you can't open this for now.
				pl.sendMessage(ChatColor.RED + "Вы не можете открыть этот стол, по крайней мере сейчас");
				e.setCancelled(true);
			}
		}
	}
	
	

	private void initCloseInventoryFixListener()
	{//remove block from hashmap on inventory close, just InventoryCloseEvent is not enough, not every mod fires it, so we will use packets.
		main.protocolManager.addPacketListener(
				  new PacketAdapter(main, ConnectionSide.CLIENT_SIDE, 
				  ListenerPriority.HIGHEST, Packets.Client.CLOSE_WINDOW) {
					@Override
				    public void onPacketReceiving(PacketEvent e) {
						String plname = e.getPlayer().getName();
					if (backreference.containsKey(plname))
						{//gotcha, you closed table inventory
						    protectblocks.remove(backreference.get(plname));
						    backreference.remove(plname);
						}
				    }
				});
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent e)
	{//Player can break opened block and then won't trigger inventory closing
		Block br = e.getBlock();
		if (protectblocks.containsKey(br))
		{
			backreference.remove(protectblocks.get(br));
			protectblocks.remove(br);
		}
	}
	
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerQuit(PlayerQuitEvent e)
	{//player can quit without closing table inventory, let's check it
		String plname = e.getPlayer().getName();
		if (backreference.containsKey(plname))
		{
		    protectblocks.remove(backreference.get(plname));
		    backreference.remove(plname);
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerKick(PlayerKickEvent e)
	{//player can be kicked without closing table inventory, let's check it
		String plname = e.getPlayer().getName();
		if (backreference.containsKey(plname))
		{
		    protectblocks.remove(backreference.get(plname));
		    backreference.remove(plname);
		}
	}
	
	
	private String getIDstring(Block bl)
	{
		String blstring = String.valueOf(bl.getTypeId());
		if (bl.getData() !=0) {blstring += ":"+bl.getData();}
		return blstring;
	}
}