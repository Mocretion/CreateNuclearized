package com.mocretion.createnuclearized.content.reactor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public record InteriorGrid(int width, int depth, BlockPos floorOrigin, int fuelIndex, int reflectorPercent,
        List<Cell> cells) {

    public static final InteriorGrid EMPTY =
            new InteriorGrid(0, 0, BlockPos.ZERO, 0, 0, List.of());

    public record Mod(ResourceLocation block, int count) {
        public Component displayName() {
            Block resolved = BuiltInRegistries.BLOCK.get(block);
            return count > 1
                    ? Component.literal(count + "x ").append(resolved.getName())
                    : resolved.getName();
        }
    }

    public enum ValveState {
        INACTIVE, ACTIVE, FULL;

        public static ValveState byOrdinal(int ordinal) {
            ValveState[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : INACTIVE;
        }
    }

    public record Cell(int col, int row, ValveState state, int burnShift, int purityShift, double rate,
            List<Mod> modifiers) {
        public BlockPos valvePos(BlockPos floorOrigin) {
            return floorOrigin.offset(col, 0, row);
        }
    }

    public boolean isEmpty() {
        return width == 0 || depth == 0;
    }

    public ReactorFuel fuel() {
        return ReactorFuel.byIndex(fuelIndex);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("W", width);
        tag.putInt("D", depth);
        tag.putInt("Ox", floorOrigin.getX());
        tag.putInt("Oy", floorOrigin.getY());
        tag.putInt("Oz", floorOrigin.getZ());
        tag.putInt("Fuel", fuelIndex);
        tag.putInt("Refl", reflectorPercent);
        ListTag cellsTag = new ListTag();
        for (Cell cell : cells) {
            CompoundTag c = new CompoundTag();
            c.putInt("c", cell.col());
            c.putInt("r", cell.row());
            c.putInt("s", cell.state().ordinal());
            c.putInt("b", cell.burnShift());
            c.putInt("p", cell.purityShift());
            c.putDouble("rt", cell.rate());
            ListTag mods = new ListTag();
            for (Mod mod : cell.modifiers()) {
                CompoundTag m = new CompoundTag();
                m.putString("id", mod.block().toString());
                m.putInt("n", mod.count());
                mods.add(m);
            }
            c.put("m", mods);
            cellsTag.add(c);
        }
        tag.put("Cells", cellsTag);
        return tag;
    }

    public static InteriorGrid load(CompoundTag tag) {
        BlockPos origin = new BlockPos(tag.getInt("Ox"), tag.getInt("Oy"), tag.getInt("Oz"));
        List<Cell> cells = new ArrayList<>();
        ListTag cellsTag = tag.getList("Cells", Tag.TAG_COMPOUND);
        for (int i = 0; i < cellsTag.size(); i++) {
            CompoundTag c = cellsTag.getCompound(i);
            List<Mod> mods = new ArrayList<>();
            ListTag modsTag = c.getList("m", Tag.TAG_COMPOUND);
            for (int j = 0; j < modsTag.size(); j++) {
                CompoundTag m = modsTag.getCompound(j);
                ResourceLocation id = ResourceLocation.tryParse(m.getString("id"));
                if (id != null)
                    mods.add(new Mod(id, m.getInt("n")));
            }
            cells.add(new Cell(c.getInt("c"), c.getInt("r"), ValveState.byOrdinal(c.getInt("s")),
                    c.getInt("b"), c.getInt("p"), c.getDouble("rt"), mods));
        }
        return new InteriorGrid(tag.getInt("W"), tag.getInt("D"), origin,
                tag.getInt("Fuel"), tag.getInt("Refl"), cells);
    }
}
