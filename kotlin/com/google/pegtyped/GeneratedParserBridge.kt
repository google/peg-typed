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

import com.google.pegtyped.runtime.None
import com.google.pegtyped.runtime.Some

fun convertGrammar(grammar: com.google.pegtyped.generated.Grammar): Grammar {
    return Grammar(grammar.startName.toString(), grammar.rules.map { convertRule(it) }, grammar.packageName.toString())
}

fun convertRule(rule: com.google.pegtyped.generated.Rule): Rule {
    val originalType = rule.type
    val type = when (originalType) {
        is Some -> {
            if (originalType.value.toString() == "slice") {
                Slice
            } else {
                Defined(originalType.value.toString())
            }
        }
        is None -> {
            if (rule.expansion is com.google.pegtyped.generated.SingleSlice) {
                Slice
            } else {
                Defined(rule.name.toString())
            }
        }
    }
    return Rule(rule.name.toString(), type, convertExpansion(rule.name.toString(), type, rule.expansion))
}

fun convertExpansion(name: String, type: Type, expansion: com.google.pegtyped.generated.RuleExpansion): List<Variant> {
    return when (expansion) {
        is com.google.pegtyped.generated.SingleConstructing -> listOf(Constructing(name, expansion.fields.map(::convertField)))
        is com.google.pegtyped.generated.SingleNonConstructing -> listOf(NonConstructing(convertExpr(expansion.expr)))
        is com.google.pegtyped.generated.Multiple -> expansion.variants.map(::convertVariant)
        is com.google.pegtyped.generated.SingleSlice -> {
            if (type != Slice) {
                throw RuntimeException("slice expansion should be used with slice type, not $type")
            }
            listOf(NonConstructing(convertExpr(expansion.expr)))
        }
    }
}

fun convertVariant(variant: com.google.pegtyped.generated.Variant): Variant {
    return when (variant) {
        is com.google.pegtyped.generated.NonConstructing -> {
            NonConstructing(convertExpr(variant.expr))
        }
        is com.google.pegtyped.generated.Constructing -> {
            Constructing(variant.name.toString(), variant.fields.map { convertField(it) })
        }
    }
}

fun convertField(field: com.google.pegtyped.generated.Field): Item {
    return when (field) {
        is com.google.pegtyped.generated.DropItem -> DropItem(convertExpr(field.expr))
        is com.google.pegtyped.generated.KeepItem -> KeepItem(field.name.toString(), convertExpr(field.expr))
    }
}

fun convertExpr(expr: com.google.pegtyped.generated.Expr): Expr {
    return when (expr) {
        is com.google.pegtyped.generated.Choices -> {
            if (expr.rest.isEmpty()) {
                convertExpr(expr.first)
            } else {
                Choice(cons(convertExpr(expr.first), expr.rest.map(::convertExpr)))
            }
        }
        is com.google.pegtyped.generated.Sequence -> {
            val builder = SequenceBuilder(true)

            for (item in expr.items) {
                when (item) {
                    is com.google.pegtyped.generated.Keep -> {
                        builder.keep(convertExpr(item.inner))
                    }
                    is com.google.pegtyped.generated.Drop -> {
                        builder.drop(convertExpr(item.inner))
                    }
                }
            }

            builder.build()
        }
        is com.google.pegtyped.generated.Literal -> Literal(expr.chars.toString()) // TODO: this would probably require some unescaping
        is com.google.pegtyped.generated.Negate -> Negate(convertExpr(expr.inner))
        is com.google.pegtyped.generated.Repeat -> Repeat(convertExpr(expr.inner))
        is com.google.pegtyped.generated.Repeat1 -> Repeat(1, convertExpr(expr.inner))
        is com.google.pegtyped.generated.CharRange -> {
            // TODO: Clean up unnecessary .toString() conversions
            if (expr.from.toString().length != 1 || expr.to.toString().length != 1) {
                throw RuntimeException("internal error: invalid CharRange")
            }
            CharRange(expr.from.toString()[0], expr.to.toString()[0])
        }
        is com.google.pegtyped.generated.AnyChar -> AnyChar
        is com.google.pegtyped.generated.Reference -> Reference(expr.target.toString())
        is com.google.pegtyped.generated.Optional -> Optional(convertExpr(expr.inner))
        is com.google.pegtyped.generated.ToString -> AsString(convertExpr(expr.inner))
    }
}
