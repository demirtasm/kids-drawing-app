package com.example.kidsdrawingapp.handler

import com.example.kidsdrawingapp.model.TouchEvent

interface TouchHandler {
    fun handleFirstTouch(event: TouchEvent)
    fun handleTouch(event: TouchEvent)
    fun handleLastTouch(event: TouchEvent)
}
