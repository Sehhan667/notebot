/*
 * Notebot — standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/** Resolves the instrument a noteblock currently uses. */
@FunctionalInterface
public interface InstrumentDetectFunction {
    NoteBlockInstrument detectInstrument(BlockState noteBlock, BlockPos blockPos);
}
