package tk.smalldeadguy.persistentchunks;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class PersistentChunks extends JavaPlugin implements Listener {

	public Logger log;
	private Set<Chunk> saveChunks = new HashSet<>();

	public void doSaveConfig() {
		FileConfiguration fc = getConfig();
		
		fc.set("savedchunks", null);
		ConfigurationSection savedChunks = fc.createSection("savedchunks");
		
		Map<String, List<String>> worlds = new HashMap<String, List<String>>();
		for(Chunk ch : saveChunks) {
			String world = ch.getWorld().getName();
			int x = ch.getX();
			int z = ch.getZ();
			if(worlds.containsKey(world))
				worlds.get(world).add("(" + x + "," + z + ")");
			else {
				List<String> chunks = new ArrayList<String>();
				chunks.add("(" + x + "," + z + ")");
				worlds.put(world, chunks);
			}
		}
		for(Entry<String, List<String>> kv : worlds.entrySet()) {
			savedChunks.set(kv.getKey(), kv.getValue());
		}
		saveConfig();
	}

	public void doLoadConfig() {
		FileConfiguration fc = getConfig();
		
		ConfigurationSection savedChunks = fc.getConfigurationSection("savedchunks");
		Set<String> worlds = savedChunks.getKeys(true);

		if(worlds != null) {
			for(String world : worlds) {
				World w = getServer().getWorld(world);
				List<String> chunks = savedChunks.getStringList(world);

				for(String c : chunks) {
					String[] vals = c.trim().substring(1, c.length() - 1).split(",");
					int x = Integer.parseInt(vals[0]);
					int y = Integer.parseInt(vals[1]);
					Chunk ch = w.getChunkAt(x, y);
					ch.load();
					saveChunks.add(ch);
				}
			}
		}
	}

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
						doSaveConfig();
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
							doSaveConfig();
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

	@Override
	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);

		log = getLogger();
		log.info("Your plugin has been enabled!");
		doLoadConfig();
	}

	@Override
	public void onDisable() {
		log.info("Your plugin has been disabled.");
	}
}
