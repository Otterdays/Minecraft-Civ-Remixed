package com.fpsmod.ottersciv;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import com.fpsmod.OogaMod;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-world-save list of players who finished at least one join on this save (tracked after first Otters Civ.
 * welcome). Lives in overworld saved data beside vanilla map data etc.
 */
public final class JoinAttendanceSavedData extends SavedData {
    private static final MapCodec<JoinAttendanceSavedData> MAP_CODEC =
        RecordCodecBuilder.mapCodec(
            instance ->
                instance
                    .group(UUIDUtil.CODEC_SET.optionalFieldOf("seen", Set.of()).forGetter(d -> Set.copyOf(d.seen)))
                        .apply(instance, JoinAttendanceSavedData::new)
        );

    private static final Codec<JoinAttendanceSavedData> CODEC = MAP_CODEC.codec();

    public static final SavedDataType<JoinAttendanceSavedData> TYPE =
        new SavedDataType<>(
            Identifier.fromNamespaceAndPath(OogaMod.MOD_ID, "join_attendance"),
            JoinAttendanceSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
        );

    private final HashSet<UUID> seen;

    public JoinAttendanceSavedData() {
        this(Set.of());
    }

    JoinAttendanceSavedData(Set<UUID> seen) {
        this.seen = new HashSet<>(seen);
    }

    public boolean hasSeenBefore(UUID id) {
        return seen.contains(id);
    }

    /** Persists via {@link #setDirty()} when the UUID is new for this save. */
    public void markSeen(UUID id) {
        if (seen.add(id)) {
            setDirty();
        }
    }
}
