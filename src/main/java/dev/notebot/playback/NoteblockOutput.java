/*
 * Notebot - standalone port of MeteorClient's Notebot (GPL-3.0).
 */

package dev.notebot.playback;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.notebot.config.NotebotConfig;
import dev.notebot.song.Note;
import dev.notebot.song.Song;
import dev.notebot.util.NotebotMode;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The classic Notebot: find nearby noteblocks, tune them to the notes the song needs,
 * then trigger them with attack packets.
 */
public final class NoteblockOutput implements NoteOutput {
    private enum Stage {
        SETUP,
        TUNE,
        WAIT,
        PLAY
    }

    private final NotebotConfig config;

    private Song song;
    private Stage stage = Stage.SETUP;

    private final Map<Note, BlockPos> positions = new HashMap<>();
    private final Multimap<Note, BlockPos> scanned = MultimapBuilder.linkedHashKeys().arrayListValues().build();
    private final Map<BlockPos, Integer> tuneHits = new HashMap<>();
    private final Set<BlockPos> clickedBlocks = new HashSet<>();

    /** Client-side prediction sequence; the server only acks it, so our own counter is fine. */
    private static int sequence = 1;

    private int ticks;
    private int waitTicks = -1;
    private boolean anyTuned;
    private boolean ready;
    private boolean failed;
    private String status = "Scanning";

    public NoteblockOutput(NotebotConfig config) {
        this.config = config;
    }

    @Override
    public void onStart(String relativePath, Song song) {
        this.song = song;
        this.stage = Stage.SETUP;
        this.positions.clear();
        this.scanned.clear();
        this.tuneHits.clear();
        this.ticks = 0;
        this.waitTicks = -1;
        this.anyTuned = false;
        this.ready = false;
        this.failed = false;
        this.status = "Scanning noteblocks";
    }

    @Override
    public void onTick() {
        if (failed || song == null) return;

        switch (stage) {
            case SETUP -> setup();
            case TUNE -> tune();
            case WAIT -> {
                if (--waitTicks <= 0) {
                    waitTicks = -1;
                    setupTuneHits();
                    stage = Stage.TUNE;
                    status = "Re-tuning";
                }
            }
            case PLAY -> {
            }
        }
    }

