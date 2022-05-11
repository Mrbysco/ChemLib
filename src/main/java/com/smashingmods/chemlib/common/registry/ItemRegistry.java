package com.smashingmods.chemlib.common.registry;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smashingmods.chemlib.ChemLib;
import com.smashingmods.chemlib.api.Chemical;
import com.smashingmods.chemlib.api.Element;
import com.smashingmods.chemlib.api.MatterState;
import com.smashingmods.chemlib.common.items.CompoundItem;
import com.smashingmods.chemlib.common.items.ElementItem;
import com.smashingmods.chemlib.common.items.IngotItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.List;

public class ItemRegistry {

    public static final CreativeModeTab CHEMISTRY_TAB = new CreativeModeTab(ChemLib.MODID) {
        @Override
        @Nonnull
        public ItemStack makeIcon() {
            return new ItemStack(getElementByName("hydrogen").get());
        }
    };

    public static final Item.Properties ITEM_PROPERTIES = new Item.Properties().tab(CHEMISTRY_TAB);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ChemLib.MODID);
    public static final BiMap<Integer, ElementItem> ELEMENTS = HashBiMap.create();
    public static final List<IngotItem> INGOTS = new ArrayList<>();
    public static final List<CompoundItem> COMPOUNDS = new ArrayList<>();
    public static final List<BlockItem> BLOCK_ITEMS = new ArrayList<>();

    private static void registerElements() {
        InputStream stream = ChemLib.class.getResourceAsStream("/data/chemlib/elements.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));

        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

        for (JsonElement jsonElement : jsonObject.getAsJsonArray("elements")) {
            JsonObject object = jsonElement.getAsJsonObject();
            String elementName = object.get("name").getAsString();
            int atomicNumber = object.get("atomic_number").getAsInt();
            String abbreviation = object.get("abbreviation").getAsString();
            String matterState = object.get("matter_state").getAsString();
            String color = object.get("color").getAsString();

            ITEMS.register(elementName, () -> new ElementItem(elementName, atomicNumber, abbreviation, MatterState.valueOf(matterState.toUpperCase()), color));

            List<String> defaultIngots = Arrays.asList("copper", "iron", "gold");
            if (matterState.equals("solid") && !defaultIngots.contains(elementName)) {
                registerIngot(ITEMS.getEntries().stream().filter(item -> item.getId().getPath().equals(elementName)).findFirst().get());
            }
        }
    }

    private static void registerCompounds() {
        InputStream stream = ChemLib.class.getResourceAsStream("/data/chemlib/compounds.json");
        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)));

        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

        for (JsonElement jsonElement : jsonObject.getAsJsonArray("compounds")) {
            JsonObject object = jsonElement.getAsJsonObject();
            String compoundName = object.get("name").getAsString();
            String matterState = object.get("matter_state").getAsString();
            String color = object.get("color").getAsString();

            JsonArray components = object.getAsJsonArray("components");
            List<ItemStack> stacks = new ArrayList<>();
            for (JsonElement component : components) {
                JsonObject componentObject = component.getAsJsonObject();
                String componentName = componentObject.get("name").getAsString();
                int count = componentObject.has("count") ? componentObject.get("count").getAsInt() : 1;

                ElementItem elementItem = getElementByName(componentName).orElse(null);
                CompoundItem compoundItem = getCompoundByName(componentName).orElse(null);

                if (elementItem != null) {
                    stacks.add(new ItemStack(elementItem, count));
                } else if (compoundItem != null) {
                    stacks.add(new ItemStack(compoundItem, count));
                }
            }

            ITEMS.register(compoundName, () -> new CompoundItem(compoundName, MatterState.valueOf(matterState.toUpperCase()), stacks, color));
        }
    }

    public static void registerLampItems() {
        BlockRegistry.BLOCKS.getEntries().forEach(ItemRegistry::fromBlock);
    }

    public static Optional<ElementItem> getElementByName(String pName) {
        return ELEMENTS.values().stream().filter(elementItem -> elementItem.getName().equals(pName)).findFirst();
    }

    public static Optional<ElementItem> getElementByAtomicNumber(Integer pAtomicNumber) {
        return ELEMENTS.values().stream().filter(elementItem -> elementItem.getAtomicNumber() == pAtomicNumber).findFirst();
    }

    public static Optional<CompoundItem> getCompoundByName(String pName) {
        return COMPOUNDS.stream().filter(compoundItem -> compoundItem.getName().equals(pName)).findFirst();
    }

    public static Optional<IngotItem> getIngotByName(String pName) {
        return INGOTS.stream().filter(ingotItem -> Objects.equals(ingotItem.getRegistryName(), pName)).findFirst();
    }

    public static <I extends Item> void registerIngot(RegistryObject<I> item) {
        ITEMS.register(String.format("%s_ingot", item.getId().getPath()), () -> new IngotItem((Element) item.get()));
    }

    public static <B extends Block> void fromBlock(RegistryObject<B> block) {
        ITEMS.register(block.getId().getPath(), () -> {
            BlockItem temp = new BlockItem(block.get(), new Item.Properties().tab(CHEMISTRY_TAB));
            BLOCK_ITEMS.add(temp);
            return temp;
        });
    }

    public static void register(IEventBus eventBus) {
        registerElements();
        registerCompounds();
        registerLampItems();
        ITEMS.register(eventBus);
    }
}
