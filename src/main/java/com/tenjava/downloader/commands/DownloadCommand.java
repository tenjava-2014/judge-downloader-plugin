package com.tenjava.downloader.commands;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.tenjava.downloader.Downloader;
import org.bukkit.command.CommandSender;

/**
 * @author MasterEjay
 */
public class DownloadCommand{

	@Command(aliases = {"download", "dl"}, desc = "Downloads a plugin from the CI server", usage = "<name> <time>", min=2, max=2)
	public static void download(CommandContext cmd, CommandSender sender) throws CommandException{
		Downloader.download(cmd.getString(0), cmd.getString(1), sender);
	}
}
