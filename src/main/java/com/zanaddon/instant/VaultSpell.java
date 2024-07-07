package com.zanaddon.instant;

import com.google.common.io.ByteStreams;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.CastResult;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.command.TomeSpell;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.spells.command.ScrollSpell;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryCloseEvent;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.*;
import java.util.List;

public class VaultSpell extends InstantSpell {

    private File dataFolder;
    private final ConfigData<VaultSize> vaultSize;
    private final ConfigData<Component> title;
    private List<String> overrideItemList;

    private ItemStack[] itemTypes;

    private int[] itemSlots;

    private double[] itemChances;

    private int[] itemMinQuantities;
    private int[] itemMaxQuantities;

    public VaultSpell(MagicConfig config, String spellName) {
        super(config, spellName);

        vaultSize = getConfigDataEnum("vault-size", VaultSize.class, null);
        title = getConfigDataComponent("title", Component.text("Window Title " + spellName));

        overrideItemList = getConfigStringList("override-items", null);
    }

    @Override
    protected void initialize() {
        super.initialize();
        // Setup data folder
        dataFolder = new File(MagicSpells.getInstance().getDataFolder(), "vaults");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        if (overrideItemList != null && !overrideItemList.isEmpty()) {
            itemTypes = new ItemStack[overrideItemList.size()];
            itemSlots = new int[overrideItemList.size()];
            itemMinQuantities = new int[overrideItemList.size()];
            itemMaxQuantities = new int[overrideItemList.size()];
            itemChances = new double[overrideItemList.size()];

            int itemSlot = -1;

            // Base MS code snippet to read item list configs. Copied to retain consistency
            for (int i = 0; i < overrideItemList.size(); i++) {
                try {
                    String str = overrideItemList.get(i);

                    int brackets = 0;
                    int closedBrackets = 0;
                    for (int j = 0; j < str.length(); j++) {
                        char ch = str.charAt(j);
                        if (ch == '{') brackets++;
                        if (ch == '}') closedBrackets++;
                    }

                    if (brackets != closedBrackets) {
                        MagicSpells.error("VaultSpell '" + internalName + "' has an invalid item defined (e1): " + str);
                        continue;
                    }

                    brackets = 0;
                    closedBrackets = 0;

                    String[] data = str.split(" ");
                    String[] vaultData = null;

                    StringBuilder itemData = new StringBuilder();

                    for (int j = 0; j < data.length; j++) {
                        for (char ch : data[j].toCharArray()) {
                            if (ch == '{') brackets++;
                            if (ch == '}') closedBrackets++;
                        }

                        itemData.append(data[j]).append(" ");
                        // magicItemData is ready, add the conjureData
                        if (brackets == closedBrackets) {
                            int dataLeft = data.length - j - 1;
                            vaultData = new String[dataLeft];

                            // fill the conjureData array with stuff like amount and chance
                            for (int d = 0; d < dataLeft; d++) {
                                vaultData[d] = data[j + d + 1];
                            }
                            break;
                        }
                    }

                    String strItemData = itemData.toString().trim();

                    if (strItemData.startsWith("TOME:")) {
                        String[] tomeData = strItemData.split(":");
                        TomeSpell tomeSpell = (TomeSpell) MagicSpells.getSpellByInternalName(tomeData[1]);
                        Spell spell = MagicSpells.getSpellByInternalName(tomeData[2]);
                        int uses = tomeData.length > 3 ? Integer.parseInt(tomeData[3].trim()) : -1;
                        itemTypes[i] = tomeSpell.createTome(spell, uses, null, SpellData.NULL);
                    } else if (strItemData.startsWith("SCROLL:")) {
                        String[] scrollData = strItemData.split(":");
                        ScrollSpell scrollSpell = (ScrollSpell) MagicSpells.getSpellByInternalName(scrollData[1]);
                        Spell spell = MagicSpells.getSpellByInternalName(scrollData[2]);
                        int uses = scrollData.length > 3 ? Integer.parseInt(scrollData[3].trim()) : -1;
                        itemTypes[i] = scrollSpell.createScroll(spell, uses, null);
                    } else {
                        MagicItem magicItem = MagicItems.getMagicItemFromString(strItemData);
                        if (magicItem == null) continue;
                        itemTypes[i] = magicItem.getItemStack();
                    }

                    int minAmount = 1;
                    int maxAmount = 1;

                    double chance = 100;

                    // add default values if there aren't any specified
                    if (vaultData == null) {
                        itemSlots[i] = itemSlot++;
                        itemMinQuantities[i] = minAmount;
                        itemMaxQuantities[i] = maxAmount;
                        itemChances[i] = chance;
                        continue;
                    }

                    if (vaultData.length >= 1) {
                        itemSlot = Integer.parseInt(vaultData[0].trim());
                    }

                    if (vaultData.length >= 2) {
                        String[] amount = vaultData[1].split("-");
                        if (amount.length == 1) {
                            minAmount = Integer.parseInt(amount[0].trim());
                            maxAmount = minAmount;
                        } else if (amount.length >= 2) {
                            minAmount = Integer.parseInt(amount[0].trim());
                            maxAmount = Integer.parseInt(amount[1].trim()) + 1;
                        }
                    }

                    // parse chance
                    if (vaultData.length >= 3) {
                        chance = Double.parseDouble(vaultData[2].replace("%", "").trim());
                    }

                    itemMinQuantities[i] = minAmount;
                    itemMaxQuantities[i] = maxAmount;
                    itemChances[i] = chance;
                    itemSlots[i] = itemSlot;

                } catch (Exception e) {
                    MagicSpells.error("VaultSpell '" + internalName + "' has specified invalid item (e2): " + overrideItemList.get(i));
                    itemTypes[i] = null;
                }
            }
        }
    }

