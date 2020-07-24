package com.google.pegtyped.runtime

class ParseError(var pos: Int, override val message: String? = null): RuntimeException(message) {
    fun ingest(other: ParseError): ParseError {
        if (other.pos > pos) {
            return other
        }
        return this
    }

    override fun toString(): String {
        return "at pos=$pos: $message"
    }
}
