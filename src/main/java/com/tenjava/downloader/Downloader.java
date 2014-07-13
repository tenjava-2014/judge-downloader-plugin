package com.tenjava.downloader;


import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.Job;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author MasterEjay
 */
public class Downloader extends JavaPlugin {

    private JenkinsServer jenkins;

    @Override
    public void onEnable() {
        try {
            jenkins = new JenkinsServer(new URI("http://ci.tenjava.com"));
        } catch (URISyntaxException e) {
            e.printStackTrace(); // Someone should see this...
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String entry;
        if (args.length < 1) {
            return false;
        } else {
            entry = args[0];
        }

        // Permission checks are done by Bukkit
        if (command.getName().equals("download")) {
            sender.sendMessage(ChatColor.GRAY + "Downloading. Server may lag for a moment.");
            attemptDownload(entry, sender);
            return true;
        }

        return false;
    }

    private void attemptDownload(String entry, CommandSender sender) {
        try {
            // First get the job
            Job job = jenkins.getJob(entry);
            if (job == null) {
                sender.sendMessage(ChatColor.RED + "Looks like that entry doesn't exist.");
                return;
            }

            // Now check for the last successful build
            Build build;
            try {
                build = job.details().getLastSuccessfulBuild();
                if (build == null) {
                    sender.sendMessage(ChatColor.RED + "That entry has never compiled (not downloaded).");
                    return;
                }
            } catch (NullPointerException e) {
                // The only job which causes this is lol768-t1 for whatever reason
                sender.sendMessage(ChatColor.RED + "This entry is corrupted.");
                return;
            }

            // Quick check to see if this plugin has failed it's last commit
            Build lastBuild = job.details().getLastBuild();
            if (lastBuild.getNumber() != build.getNumber()) {
                sender.sendMessage(ChatColor.YELLOW + "Warning: The last successful build is NOT the latest!");
            }

            // Because people renamed their jars to funny names we need to parse the HTML for the
            // real artifact ID then download that instead.
            String html = getPageSource(build.getUrl());
            Document document = Jsoup.parse(html);

            // Find the artifact ID
            String artifactString = "/";
            for (Element element : document.getAllElements()) {
                if (element.hasAttr("href") && element.attr("href").startsWith("artifact/target")) {
                    artifactString += element.attr("href");
                    break;
                }
            }

            if (artifactString.length() == 1) {
                sender.sendMessage(ChatColor.RED + "Failed to locate artifact");
                return;
            }

            File downloadFile = new File(getDataFolder().getParentFile(), "__temp.jar");
            File pluginFile = new File(getDataFolder().getParentFile(), entry + ".jar");

            if (downloadFile.exists()) downloadFile.delete();

            // Now for the actual download (w00t. nio ftw)
            URL website = new URL(build.getUrl() + artifactString);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(downloadFile);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            rbc.close();
            fos.close();

            // Now we need to fix the name so that we can disable it later if needed. This is done by
            // unpacking the __temp.jar file into a stream and re-packing it as the entry.jar file. We
            // do a search for the plugin.yml and then load it's contents into Bukkit to do some magic
            // with it.
            unzipAndFixPlugin(downloadFile, pluginFile, entry);
            downloadFile.delete();

            // Now tell them to start 'er up
            sender.sendMessage(ChatColor.GREEN + entry + " has been downloaded to your server's plugin folder.");
            sender.sendMessage(ChatColor.GRAY + "Type /reload to enable it. Don't forget about previous plugins!");
        } catch (IOException e) {
            e.printStackTrace();
            sender.sendMessage(ChatColor.RED + "Something went wrong with the download. See the console for more information.");
        }
    }

    private String getPageSource(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

        String inputLine;
        StringBuilder a = new StringBuilder();

        while ((inputLine = in.readLine()) != null)
            a.append(inputLine);
        in.close();

        return a.toString();
    }

    private void unzipAndFixPlugin(File source, File destination, String name) throws IOException {
        ZipFile zipFile = new ZipFile(source);
        final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(destination));
        for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
            ZipEntry entryIn = (ZipEntry) e.nextElement();
            if (!entryIn.getName().equalsIgnoreCase("plugin.yml")) {
                zos.putNextEntry(entryIn);
                InputStream is = zipFile.getInputStream(entryIn);
                byte[] buf = new byte[1024];
                int len;
                while ((len = (is.read(buf))) > 0) {
                    zos.write(buf, 0, len);
                }
            } else {
                zos.putNextEntry(new ZipEntry("plugin.yml"));

                InputStream is = zipFile.getInputStream(entryIn);
                YamlConfiguration config = YamlConfiguration.loadConfiguration(is);
                config.set("name", name);
                String newYaml = config.saveToString();
                byte[] asBytes = newYaml.getBytes();

                zos.write(asBytes);
            }
            zos.closeEntry();
        }
        zos.close();
    }

}
