package net.generic404_.armorspeed;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Objects;

public class TickEvent {
    @SubscribeEvent
    public static void onServerTickEvent(net.minecraftforge.event.TickEvent.ServerTickEvent event){
        Constants.LOG.info("gay");

        MinecraftServer EndTick = event.getServer();

        List<ServerPlayer> players = EndTick.getPlayerList().getPlayers();
        for (int i = 0; i < players.toArray().length; i++) {
            ServerPlayer plr = players.get(i);
            Objects.requireNonNull(plr.getAttribute(Attributes.MOVEMENT_SPEED))
                    .addOrReplacePermanentModifier(new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID,"speedmodifier"),
                            (plr.getArmorValue()*-0.01)+0.01,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ));
            Constants.LOG.info(String.valueOf(plr.getArmorValue()));
        }
    }
}
