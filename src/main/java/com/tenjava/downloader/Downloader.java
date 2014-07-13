package com.tenjava.downloader;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissionsException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.sk89q.minecraft.util.commands.CommandsManager;
import com.sk89q.minecraft.util.commands.MissingNestedCommandException;
import com.sk89q.minecraft.util.commands.WrappedCommandException;
import com.tenjava.downloader.commands.DownloadCommand;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

/**
 * @author MasterEjay
 */
public class Downloader extends JavaPlugin {

	public CommandsManager<CommandSender> commands;
	public static JenkinsServer jenkinsServer;

	@Override
	public void onEnable(){
		super.onEnable();
		setupCommands();
		try{
			jenkinsServer = new JenkinsServer(new URI("http://ci.tenjava.com/"));
		}catch(URISyntaxException e){
			Bukkit.getLogger().severe(e.getMessage());
			Bukkit.shutdown();
		}
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


	public static void download(String name, String time, CommandSender sender){
		Map<String,Job> jobs;
		Job selectedJob = null;
		try{
			jobs = jenkinsServer.getJobs();
			String jobName = name + "-" + time;

			for (Job job : jobs.values()){
				if (jobName.equalsIgnoreCase(job.getName())){
					selectedJob = job;
				}
			}

		}catch(IOException e){
			Bukkit.getLogger().severe(e.getMessage());
			Bukkit.shutdown();
		}

		if (selectedJob == null){
			sender.sendMessage(ChatColor.RED + "That job could not be found on the CI repo.");
			return;
		}

	   if (!Bukkit.getUpdateFolderFile().exists()){
		   Bukkit.getUpdateFolderFile().mkdir();
	   }
		 sender.sendMessage(ChatColor.GOLD + "Downloading....");
		try{
			FileUtils.copyURLToFile(new URL(selectedJob.details().getLastSuccessfulBuild().getUrl()), new File(Bukkit.getUpdateFolderFile(), selectedJob.getName() + ".jar"));
		}catch(IOException e){
			Bukkit.getLogger().severe(e.getMessage());
			Bukkit.shutdown();
		}
		sender.sendMessage(ChatColor.GREEN + "The file has been downloaded and placed in the update folder. Restart the server!");

	}
}
