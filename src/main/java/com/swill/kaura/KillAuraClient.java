package com.swill.kaura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Random;

public class KillAuraClient implements ClientModInitializer {
    
    private static boolean enabled = true;
    private static float reach = 3.4f;
    private static float minIntervalSeconds = 1.7f;
    private static float maxIntervalSeconds = 1.87f;
    private static boolean silentRotations = true;
    private static boolean fakeSwing = true;
    
    private static Random random = new Random();
    private static KeyBinding toggleKey;
    private static int cooldownTicks = 0;
    
    @Override
    public void onInitializeClient() {
        System.out.println("[SWILL] KillAura Client загружен | Интервал 1.7-1.87 сек");
        
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.killaura.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "category.killaura"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.world == null) return;
            
            while (toggleKey.wasPressed()) {
                enabled = !enabled;
                System.out.println("[SWILL] KillAura: " + (enabled ? "ВКЛ" : "ВЫКЛ"));
            }
            if (!enabled) return;
            
            if (cooldownTicks > 0) {
                cooldownTicks--;
                return;
            }
            
            LivingEntity target = findBestTarget(client);
            if (target != null) {
                attack(client, target);
                float interval = minIntervalSeconds + random.nextFloat() * (maxIntervalSeconds - minIntervalSeconds);
                cooldownTicks = (int)(interval * 20);
            }
        });
    }
    
    private LivingEntity findBestTarget(MinecraftClient client) {
        Vec3d eyePos = client.player.getEyePos();
        double bestDistance = reach + 0.5;
        LivingEntity bestTarget = null;
        
        Box searchBox = client.player.getBoundingBox().expand(reach);
        List<LivingEntity> entities = client.world.getEntitiesByClass(
            LivingEntity.class,
            searchBox,
            entity -> entity != client.player && entity.isAlive() && !entity.isRemoved()
        );
        
        for (LivingEntity entity : entities) {
            double distance = eyePos.distanceTo(entity.getPos());
            if (distance > reach) continue;
            if (!client.player.canSee(entity)) continue;
            
            if (distance < bestDistance) {
                bestDistance = distance;
                bestTarget = entity;
            }
        }
        return bestTarget;
    }
    
    private void attack(MinecraftClient client, LivingEntity target) {
        float originalYaw = client.player.getYaw();
        float originalPitch = client.player.getPitch();
        
        Vec3d direction = target.getPos().add(0, target.getHeight()/2, 0).subtract(client.player.getEyePos());
        float targetYaw = (float)(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90);
        float targetPitch = (float)(-Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x*direction.x + direction.z*direction.z))));
        
        if (silentRotations) {
            client.player.setYaw(targetYaw);
            client.player.setPitch(targetPitch);
        }
        
        client.interactionManager.attackEntity(client.player, target);
        
        if (fakeSwing) {
            client.player.swingHand(Hand.MAIN_HAND);
        }
        
        if (silentRotations) {
            client.player.setYaw(originalYaw);
            client.player.setPitch(originalPitch);
        }
    }
}
