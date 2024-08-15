package com.zanaddon.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

import com.zanaddon.util.SerializationUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StoreBlocksInChunkSpell extends TargetedSpell implements TargetedLocationSpell {
    public StoreBlocksInChunkSpell(MagicConfig config, String spellName) {
        super(config, spellName);
    }

    @Override
    public CastResult cast(SpellData data) {
        if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        RayTraceResult result = rayTraceBlocks(data);
        if (result == null) return noTarget(data);

        Block block = result.getHitBlock();
        if (block == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

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

        if (locationList.contains(block.getLocation())) {
            return new CastResult(PostCastAction.ALREADY_HANDLED, data);
        }
        locationList.add(block.getLocation());

        byte[] newByteArray = SerializationUtil.serializeLocationList(locationList);
        pdc.set(key, PersistentDataType.BYTE_ARRAY, newByteArray);

        caster.sendMessage(locationList.toString());

        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }
}