    @Override
    public void playNotes(java.util.Collection<Note> notes) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.gameMode == null || mc.level == null || notes.isEmpty()) return;

        if (config.autoRotate) {
            BlockPos first = positions.get(notes.iterator().next());
            if (first != null) Rotations.lookAt(Vec3.atCenterOf(first));
        }
        if (config.swingArm) swing(player);

        for (Note note : notes) {
            BlockPos pos = positions.get(note);
            if (pos == null) continue;
            int seq = sequence++;
            player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.DOWN, seq));
        }
    }

    @Override
    public boolean ready() {
        return ready;
    }

    @Override
    public boolean failed() {
        return failed;
    }

    @Override
    public String status() {
        return status;
    }

    // ------------------------------------------------------------------ setup

    private void setup() {
        scan();

        if (scanned.isEmpty()) {
            fail("No noteblock in reach");
            return;
        }

        setupPositions();
        if (positions.isEmpty()) {
            fail("Not enough noteblocks for this song");
            return;
        }

        setupTuneHits();
        ticks = 0;
        stage = Stage.TUNE;
        status = "Tuning";
    }

    private void scan() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null) return;

        scanned.clear();

        int range = (int) player.blockInteractionRange();
        int min = -range - 2;
        int max = range + 2;

        for (int y = min; y < max; y++) {
            for (int x = min; x < max; x++) {
                for (int z = min; z < max; z++) {
                    BlockPos pos = player.blockPosition().offset(x, y + 1, z);

                    BlockState state = mc.level.getBlockState(pos);
                    if (state.getBlock() != Blocks.NOTE_BLOCK) continue;
                    if (!player.isWithinBlockInteractionRange(pos, 1)) continue;
                    if (!mc.level.getBlockState(pos.above()).isAir()) continue;

                    NoteBlockInstrument instrument = null;
                    if (config.usesExactInstruments()) {
                        instrument = config.instrumentDetectMode.getFunction().detectInstrument(state, pos);
                    }

                    int level = state.getValue(NoteBlock.NOTE);
                    scanned.put(new Note(instrument, level), pos);
                }
            }
        }
    }

    private void setupPositions() {
        positions.clear();

        List<Note> uniqueNotes = new ArrayList<>(song.getRequirements());
        Map<NoteBlockInstrument, List<BlockPos>> incorrect = new HashMap<>();

        for (var entry : scanned.asMap().entrySet()) {
            Note note = entry.getKey();
            List<BlockPos> blocks = new ArrayList<>(entry.getValue());

            if (uniqueNotes.contains(note)) {
                positions.put(note, blocks.removeFirst());
                uniqueNotes.remove(note);
            }

            if (!blocks.isEmpty()) {
                if (!incorrect.containsKey(note.getInstrument())) {
                    incorrect.put(note.getInstrument(), new ArrayList<>());
                }
                incorrect.get(note.getInstrument()).addAll(blocks);
            }
        }

        for (var entry : incorrect.entrySet()) {
            List<BlockPos> blocks = entry.getValue();

            if (config.mode == NotebotMode.EXACT_INSTRUMENTS) {
                NoteBlockInstrument instrument = entry.getKey();
                List<Note> found = uniqueNotes.stream()
                    .filter(note -> note.getInstrument() == instrument)
                    .collect(Collectors.toList());

                if (found.isEmpty()) continue;

                for (BlockPos pos : blocks) {
                    if (found.isEmpty()) break;
                    Note note = found.removeFirst();
                    positions.put(note, pos);
                    uniqueNotes.remove(note);
                }
            } else {
                for (BlockPos pos : blocks) {
                    if (uniqueNotes.isEmpty()) break;
                    Note note = uniqueNotes.removeFirst();
                    positions.put(note, pos);
                }
            }
        }
    }

    private void setupTuneHits() {
        tuneHits.clear();

        var mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (var entry : positions.entrySet()) {
            int target = entry.getKey().getNoteLevel();
            BlockPos pos = entry.getValue();

            BlockState state = mc.level.getBlockState(pos);
            if (state.getBlock() != Blocks.NOTE_BLOCK) continue;

            int current = state.getValue(NoteBlock.NOTE);
            if (current != target) {
                tuneHits.put(pos, calcHits(current, target));
            }
        }
    }

    private void tune() {
        if (tuneHits.isEmpty()) {
            if (anyTuned) {
                anyTuned = false;
                waitTicks = config.checkNoteblocksAgainDelay;
                stage = Stage.WAIT;
                status = "Checking noteblocks";
            } else {
                stage = Stage.PLAY;
                ready = true;
                status = "Playing";
            }
            return;
        }

        if (ticks < config.tickDelay) {
            ticks++;
            return;
        }

        tuneBlocks();
        ticks = 0;
    }

    private void tuneBlocks() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.gameMode == null || mc.level == null) {
            fail("Player left the world");
            return;
        }

        if (config.swingArm) swing(player);

        int done = 0;
        var iterator = tuneHits.entrySet().iterator();
        while (iterator.hasNext() && done < config.concurrentTuneBlocks) {
            var entry = iterator.next();
            BlockPos pos = entry.getKey();

            if (config.autoRotate) Rotations.lookAt(Vec3.atCenterOf(pos));

            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false));

            anyTuned = true;

            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
            done++;
        }
    }

    private void fail(String reason) {
        failed = true;
        status = reason;
    }

    private static void swing(net.minecraft.client.player.LocalPlayer player) {
        player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
    }

    /** How many hits are needed to go from one note level to another (note blocks wrap at 24). */
    private static int calcHits(int from, int to) {
        return from > to ? (25 - from) + to : to - from;
    }
}
