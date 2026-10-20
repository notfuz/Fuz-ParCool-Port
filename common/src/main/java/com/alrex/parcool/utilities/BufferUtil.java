package com.alrex.parcool.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;

public class BufferUtil {
	ByteBuffer buffer;

	/**
	 * A start/state buffer, 128 bytes. The largest honest payload today is HideInBlock's 98-byte
	 * start buffer (2 booleans + 2 BlockPos + 3 Vec3), so the head-room is 30 bytes; every writer is
	 * checked against {@link #remaining} through {@link #ensureRoom}, because an action that added one
	 * field used to overflow silently, and the resulting BufferOverflowException was caught by
	 * ActionProcessor and turned into a force-finish with no indication of the real cause.
	 */
	public static final int SYNC_BUFFER_SIZE = 128;

	private BufferUtil(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public static BufferUtil wrap(ByteBuffer byteBuffer) {
		return new BufferUtil(byteBuffer);
	}

	/** Free bytes left in the wrapped buffer, for an action that wants to check before writing. */
	public int remaining() {
		return this.buffer.remaining();
	}

	private void ensureRoom(int bytes) {
		if (this.buffer.remaining() < bytes) {
			throw new IllegalStateException("ParCool sync buffer overflow: " + bytes
					+ " bytes needed, " + this.buffer.remaining() + " left (limit " + SYNC_BUFFER_SIZE
					+ "). Enlarge SYNC_BUFFER_SIZE for the payload this action writes.");
		}
	}

	public BufferUtil putInt(int value) {
		ensureRoom(4);
		buffer.putInt(value);
		return this;
	}

	public BufferUtil putBoolean(boolean bool) {
		ensureRoom(1);
		buffer.put(bool ? (byte) 1 : 0);
		return this;
	}

	public static boolean getBoolean(ByteBuffer buffer) {
		return buffer.get() != 0;
	}

    public BufferUtil putBlockPos(BlockPos pos) {
        return putVector3i(pos);
    }

    public BufferUtil putVector3i(Vec3i vec) {
        ensureRoom(12);
        buffer.putInt(vec.getX()).putInt(vec.getY()).putInt(vec.getZ());
        return this;
    }

    public BufferUtil putVec3(Vec3 vec) {
        ensureRoom(24);
        buffer.putDouble(vec.x()).putDouble(vec.y()).putDouble(vec.z());
        return this;
    }

    public static BlockPos getBlockPos(ByteBuffer buffer) {
        return new BlockPos(buffer.getInt(), buffer.getInt(), buffer.getInt());
    }

    public static Vec3i getVector3i(ByteBuffer buffer) {
        return new Vec3i(buffer.getInt(), buffer.getInt(), buffer.getInt());
    }

    public static Vec3 getVec3(ByteBuffer buffer) {
        return new Vec3(buffer.getDouble(), buffer.getDouble(), buffer.getDouble());
    }

	public ByteBuffer unwrap() {
		return buffer;
	}

	public static boolean haveSameContents(ByteBuffer buffer1, ByteBuffer buffer2) {
		if (buffer1.limit() != buffer2.limit()) {
			return false;
		}
		while (buffer1.hasRemaining() && buffer2.hasRemaining()) {
			if (buffer1.get() != buffer2.get()) {
				return false;
			}
		}
		return true;
	}
}
