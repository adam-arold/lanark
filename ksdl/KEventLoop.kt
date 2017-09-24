package kdsl

import eventNames
import kotlinx.cinterop.*
import ksdl.*
import sdl2.*

class KEventLoop() {
    private var quit = false

    private var queue = ArrayList<(() -> Unit)?>(2).apply {
        add(null)
        add(null)
    }

    private var queueHead = 0
    private var queueTail = 0

    val windowEvents = KEventSource<KEventWindow>("Window")
    val appEvents = KEventSource<KEventApp>("App")
    val keyEvents = KEventSource<KEventKey>("Key")

    fun submitSelf() {
        val task = currentTask
        if (task == null)
            logger.error("submitSelf should be called from within executing task only")
        else
            submit(task)
    }

    fun submit(task: () -> Unit) {
        if (queueHead == queueTail + 1) {
            // queue is filled, expand it (improve algorithm by adding more)
            queue.add(null)
            for (index in queue.lastIndex - 1 downTo queueHead) {
                queue[index + 1] = queue[index]
            }
            queueHead++
            logger.trace("Expanded queue: ${queue.size} ")
        } else if (queueHead == 0 && queueTail == queue.lastIndex) {
            queue.add(null)
            logger.trace("Expanded queue: ${queue.size} ")
        }

        if (queueTail == queue.lastIndex) {
            queue[queueTail] = task
            queueTail = 0
        } else {
            queue[queueTail++] = task
        }
        //logger.trace("Submitted task: ${queue.size} [$queueHead, $queueTail]")
    }

    fun peek(): (() -> Unit)? {
        if (queueHead == queueTail) return null
        val task = queue[queueHead]
        queue[queueHead] = null
        if (queueHead == queue.lastIndex)
            queueHead = 0
        else
            queueHead++
        //logger.trace("Peeked task: ${queue.size} [$queueHead, $queueTail]")
        return task
    }

    fun run() {
        logger.trace("Running event loop")
        while (!quit) {
            while (true) {
                memScoped {
                    val event = alloc<SDL_Event>()
                    while (SDL_PollEvent(event.ptr) == 1) {
                        processEvent(event)
                        if (quit)
                            break
                    }
                }

                if (quit)
                    break

                currentTask = peek()
                (currentTask ?: break)()
                currentTask = null
            }
        }
        logger.trace("Stopped event loop")
    }

    private fun processEvent(event: SDL_Event) {
        val eventName = eventNames[event.type]
        when (event.type) {
            SDL_QUIT -> {
                quit = true
                logger.trace("Event: SDL_QUIT")
                return
            }
            SDL_APP_TERMINATING, SDL_APP_LOWMEMORY, SDL_APP_DIDENTERBACKGROUND,
            SDL_APP_DIDENTERFOREGROUND, SDL_APP_WILLENTERBACKGROUND, SDL_APP_WILLENTERFOREGROUND -> {
                val eventApp = KEventApp.createEvent(event)
                logger.trace("Event: $eventApp")
                appEvents.raise(eventApp)
            }
            SDL_WINDOWEVENT -> {
                val eventWindow = KEventWindow.createEvent(event)
                logger.trace("Event: $eventWindow")
                windowEvents.raise(eventWindow)
            }
            SDL_KEYUP, SDL_KEYDOWN -> {
                val eventWindow = KEventKey.createEvent(event)
                logger.trace("Event: $eventWindow")
                keyEvents.raise(eventWindow)
            }
            else -> {
                if (eventName == null)
                    logger.trace("Unknown event eventType: ${event.type}")
                else
                    logger.trace("Event: $eventName")
            }
        }
    }

    var currentTask: (() -> Unit)? = null
}

