package com.tekmoon.utilities

/**
 * Generates a random UUID v4 string in canonical 8-4-4-4-12 form
 * (e.g., "550e8400-e29b-41d4-a716-446655440000"), using each platform's
 * native UUID facility.
 */
expect fun randomUUID(): String
