package com.techmath.ticktask

import java.io.Serializable

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long
) : Serializable