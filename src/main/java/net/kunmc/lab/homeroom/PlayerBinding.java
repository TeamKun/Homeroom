package net.kunmc.lab.homeroom;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerBinding {
    private final Map<UUID, Long> uuidBinding = new HashMap<>();
    private final Map<String, Long> nameBinding = new HashMap<>();

    public boolean load(File file) {
        if (!file.exists() || !file.isFile())
            return false;

        uuidBinding.clear();
        nameBinding.clear();

        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String[] lineSplit = line.replace(" ", "").split(",");

                if (lineSplit.length >= 3) {
                    uuidBinding.put(UUID.fromString(lineSplit[2]), Long.parseLong(lineSplit[0]));
                    nameBinding.put(lineSplit[1], Long.parseLong(lineSplit[0]));
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            Homeroom.LOGGER.log(Level.SEVERE, "Failed to load binding file", e);
            return false;
        }

        return true;
    }

    public Long getDiscordId(UUID uuid, String name) {
        Long id = uuidBinding.get(uuid);
        if (id == null)
            id = nameBinding.get(name);
        return id;
    }
}
