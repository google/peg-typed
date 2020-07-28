// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.pegtyped

import java.io.File

sealed class Variant
data class Constructing(val name: String, val items: List<Item>) : Variant()
data class NonConstructing(val expr: Expr) : Variant()

sealed class Type {
    fun kotlinName(): String {
        return when (this) {
            is Defined -> name
            Slice -> "LocationInfo"
        }
    }
}

data class Defined(val name: String) : Type()
object Slice : Type() {
    override fun toString(): String {
        return this::class.java.simpleName
    }
}

data class Rule(
        val name: String,
        val type: Type,
        val expansion: List<Variant>
)

sealed class Item
data class KeepItem(
        val name: String,
        val expr: Expr
) : Item()

data class DropItem(val expr: Expr) : Item()

sealed class Expr
data class Literal(val s: String) : Expr()
data class CharRange(val from: Char, val to: Char) : Expr()
data class Reference(val target: String) : Expr()
data class Repeat(val minCount: Int, val item: Expr) : Expr() {
    constructor(item: Expr) : this(0, item)
}
data class Negate(val inner: Expr): Expr()
object AnyChar: Expr() {
    override fun toString(): String {
        return this::class.java.simpleName
    }
}

data class AsString(val inner: Expr) : Expr()
data class Sequence(val keepIndex: Int, val children: List<Expr>) : Expr()
data class Choice(val options: List<Expr>): Expr()
data class Optional(val inner: Expr): Expr()

class Grammar(
        val startName: String,
        val rules: List<Rule>,
        val packageName: String?
) {
    val ruleByName = rules.map { Pair(it.name, it) }.toMap()

    val startRule = ruleByName[startName] ?: throw RuntimeException("start rule $startName not found")
}

fun writeGrammar(destination: String, grammar: Grammar) {
    val stream = File(destination).outputStream()
    val writer = stream.writer()
    val generator = CodeGenerator(4, grammar)
    generator.writeAll(writer)
    writer.close()
    stream.close()
}

