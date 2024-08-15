package com.zanaddon.util;

import com.google.common.io.ByteStreams;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {
    public static byte[] serializeItemStackList(List<ItemStack> items) {
        ByteArrayDataOutput stream = ByteStreams.newDataOutput();

        ItemStack[] itemArray = items.toArray(new ItemStack[0]);

        for (ItemStack item : itemArray) {
            byte[] data = item == null || item.getType() == Material.AIR ? new byte[]{} : item.serializeAsBytes();

            stream.writeInt(data.length);
            stream.write(data);
        }

        stream.writeInt(-1);
        return stream.toByteArray();
    }

    public static ItemStack[] deserializeItemStackByteArray(byte[] bytes) {
        List<ItemStack> items = new ArrayList<>();
        ByteArrayDataInput inputStream = ByteStreams.newDataInput(bytes);
        while (true) {
            int length = inputStream.readInt();
            if (length == -1) break;
            if (length == 0) {
                items.add(null);
            } else {
                byte[] data = new byte[length];
                inputStream.readFully(data);
                items.add(ItemStack.deserializeBytes(data));
            }
        }

        return items.toArray(new ItemStack[0]);
    }
}
