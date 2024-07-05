package com.zanaddon.targeted;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemEditSpell extends TargetedSpell implements TargetedEntitySpell {

    private final List<String> itemNames;
    private final Set<MagicItemData> items = new HashSet<>();
    private final ConfigData<Precision> precision;
    private final List<Integer> slots;

    private final ConfigData<Component> resItemName;
    private final List<String> resItemLore;
    private final ConfigData<Integer> resItemModelData;
    private final ConfigData<Boolean> resItemUnbreakable;

    public ItemEditSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        itemNames = getConfigStringList("items", null);
        precision = getConfigDataEnum("precision", Precision.class, Precision.HAND);
        slots = getConfigIntList("slots", null);
        resItemName = getConfigDataComponent("item-name",null);
        resItemLore = getConfigStringList("item-lore", null);
        resItemModelData = getConfigDataInt("custom-model-data", null);
        resItemUnbreakable = getConfigDataBoolean("unbreakable", null);
    }

    @Override
    public void initialize() {
        super.initialize();

        if (itemNames == null) return;

        for (String itemString : itemNames) {
            MagicItemData itemData = MagicItems.getMagicItemDataFromString(itemString);
            if (itemData == null) {
                MagicSpells.error("ItemEditSpell '" + internalName + "' has an invalid magic item specified: " + itemString);
                continue;
            }

            items.add(itemData);
        }
    }

    @Override
    public CastResult cast(SpellData data) {
        TargetInfo<LivingEntity> info = getTargetedEntity(data);
        if (info.noTarget()) return noTarget(info);

        return castAtEntity(info.spellData());
    }

    public CastResult castAtEntity(SpellData data) {
        if(!(data.target() instanceof Player target)) return noTarget(data);

        if (items.isEmpty()) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        for (ItemStack item : getInventoryEditItems(precision.get(data), target.getInventory())) {
            setItemMeta(data, item.getItemMeta(), item);
        }


        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }

    private List<ItemStack> getInventoryEditItems(Precision precision, PlayerInventory inventory) {
        List<ItemStack> editItems = new ArrayList<>();
        if (slots != null) {
            for (int i : slots) {
                if (inventory.getItem(i) == null) continue;
                ItemStack slotItem = inventory.getItem(i);
                for (MagicItemData data : items) {
                    if (data.matches(MagicItems.getMagicItemDataFromItemStack(slotItem))) editItems.add(slotItem);
                }
            }
            return editItems;
        }
        else {
            for (ItemStack item : inventory.getContents()) {
                if (item == null) continue;
                for (MagicItemData data : items) {
                    if (data.matches(MagicItems.getMagicItemDataFromItemStack(item))) editItems.add(item);
                }
            }

            switch (precision) {
                case ALL -> {
                    return editItems;
                }
                case FIRST -> {
                    return List.of(editItems.getFirst());
                }
                case LAST -> {
                    return List.of(editItems.getLast());
                }
                default -> {
                    return List.of(inventory.getItemInMainHand());
                }
            }
        }
    }

    private void setItemMeta(SpellData data, ItemMeta itemMeta, ItemStack item) {
        if (resItemName != null) itemMeta.displayName(resItemName.get(data));
        if (resItemLore != null) {
            MagicSpells.error("HEllo world");
            List<Component> resLoreComponents = new ArrayList<>();
            for (String s : resItemLore) {
                resLoreComponents.add(ConfigDataUtil.getComponent(s).get(data));
            }
            itemMeta.lore(resLoreComponents);
        }
        if (resItemModelData != null) itemMeta.setCustomModelData(resItemModelData.get(data));
        if (resItemUnbreakable != null) itemMeta.setUnbreakable(resItemUnbreakable.get(data));

        item.setItemMeta(itemMeta);
    }

    private enum Precision {
        FIRST,
        LAST,
        ALL,
        HAND
    }
}