    @Override
    public CastResult cast(SpellData data) {
        if (!(data.caster() instanceof Player caster)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        File playerVaultFolder = new File(dataFolder, String.valueOf(caster.getUniqueId()));

        VaultSize vaultSize = this.vaultSize.get(data);
        if (vaultSize == null) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

        VaultInventory playerVault = new VaultInventory(internalName, vaultSize.getSize(), title.get(data));

        if (!(playerVaultFolder.exists()))
            playerVaultFolder.mkdirs();

        File playerVaultFile = new File(playerVaultFolder, internalName + ".bin");
        playerVault.setPlayerVaultFile(playerVaultFile);

        if (overrideItemList != null && !overrideItemList.isEmpty()) {
            HashMap<ItemStack, Integer> vaultItemMap = createOverrideItemsMap(playerVault.size, data);

            for(Map.Entry<ItemStack, Integer> entry : vaultItemMap.entrySet()) {
                playerVault.getInventory().setItem(entry.getValue(), entry.getKey());
            }

            caster.openInventory(playerVault.getInventory());
            return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
        }

        if (!(playerVaultFile.exists())) {
            caster.openInventory(playerVault.getInventory());
            try {
                Files.createFile(playerVaultFile.toPath());
            } catch (IOException e) {
                MagicSpells.error("Vault Spell " + internalName + " encountered an error saving player vault file");
                throw new RuntimeException(e);
            }

            return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
        }

        try {

            ItemStack[] vaultItems = deserializeItemStackByteArray(Files.readAllBytes(playerVaultFile.toPath()));
            if (vaultItems.length != 0) {
                if (vaultItems.length > playerVault.getInventory().getSize()) {
                    vaultItems = Arrays.copyOf(vaultItems, playerVault.getInventory().getSize());
                }

                playerVault.getInventory().setContents(vaultItems);
            }
        } catch (IOException e) {
            MagicSpells.error("Vault Spell " + internalName + " encountered an error reading saved items");
            throw new RuntimeException(e);
        }

        caster.openInventory(playerVault.getInventory());

        playSpellEffects(EffectPosition.CASTER, data.caster(), data);
        return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
    }

    private HashMap<ItemStack, Integer> createOverrideItemsMap(int vaultSize, SpellData data) {

        HashMap<ItemStack, Integer> overrideItems = new HashMap<>();
        Set<Integer> usedSlots = new HashSet<>();

        double r = random.nextDouble() * 100;
        double m = 0;
        for (int i = 0; i < itemTypes.length; i++) {
            if (itemTypes[i] != null && r < itemChances[i] + m) {
                int slot = itemSlots[i];

                if (usedSlots.contains(itemSlots[i])) continue;

                if (slot == -1) {
                    slot = random.nextInt(vaultSize);
                    while (usedSlots.contains(slot)) {
                        slot = random.nextInt(vaultSize);
                    }
                }
                ItemStack overrideItem = createOverrideItem(itemTypes[i], itemMinQuantities[i], itemMaxQuantities[i]);
                overrideItems.put(overrideItem, slot);
                usedSlots.add(itemSlots[i]);
            }
        }

        return overrideItems;
    }

    private ItemStack createOverrideItem(ItemStack item, int minQuant, int maxQuant) {
        int quant = minQuant;
        if (maxQuant > minQuant) {
            quant = random.nextInt(maxQuant - minQuant) + minQuant;
        }
        if (quant > 0) {
            ItemStack overrideItem = item.clone();
            overrideItem.setAmount(quant);
            return overrideItem;
        }
        return new ItemStack(Material.AIR);
    }

    private byte[] serializeItemStackList(List<ItemStack> items) {
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

    private ItemStack[] deserializeItemStackByteArray(byte[] bytes) {
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof  VaultInventory vault)) return;

        List<ItemStack> vaultItems = new ArrayList<>(Arrays.asList(event.getInventory().getContents()));

        try {
            Files.write(vault.getPlayerVaultFile().toPath(), serializeItemStackList(vaultItems));
        } catch (IOException e) {
            MagicSpells.error("Vault Spell " + internalName + " encountered an error saving items when closing the vault");
            throw new RuntimeException(e);
        }
    }

    private static class VaultInventory implements InventoryHolder {

        Inventory inventory;
        String vaultName;
        int size;
        private File playerVaultFile;
        public VaultInventory(String vaultName, int size, Component displayName) {
            this.inventory = Bukkit.createInventory(this, size, displayName);
            this.vaultName = vaultName;
            this.size = size;
        }

        public void setPlayerVaultFile(File playerVaultFile) {
            this.playerVaultFile = playerVaultFile;
        }

        public File getPlayerVaultFile() {
            return playerVaultFile;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    public enum VaultSize {
        SIZE_9(9),
        SIZE_18(18),
        SIZE_27(27),
        SIZE_36(36),
        SIZE_45(45),
        SIZE_54(54);

        private final int size;

        VaultSize(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

}
