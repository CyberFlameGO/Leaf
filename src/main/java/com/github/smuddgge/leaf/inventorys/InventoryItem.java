package com.github.smuddgge.leaf.inventorys;

import com.github.smuddgge.leaf.MessageManager;
import com.github.smuddgge.leaf.configuration.squishyyaml.ConfigurationSection;
import com.github.smuddgge.leaf.configuration.squishyyaml.YamlConfigurationSection;
import com.github.smuddgge.leaf.datatype.User;
import com.github.smuddgge.leaf.placeholders.PlaceholderManager;
import dev.simplix.protocolize.api.item.ItemFlag;
import dev.simplix.protocolize.api.item.ItemStack;
import dev.simplix.protocolize.data.ItemType;
import dev.simplix.protocolize.data.inventory.InventoryType;
import net.querz.nbt.tag.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Represents a custom inventory item.
 */
public class InventoryItem {

    private final ConfigurationSection section;
    private final String slot;
    private final User user;

    /**
     * Used to create an inventory item.
     *
     * @param section The configuration section.
     * @param slot    The slot information.
     * @param user    The item will be in context of this user.
     */
    public InventoryItem(ConfigurationSection section, String slot, User user) {
        this.section = section;
        this.slot = slot;
        this.user = user;
    }

    /**
     * Get an inventory item with the section combined to this
     * item's configuration section.
     *
     * @param section The instance of the configuration section.
     * @return A new inventory item instance.
     */
    public InventoryItem append(ConfigurationSection section) {
        ConfigurationSection clone = new YamlConfigurationSection(new HashMap<>());
        for (String key : section.getKeys()) {
            clone.setInSection(key, section.get(key));
        }
        for (String key : this.section.getKeys()) {
            clone.setInSection(key, section.get(key));
        }

        return new InventoryItem(clone, this.slot, this.user);
    }

    /**
     * Used to get the list of slots.
     *
     * @param inventoryType The type of inventory the slots are in.
     * @return The requested slots
     */
    public List<Integer> getSlots(InventoryType inventoryType) {
        List<Integer> slots = new ArrayList<>();

        for (String argument : this.slot.split(",")) {
            // Check if it's an integer
            if (argument.trim().matches("^[0-9]+$")) {
                slots.add(Integer.parseInt(argument.trim()));
            }

            List<Integer> argumentsSlots = SlotManager.parseSlot(argument.trim(), inventoryType);
            for (Integer integer : argumentsSlots) {
                if (slots.contains(integer)) continue;
                slots.add(integer);
            }
        }

        return slots;
    }

    /**
     * Used to get a default item stack.
     *
     * @return The requested item stack.
     */
    public ItemStack getDefaultItemStack() {
        ItemStack item = new ItemStack(ItemType.LIGHT_GRAY_STAINED_GLASS_PANE);
        item.displayName(MessageManager.convertToLegacy("&7"));
        return item;
    }

    /**
     * Used to get this inventory item as an item stack
     * given a base item stack.
     *
     * @return The requested item stack.
     */
    public ItemStack getItemStack(ItemStack item) {
        CompoundTag compoundTag = new CompoundTag();

        // Set the material of the item.
        try {
            item.itemType(ItemType.valueOf(this.section.getString("material", "LIGHT_GRAY_STAINED_GLASS_PANE")
                    .toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            MessageManager.warn("Invalid material for inventory item. Reference : &f" + this.section.getData().toString());
        }

        // If the item has a skull value.
        if (this.section.getKeys().contains("skull")) {
            item.itemType(ItemType.PLAYER_HEAD);
            String message = this.section.getString("skull", "Steve");
            compoundTag.putString("SkullOwner", PlaceholderManager.parse(message, null, this.user));
        }

        // If the item has nbt values.
        if (this.section.getKeys().contains("nbt")) {
            for (String key : this.section.getSection("nbt").getKeys()) {
                String message = this.section.getSection("nbt").getString(key);
                compoundTag.putString(key, PlaceholderManager.parse(message, null, this.user));
            }
        }

        // Set the display name.
        String name = this.section.getString("name", "&7");
        item.displayName(MessageManager.convertToLegacy(PlaceholderManager.parse(name, null, this.user)));

        // Set the lore.
        for (String line : this.section.getListString("lore", new ArrayList<>())) {
            item.addToLore(MessageManager.convertToLegacy(PlaceholderManager.parse(line, null, this.user)));
        }

        if (this.section.getKeys().contains("durability")) {
            item.durability((short) this.section.getInteger("durability"));
        }

        item.amount((byte) this.section.getInteger("amount", 1));

        // Add the nbt data.
        item.nbtData(compoundTag);

        return item;
    }

    /**
     * Used to get this inventory item as an item stack.
     *
     * @return The requested item stack.
     */
    public ItemStack getItemStack() {
        return this.getItemStack(this.getDefaultItemStack());
    }

    /**
     * Used to check if the inventory item has a command type function.
     *
     * @return True if the item has a command type function.
     */
    public boolean isFunction() {
        return this.section.getKeys().contains("function");
    }

    /**
     * Used to get the function configuration section.
     *
     * @return The requested configuration section.
     */
    public ConfigurationSection getFunctionSection() {
        return this.section.getSection("function");
    }

    /**
     * Used to get the item's configuration section.
     *
     * @return The requested configuration section.
     */
    public ConfigurationSection getSection() {
        return this.section;
    }
}
