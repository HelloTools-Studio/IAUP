package io.iaup;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;


public final class ItemsAdderUploadPlus extends JavaPlugin {
    private FileConfiguration YamlConfig;
    private FileConfiguration IAConfig;


    @Override
    public void onEnable() {
        // Plugin startup logic
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            boolean isMkdirSuccess = configFile.getParentFile().mkdirs();
            if (!isMkdirSuccess) Bukkit.getConsoleSender().sendMessage("An error occurred unexpectedly. "
                    + "Failed to create plugin configuration!");
            saveResource("config.yml", false);
        }
        YamlConfig = new YamlConfiguration();
        try {
            YamlConfig.load(configFile);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("An error occurred unexpectedly. " + "Failed to read config!");
            e.printStackTrace();
        }
        if (!YamlConfig.contains("global.uid")) {
            String newuid = UUID.randomUUID().toString();
            YamlConfig.set("global.uid", newuid);
            try {
                YamlConfig.save(configFile);
            } catch (IOException e) {
                Bukkit.getConsoleSender().sendMessage("An error occurred unexpectedly. " + "Failed to save config!");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            File configFile = new File(getDataFolder(), "config.yml");
            try {
                YamlConfig.load(configFile);
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("An error occurred unexpectedly. " + "Failed to read config!");
                e.printStackTrace();
            }
            sender.sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.reload")));
        }
        File file = new File("plugins/ItemsAdder/output/generated.zip");
        if (file.exists()) {
            uploadFileAsync(file, sender);
        } else {
            Bukkit.getConsoleSender().sendMessage("An error occurred unexpectedly. " + "Failed to read generated.zip!");
            sender.sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.failed-read")));
        }
        return true;
    }

    private void uploadFileAsync(File file, CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> uploadFile(file, sender));
    }

    public void uploadFile(File file, CommandSender sender) {
        String uid = YamlConfig.getString("global.uid");
        String url = YamlConfig.getString("global.url");
        String newFileName = uid + "_" + UUID.randomUUID().toString().substring(0, 5) + ".zip";

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost uploadFile = new HttpPost(url);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", file, ContentType.DEFAULT_BINARY, newFileName);
        HttpEntity multipart = builder.build();

        sender.sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.ready-upload")));
        uploadFile.setEntity(multipart);

        try (CloseableHttpResponse response = httpClient.execute(uploadFile)) {
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity);

            JSONObject jsonResponse = new JSONObject(responseString);
            if (jsonResponse.getString("status").equals("success")) {
                String downloadUrl = jsonResponse.getString(Objects.requireNonNull(
                        YamlConfig.getString("global.download_key")));
                modifyConfigFile(downloadUrl);
                sender.sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.success-upload")));
            } else {
                sender.sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.failed-upload-player")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.failed-upload-console")));
        }
    }

    public void modifyConfigFile(String newUrl) {
        IAConfig = new YamlConfiguration();
        File IAconfigFile = new File(Objects.requireNonNull(getServer().getPluginManager().getPlugin("ItemsAdder"))
                .getDataFolder(), "config.yml");
        IAConfig = new YamlConfiguration();
        try {
            IAConfig.load(IAconfigFile);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.error"))
                    + Objects.requireNonNull(YamlConfig.getString("locale.failed-read")));
            e.printStackTrace();
        }
        try {
            IAConfig.set("resource-pack.hosting.external-host.url", newUrl);
            IAConfig.save(IAconfigFile);
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(Objects.requireNonNull(YamlConfig.getString("locale.error"))
                    + Objects.requireNonNull(YamlConfig.getString("locale.failed-save-console")));
            e.printStackTrace();
        }
    }
}
