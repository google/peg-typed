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

class GrammarBuilder(val wsToken: String? = null) {
    val rules = mutableListOf<Rule>()
    val unnamedTokens = mutableMapOf<String, String>()

    fun Reference.variants(fn: VariantsBuilder.() -> Unit): Reference {
        return variants(Defined(target), fn)
    }

    fun Reference.variants(mergeTo: Reference, fn: VariantsBuilder.() -> Unit): Reference {
        return variants(Defined(mergeTo.target), fn)
    }

    fun Reference.variants(type: Type, fn: VariantsBuilder.() -> Unit): Reference {
        val builder = VariantsBuilder(this@GrammarBuilder)
        fn(builder)
        val result = Rule(target, type, builder.variants)
        rules.add(result)
        return this
    }

    fun Reference.single(fn: ItemsBuilder.() -> Unit): Reference {
        return single(this, fn)
    }

    fun Reference.single(mergeTo: Reference, fn: ItemsBuilder.() -> Unit): Reference {
        return single(Defined(mergeTo.target), fn)
    }

    fun Reference.single(type: Type, fn: ItemsBuilder.() -> Unit): Reference {
        val builder = ItemsBuilder(this@GrammarBuilder)
        fn(builder)
        val result = Rule(target, type, listOf(Constructing(target, builder.items)))
        rules.add(result)
        return this
    }

    fun Reference.alias(fn: SequenceBuilder.() -> Unit): Reference {
        return alias(Defined(target), fn)
    }

    fun Reference.alias(mergeTo: Reference, fn: SequenceBuilder.() -> Unit): Reference {
        return alias(Defined(mergeTo.target), fn)
    }

    fun Reference.alias(type: Type, fn: SequenceBuilder.() -> Unit): Reference {
        val builder = SequenceBuilder(parentBuilder = this@GrammarBuilder)
        fn(builder)
        val result = Rule(target, type, listOf(NonConstructing(builder.build())))
        rules.add(result)
        return this
    }

    fun createToken(name: String, token: String): Reference {
        return createToken(name) {
            keep(Literal(token))
        }
    }

    fun createToken(literal: String): Reference {
        val ruleName = unnamedTokens[literal]
        return when (ruleName) {
            null -> {
                val tokenName = unnamedTokens.computeIfAbsent(literal) {
                    "Token${unnamedTokens.size}"
                }

                return createToken(tokenName, literal)
            }
            else -> Reference(ruleName)
        }
    }

    fun repeat(minTimes: Int = 0, fn: SequenceBuilder.() -> Unit): Repeat {
        val builder = SequenceBuilder(parentBuilder = this)
        fn(builder)
        return Repeat(minTimes, builder.build())
    }

    fun option(fn: SequenceBuilder.() -> Unit): Optional {
        val builder = SequenceBuilder(parentBuilder = this)
        fn(builder)
        return Optional(builder.build())
    }

    fun createToken(name: String, fn: SequenceBuilder.() -> Unit): Reference {
        if (wsToken == null) {
            runtimeException("should provide whitespace rule name to use token function")
        }
        val builder = SequenceBuilder(parentBuilder = null)
        fn(builder)
        builder.drop(Reference(wsToken))
        rules.add(Rule(name, Slice, listOf(NonConstructing(builder.build()))))
        return Reference(name)
    }
}

fun grammar(startToken: String, wsToken: String? = null, packageName: String? = null, fn: GrammarBuilder.() -> Unit): Grammar {
    val builder = GrammarBuilder(wsToken)
    fn(builder)
    return Grammar(startToken, builder.rules, packageName)
}

class VariantsBuilder(val parentBuilder: GrammarBuilder) {
    val variants = mutableListOf<Variant>()

    operator fun String.invoke(fn: ItemsBuilder.() -> Unit) {
        val builder = ItemsBuilder(parentBuilder)
        fn(builder)
        variants.add(Constructing(this, builder.items))
    }

    fun direct(fn: SequenceBuilder.() -> Unit) {
        val builder = SequenceBuilder(parentBuilder = parentBuilder)
        fn(builder)
        variants.add(NonConstructing(builder.build()))
    }
}

class ItemsBuilder(val parentBuilder: GrammarBuilder) {
    val items = mutableListOf<Item>()

    operator fun String.invoke(fn: SequenceBuilder.() -> Unit) {
        val builder = SequenceBuilder(parentBuilder = parentBuilder)
        fn(builder)
        items.add(KeepItem(this, builder.build()))
    }

    infix fun String.to(expr: Expr) {
        items.add(KeepItem(this, expr))
    }

    infix fun String.to(string: String) {
        items.add(KeepItem(this, Literal(string)))
    }

    fun drop(fn: SequenceBuilder.() -> Unit) {
        val builder = SequenceBuilder(parentBuilder = parentBuilder)
        fn(builder)
        items.add(DropItem(builder.build()))
    }

    fun drop(string: String) {
        items.add(DropItem(Literal(string)))
    }

    fun drop(expr: Expr) {
        items.add(DropItem(expr))
    }

    fun token(string: String) {
        drop(parentBuilder.createToken(string))
    }
}

data class SequenceItem(
        val keep: Boolean,
        val expr: Expr
)

class SequenceBuilder(val implicitKeep: Boolean = false, val parentBuilder: GrammarBuilder? = null) {
    val items = mutableListOf<SequenceItem>()

    fun build(): Expr {
        return when (items.size) {
            0 -> runtimeException("should add at least one item")
            1 -> {
                val only = items[0]
                if (only.keep || implicitKeep) {
                    only.expr
                } else {
                    runtimeException("should add at least one item to keep: $items")
                }
            }
            else -> {
                var keepIndex = -1
                items.forEachIndexed { i, item ->
                    if (item.keep) {
                        if (keepIndex != -1) {
                            runtimeException("only one item should be marked to keep")
                        }
                        keepIndex = i
                    }
                }

                Sequence(keepIndex, items.map { it.expr })
            }
        }
    }

    fun keep(expr: Expr) {
        items.add(SequenceItem(true, expr))
    }

    fun keep(string: String) {
        keep(Literal(string))
    }

    fun drop(expr: Expr) {
        items.add(SequenceItem(false, expr))
    }

    fun drop(string: String) {
        drop(Literal(string))
    }

    fun token(string: String) {
        if (parentBuilder == null) {
            throw RuntimeException("parentBuilder must be present to use 'token' function")
        }

        drop(parentBuilder.createToken(string))
    }
}