package com.zanaddon.conditions;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.zanaddon.util.SerializationUtil;
import jdk.jfr.Name;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Name("blockinchunk")
public class BlockInChunkCondition extends Condition implements Listener {

    @Override
    public boolean initialize(@NotNull String var) {
        return true;
    }

    @Override
    public boolean check(LivingEntity caster) {
        caster.sendMessage("test");
        return lookingAt(caster);
    }

    @Override
    public boolean check(LivingEntity caster, LivingEntity target) {
        return lookingAt(target);
    }

    @Override
    public boolean check(LivingEntity livingEntity, Location location) {
        return false;
    }

    private boolean lookingAt(LivingEntity target) {
        Set<Material> transparent = MagicSpells.getTransparentBlocks();
        Location location = target.getEyeLocation();

        RayTraceResult result = location.getWorld().rayTraceBlocks(location, location.getDirection(), 4);
        if (result == null) return false;

        Block block = result.getHitBlock();
        if (block == null) return false;

        NamespacedKey key = new NamespacedKey("magicspells", "placedblocks");
        PersistentDataContainer pdc = block.getChunk().getPersistentDataContainer();

        List<Location> locationList;
        if (pdc.has(key, PersistentDataType.BYTE_ARRAY)) {
            byte[] locationByteArray = pdc.get(key, PersistentDataType.BYTE_ARRAY);
            locationList = SerializationUtil.deserializeByteArray(locationByteArray);
        }
        else {
            locationList = new ArrayList<>();
        }

        return locationList.contains(block.getLocation());
    }
}
