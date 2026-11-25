/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/** Minimal "look at this block" helper: rotates locally and tells the server in the same tick. */
public final class Rotations {
    private Rotations() {
    }

    public static void lookAt(Vec3 target) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || player.connection == null) return;

        Vec3 eye = player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision));
    }
}
