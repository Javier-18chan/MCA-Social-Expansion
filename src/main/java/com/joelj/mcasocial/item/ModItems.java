package com.joelj.mcasocial.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "mcasocial");

    public static final RegistryObject<Item> SMARTPHONE = ITEMS.register("smartphone",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CARTA = ITEMS.register("carta",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CRYSTAL = ITEMS.register("crystal",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MAGIC_MIRROR = ITEMS.register("magic_mirror",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
