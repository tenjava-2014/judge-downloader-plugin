package com.tenjava.downloader;

import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.tenjava.downloader.commands.DownloadCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author MasterEjay
 */
public class Downloader extends JavaPlugin {

	public CommandsManager<CommandSender> commands;


	@Override
	public void onEnable(){
		super.onEnable();
		setupCommands();
	}

	@Override
	public void onDisable(){
		super.onDisable();
	}

	public void setupCommands() {
		commands = new CommandsManager<CommandSender>() {
			@Override
			public boolean hasPermission(CommandSender sender, String permission) {
				return sender instanceof ConsoleCommandSender|| sender.hasPermission(permission);
			}
		};

		CommandsManagerRegistration cmdRegister = new CommandsManagerRegistration(this, commands);
		cmdRegister.register(DownloadCommand.class);
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		try {
			this.commands.execute(cmd.getName(), args, sender, sender);
		} catch (CommandPermissionsException ex) {
			sender.sendMessage(ChatColor.RED + "You don't have permission.");
		} catch (MissingNestedCommandException ex) {
			sender.sendMessage(ChatColor.RED + ex.getUsage());
		} catch (CommandUsageException ex) {
			sender.sendMessage(ChatColor.RED + ex.getMessage());
			sender.sendMessage(ChatColor.RED + ex.getUsage());
		} catch (WrappedCommandException ex) {
			if (ex.getCause() instanceof NumberFormatException) {
				sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
			} else {
				sender.sendMessage(ChatColor.RED + "An error has occurred running command " + ChatColor.DARK_RED + cmd.getName());
				ex.printStackTrace();
			}
		} catch (CommandException ex) {
			sender.sendMessage(ChatColor.RED + ex.getMessage());
		}

		return true;
	}
}
