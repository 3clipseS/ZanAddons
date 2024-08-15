package com.zanaddon.util;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SerializationUtil {

    public static byte[] serializeLocationList(List<Location> locations) {
        StringBuilder combinedString = new StringBuilder();
        for (Location location : locations) {
            combinedString.append(location.getWorld().getName())
                    .append(",")
                    .append(location.getX())
                    .append(",")
                    .append(location.getY())
                    .append(",")
                    .append(location.getZ())
                    .append(";");
        }

        return combinedString.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static List<Location> deserializeByteArray (byte[] byteArray) {
        String combinedString = new String(byteArray, StandardCharsets.UTF_8);

        String[] locationListStrings = combinedString.split(";");
        List<Location> locations = new ArrayList<>();

        for (String str : locationListStrings) {
            try {
                String[] parts = str.split(",");
                if (parts.length != 4) continue;

                World world = Bukkit.getWorld(parts[0]);
                if (world == null) continue;

                double x = Double.parseDouble(parts[1]);
                double y = Double.parseDouble(parts[2]);
                double z = Double.parseDouble(parts[3]);

                locations.add(new Location(world,x,y,z));
            } catch (Exception e) {
                MagicSpells.error("Could not deserialize location");
            }
        }

        return locations;
    }
}
