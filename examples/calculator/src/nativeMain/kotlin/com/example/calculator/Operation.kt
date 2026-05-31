package com.example.calculator

enum class Operation {
    ADD,
    SUBTRACT,
    MULTIPLY,
}

enum class Status(val code: Int) {
    SUCCESS(0),
    ERROR(1),
    PENDING(2);
}
