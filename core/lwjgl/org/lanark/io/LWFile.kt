package org.lanark.io

import org.lanark.system.*
import java.nio.*
import java.nio.channels.*
import java.nio.file.*

actual class File(val file: SeekableByteChannel) : Managed {
    actual val size: Long
        get() = file.size()
    
    actual val position: Long
        get() = file.position()

    actual fun read(count: Int): ByteArray {
        val buffer = ByteBuffer.allocate(count)
        file.read(buffer)
        return buffer.array()
    }

    actual fun write(source: ByteArray): ULong {
        return file.write(ByteBuffer.wrap(source)).toULong()
    }

    actual fun seek(position: Long, seekFrom: SeekFrom): Long {
        file.position(position)
        return file.position()
    }

    override fun release() {
        close()
    }

    actual fun close() {
        file.close()
    }
}

actual enum class FileOpenMode(vararg val value: OpenOption) {
    Read(StandardOpenOption.READ),
    Truncate(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE),
    Append(StandardOpenOption.APPEND, StandardOpenOption.WRITE),
    Update(StandardOpenOption.WRITE),
}