package net.generic404.armorspeed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShieldItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Armorspeed implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("Armorspeed");
	/** Amount of speed to take off per armor point */
	public static float DEBUFF_AMOUNT = 0.01f;
	/** Amount of speed to take off when shield is held */
	public static float DEBUFF_AMOUNT_SHIELD = 0.05f;
	/** Amount of speed to always add after debuffs */
	public static float DEBUFF_OFFSET = 0.1f;
	/** Apply speed modifier to mobs as well? */
	public static boolean AFFECT_MOBS = true;

	@Override
	public void onInitialize() {
		ItemTags.init();
		Config.init();

		ServerTickEvents.END_SERVER_TICK.register((server) -> applySlowness(server, ItemTags.WEIGHTLESS));
	}

	public static double getArmorValue(LivingEntity entity, TagKey<Item> WEIGHTLESS) {
		double out = 0;

		for (var stack : entity.getArmorAndBodyArmorSlots()) {
			if (stack.getItem() instanceof ArmorItem && !stack.is(WEIGHTLESS)) {
				var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
				if (modifiers != null) {
					for (var modifier : modifiers.modifiers()) {
						if (modifier.attribute().equals(Attributes.ARMOR)) {
							out += modifier.modifier().amount();
						}
					}
				}
			}
		}

		return out;
	}

	private static boolean isHoldingShield(LivingEntity entity) {
		return (
				entity.getOffhandItem().getItem() instanceof ShieldItem || entity.getMainHandItem().getItem() instanceof ShieldItem
		);
	}

	public static AttributeModifier getArmorSpeedModifier(LivingEntity entity, TagKey<Item> WEIGHTLESS) {
		return new AttributeModifier(
				ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,"speedmodifier"),
				(
						(
								(getArmorValue(entity,WEIGHTLESS)*DEBUFF_AMOUNT) // base armor debuff
										+ (isHoldingShield(entity)?DEBUFF_AMOUNT_SHIELD:0) // shield debuff
						)
								* -1 // invert so it actually subtracts from speed
								+ DEBUFF_OFFSET // offset
				),
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		);
	}

	private static long lastLog = System.currentTimeMillis();
	private static final long logCooldown = 1000;
	public static void applySlowness(MinecraftServer server, TagKey<Item> WEIGHTLESS) {
		if (AFFECT_MOBS) {

			var levels = server.getAllLevels();
			for (var level : levels) {
				var entities = level.getAllEntities();
				for (var entity : entities) {
					if (entity instanceof LivingEntity living && living.isAlive()) {

						try {
							living.getAttribute(Attributes.MOVEMENT_SPEED)
									.addOrUpdateTransientModifier(
											getArmorSpeedModifier(
													living,
													WEIGHTLESS
											)
									);
						} catch (Exception e) {
							if (lastLog==-1 || System.currentTimeMillis()>lastLog+logCooldown) {
								Constants.LOGGER.error("Failed to apply slowness to living entity {} / UUID: {}", living.getName().getString(), living.getStringUUID());
								lastLog = -1;
							}
						}

					}
				}
			}

		} else {

			var players = server.getPlayerList().getPlayers();
			for (var player : players) {

				try {
					player.getAttribute(Attributes.MOVEMENT_SPEED)
							.addOrUpdateTransientModifier(
									getArmorSpeedModifier(
											player,
											WEIGHTLESS
									)
							);
				} catch (Exception e) {
					if (lastLog==-1 || System.currentTimeMillis()>lastLog+logCooldown) {
						Constants.LOGGER.error("Failed to apply slowness to player {}", player.getName().getString());
						lastLog = -1;
					}
				}

			}

		}

		if (lastLog == -1) {
			lastLog = System.currentTimeMillis();
		}
	}
}
