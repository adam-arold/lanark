package org.lanark.playground

import org.lanark.application.*
import org.lanark.drawing.*
import org.lanark.events.*
import org.lanark.geometry.*

class Bouncer(
    private val tile: Tile,
    initialPosition: Point,
    private val minPosition: Int,
    private val maxPosition: Int,
    private val speed: Int,
    private val normalCursor: Cursor,
    private val hotCursor: Cursor
) {
    private var itemPosition = initialPosition

    suspend fun run() {
        try {
            while (true) {
                while (itemPosition.x < maxPosition) {
                    itemPosition += Vector(speed /* * dt */, 0)
                    nextTick()
                }
                while (itemPosition.x > minPosition) {
                    itemPosition -= Vector(speed, 0)
                    nextTick()
                }
            }
        } catch (e: Throwable) {
            println("org.lanark.playground.Bouncer: $e")
            throw e
        }
    }

    fun render(frame: Frame) {
        frame.draw(tile, itemPosition)
    }

    fun handle(frame: Frame, event: Event): Boolean {
        when (event) {
            is EventMouseMotion -> {
                if (event.position in Rect(itemPosition, tile.size)) {
                    frame.cursor = hotCursor
                    return true
                }
            }
        }
        return false
    }
}