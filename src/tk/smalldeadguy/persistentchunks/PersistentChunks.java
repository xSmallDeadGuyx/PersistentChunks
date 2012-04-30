package tk.smalldeadguy.persistentchunks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class PersistentChunks extends JavaPlugin implements Listener {

	public Logger log;
	public Set<Chunk> saveChunks = new HashSet<Chunk>();

	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent cue) {
		if(saveChunks.contains(cue.getChunk()))
			cue.setCancelled(true);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String name = command.getName();
		if(name.toLowerCase().startsWith("pc") && name.length() > 2) {
			String[] temp = args.clone();
			args = new String[temp.length + 1];
			args[0] = name.substring(2);
			name = "pc";
			for(int i = 0; i < temp.length; i++)
				args[i + 1] = temp[i];
		}
		if(name.equalsIgnoreCase("pc")) {
			if(args.length == 1 && args[0].equalsIgnoreCase("status")) {
				String chunks = "";
				for(Chunk c : saveChunks)
					chunks += " (" + c.getX() + ", " + c.getZ() + ")";
				sender.sendMessage(saveChunks.isEmpty() ? "No persisting chunks" : saveChunks.size() + " chunks persisting." + chunks);
				
				return true;
			}
			if(args.length == 1 && args[0].equalsIgnoreCase("current")) {
				if(sender instanceof Player) {
					Player p = (Player) sender;
					Chunk c = p.getLocation().getChunk();
					p.sendMessage("Chunk (" + c.getX() + ", " + c.getZ() + ") is " + (saveChunks.contains(c) ? "" : "not ") + "begin persisted");
				}
				else
					sender.sendMessage("You can only check the status of a select chunk in-game");
				return true;
			}
			if(args.length == 1 && args[0].equalsIgnoreCase("add")) {
				if(sender instanceof Player) {
					Player p = (Player) sender;
					Chunk c = p.getLocation().getChunk();
					if(saveChunks.contains(c))
						p.sendMessage("Current chunk is already being persisted");
					else {
						p.sendMessage("Chunk (" + c.getX() + ", " + c.getZ() + ") is now being persisted");
						saveChunks.add(c);
					}
				}
				else
					sender.sendMessage("You can only persist chunks in-game");

				return true;
			}
			if(args.length > 0 && args[0].equalsIgnoreCase("remove")) {
				if(args.length == 2 && args[1].equalsIgnoreCase("all")) {
					sender.sendMessage("No longer persisting any chunks");
					saveChunks.clear();
					return true;
				}
				else if(args.length == 1) {
					if(sender instanceof Player) {
						Player p = (Player) sender;
						Chunk c = p.getLocation().getChunk();
						if(saveChunks.contains(c)) {
							p.sendMessage("Chunk (" + c.getX() + ", " + c.getZ() + ") is no longer being persisted");
							saveChunks.remove(c);
						}
						else
							p.sendMessage("Current chunk is not being persisted");
					}
					else
						sender.sendMessage("You can only remove select chunks in-game");

					return true;
				}
			}
		}
		return false;
	}

	public void onEnable(){
		getServer().getPluginManager().registerEvents(this, this);

		log = getLogger();
		log.info("Your plugin has been enabled!");

	}

	public void onDisable(){
		log.info("Your plugin has been disabled.");
	}
}
