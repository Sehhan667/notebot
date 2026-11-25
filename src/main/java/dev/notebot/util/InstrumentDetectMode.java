/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.NoteBlock;

/**
 * How the instrument of a noteblock is detected.
 * Useful for servers whose plugins rewrite the block state while the sound still plays correctly.
 */
public enum InstrumentDetectMode {
    BLOCK_STATE((state, pos) -> state.getValue(NoteBlock.INSTRUMENT)),
    BELOW_BLOCK((state, pos) -> {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        return level.getBlockState(pos.below()).instrument();
    });

    private final InstrumentDetectFunction function;

    InstrumentDetectMode(InstrumentDetectFunction function) {
        this.function = function;
    }

    public InstrumentDetectFunction getFunction() {
        return function;
    }
}
