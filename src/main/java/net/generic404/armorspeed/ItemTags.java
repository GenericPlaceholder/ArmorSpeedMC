package net.generic404.armorspeed;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

public class ItemTags {
	public static final TagKey<Item> WEIGHTLESS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID,"weightless"));
}
