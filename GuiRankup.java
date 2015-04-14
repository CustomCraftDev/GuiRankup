package xyz.undeaD_D.pokemononline;
import java.util.Arrays;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GuiRankup extends JavaPlugin implements Listener{
	private String prefix;
	private FileConfiguration config;
	private boolean debug;
	private static Permission permission = null;
	private static Economy economy = null;
		
	
	public void onEnable() {
		config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		
		if(setupEconomy() && setupPermissions()){
			System.out.println(ChatColor.stripColor(prefix + "-----------------------------------------------"));
			System.out.println(ChatColor.stripColor(prefix + " Dependency [Vault] was not found."));
			System.out.println(ChatColor.stripColor(prefix + " Without Vault GuiRankup won't work."));
			System.out.println(ChatColor.stripColor(prefix + " Plugin will now disable itself."));
			System.out.println(ChatColor.stripColor(prefix + "-----------------------------------------------"));
			this.setEnabled(false);
			return;
		}
			
		prefix = ChatColor.translateAlternateColorCodes('&',config.getString("settings.prefix"));
		boolean b = config.getBoolean("settings.updater");
		if(b) {
			new Updater(this);
		}
		
		debug = config.getBoolean("settings.debug");
		if(debug) {
			System.out.println(ChatColor.stripColor(prefix + "-----------------------------------------------"));
			System.out.println(ChatColor.stripColor(prefix + " GuiRankup DEBUG-MODE was activated."));
			System.out.println(ChatColor.stripColor(prefix + " It is advisable to turn it back off ..."));
			System.out.println(ChatColor.stripColor(prefix + " this can be done in the config.yml"));
			System.out.println(ChatColor.stripColor(prefix + "-----------------------------------------------"));
		}
		
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	
	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
		if(cmd.toString().equalsIgnoreCase("guirank")) {
			if(args.length > 0) {
				if(sender instanceof Player) {
					Player p = (Player) sender;
					if(p.hasPermission("guirank.others")) {
						try {
							Player target = getServer().getPlayer(args[0]);
							if(target != null) {
								createInventory(target);
								return true;
							}else {
								p.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("msg.noplayer")));
								return true;
							}
						}catch(Exception ex) {}
					}else {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("msg.noperm")));
						return true;
					}
				}else {
					try {
						Player target = getServer().getPlayer(args[0]);
						if(target != null) {
							createInventory(target);
							return true;
						}else {
							System.out.println(ChatColor.stripColor(config.getString("msg.noplayer")));
							return true;
						}
					}catch(Exception ex) {}
				}
			}else {
				if(sender instanceof Player) {
					Player p = (Player) sender;
					if(p.hasPermission("guirank.use")) {
						createInventory(p);
						return true;
					}else {
						p.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("msg.noperm")));
						return true;
					}
				}else {
					System.out.println(ChatColor.stripColor(config.getString("msg.ingameonly")));
					return true;
				}
			}
		}
		
		if(cmd.toString().equalsIgnoreCase("guirank-reload")) {
			if(sender instanceof Player) {
				Player p = (Player) sender;
				if(p.hasPermission("guirank.reload")) {
					reloadConfig();
					config = getConfig();
					p.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("msg.reloaded")));
					return true;
				}else {
					p.sendMessage(ChatColor.translateAlternateColorCodes('&',config.getString("msg.noperm")));
					return true;
				}
			}else {
				reloadConfig();
				config = getConfig();
				System.out.println(ChatColor.stripColor(config.getString("msg.reloaded")));
				return true;
			}
		}
		
		return false;
	}

	
	// [Events]-----------------------------------------------------------------------------------------------------------------------------

	
	@EventHandler
    public void InventoryClickEvent(InventoryClickEvent e) {
		if(e.getInventory().getName().equalsIgnoreCase(config.getString("inventory.title"))) {
			if(e.getWhoClicked() instanceof Player) {
				check(((Player)e.getWhoClicked()), e.getCurrentItem());
			}
			e.setCancelled(true);
		}
    }
		 

	// Helper -------------------------------------------------------------------------------------------------------------------------------
	
	
	private void check(Player p, ItemStack item) {
		if(item.getItemMeta().getDisplayName().startsWith(config.getString("colors.bought"))) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("msg.bought")));
			return;
		}else if(item.getItemMeta().getDisplayName().startsWith(config.getString("colors.locked"))) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("msg.locked")));
			return;
		}else {
			int count = config.getInt("ranks.amount");
			for(int i = 0; i <= count; i++) {
				if(item.getItemMeta().getDisplayName().equalsIgnoreCase(config.getString("ranks." + i + ".title"))) {
					execute(p, config.getString("ranks." + i + ".group"), config.getInt("ranks." + i + ".price"),  config.getString("ranks." + i + ".msg"));
					break;
				}
			}
		}
	}
	
	
	private void execute(Player p, String group, int price, String msg) {
		EconomyResponse result = economy.withdrawPlayer(getServer().getOfflinePlayer(p.getUniqueId()), price);
		p.closeInventory();
		if(result.type.equals(ResponseType.SUCCESS)) {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
		}else {
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', config.getString("msg.money")));
		}
	}
	
	
	private void createInventory(Player p) {
		Inventory inv = getServer().createInventory(p, config.getInt("inventory.size"), ChatColor.translateAlternateColorCodes('&', config.getString("inventory.title")));
		int count = config.getInt("ranks.amount");
		boolean locked = false;
		
		for(int i = 0; i <= count; i++) {
			ItemStack item = new ItemStack(Material.valueOf(config.getString("ranks." + i + ".material")), 1);
			ItemMeta m = item.getItemMeta();
			if(!locked) {
				if(permission.playerInGroup(p, config.getString("ranks." + i + ".group"))) {
					m.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("colors.bought") + config.getString("ranks." + i + ".title")));
					m.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&',config.getString("lore.price") + config.getInt("ranks." + i + ".price")),ChatColor.translateAlternateColorCodes('&', config.getString("lore.bought"))));
				}else {
					m.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("colors.available") + config.getString("ranks." + i + ".title")));
					m.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&',config.getString("lore.price") + config.getInt("ranks." + i + ".price")),ChatColor.translateAlternateColorCodes('&', config.getString("lore.available"))));
					locked = true;
				}
			}else {
				item.setType(Material.valueOf(config.getString("ranks.locked-type")));
				m.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("colors.locked") + config.getString("ranks.locked-title")));
				m.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&',config.getString("lore.price") + config.getInt("ranks." + i + ".price")),ChatColor.translateAlternateColorCodes('&', config.getString("lore.locked"))));
			}
			item.setItemMeta(m);
			inv.setItem(i, item);
		}

		p.openInventory(inv);
		if(config.getBoolean("inventory.sound-toggle")) {
			p.playSound(p.getLocation(),Sound.valueOf(config.getString("inventory.sound-type")), 1F, 1F);
		}
	}


	private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
	
	
	private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
	
	
	// UPDATER ------------------------------------------------------------------------------------------------------------------------------
	
	
	protected void say(Player p, boolean console) {
		if(console) {
			System.out.println(ChatColor.stripColor(prefix  + "-----------------------------------------------------"));
			System.out.println(ChatColor.stripColor(prefix + " GuiRankup is outdated. Get the new version here:"));
			System.out.println(ChatColor.stripColor(prefix + " http://www.pokemon-online.xyz/plugin"));
			System.out.println(ChatColor.stripColor(prefix + "-----------------------------------------------------"));
		}else {
		   	p.sendMessage(prefix + "------------------------------------------------------");
		   	p.sendMessage(prefix + " GuiRankup  is outdated. Get the new version here:");
		   	p.sendMessage(prefix + " http://www.pokemon-online.xyz/plugin");
		   	p.sendMessage(prefix + "------------------------------------------------------");
		}
	}
	
	
}
