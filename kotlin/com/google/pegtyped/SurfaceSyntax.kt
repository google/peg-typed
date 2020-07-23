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

val grammarGrammar = grammar("Grammar", "WS", "com.google.pegtyped.generated") {
    val grammar = Reference("Grammar")
    val ws = Reference("WS")
    val wsChar = Reference("WSChar")
    val id = token("ID") {
        keep(AsString(Repeat(1, Choice(listOf(
                CharRange('a', 'z'),
                CharRange('A', 'Z'))))))
    }
    val rule = Reference("Rule")
    val variant = Reference("Variant")
    val sequenceItem = Reference("SequenceItem")
    val realSequenceItem = Reference("RealSequenceItem")
    val expr = Reference("Expr")
    val field = Reference("Field")
    val sequence = Reference("Sequence")
    val ruleExpansion = Reference("RuleExpansion")

    grammar.single {
        drop(token("package"))
        "packageName" to id
        drop(token(";"))
        drop(token("start"))
        "startName" to id
        drop(token(";"))
        "rules" to Repeat(rule)
    }

    rule.single {
        "name" to id
        "type" to option {
            drop(token(":"))
            keep(id)
        }
        "expansion" to ruleExpansion
        drop(token(";"))
    }

    ruleExpansion.variants {
        "SingleConstructing" {
            drop(token("="))
            drop(token("{"))
            "fields" to Repeat(field)
            drop(token("}"))
        }
        "SingleNonConstructing" {
            drop(token("="))
            drop(token("direct"))
            "expr" to expr
        }
        "SingleSlice" {
            drop(token(":="))
            "expr" to expr
        }
        "Multiple" {
            "variants" to Repeat(variant)
        }
    }

    variant.variants {
        "NonConstructing" {
            drop(token("|"))
            drop(token("direct"))
            "expr" to expr
        }
        "Constructing" {
            drop(token("|"))
            "name" to id
            drop(token("{"))
            "fields" to Repeat(field)
            drop(token("}"))
        }
    }

    field.variants {
        "DropItem" {
            drop(token("drop"))
            "expr" to expr
            drop(token(";"))
        }
        "KeepItem" {
            "name" to id
            drop(token(":"))
            "expr" to expr
            drop(token(";"))
        }
    }

    expr.variants {
        "Choices" {
            "first" to sequence
            "rest" to repeat {
                drop(token("/"))
                keep(sequence)
            }
        }
    }

    sequence.single(expr) {
        "items" to Repeat(1, sequenceItem)
    }

    sequenceItem.variants {
        "Keep" {
            drop(token("#"))
            "inner" to realSequenceItem
        }
        "Drop" {
            "inner" to realSequenceItem
        }
    }

    realSequenceItem.variants(expr) {
        "Literal" {
            drop("\"") // no token here, because we do not want to drop whitespace inside string literal
            "chars" to AsString(repeat {
                drop(Negate(Literal("\"")))
                keep(AnyChar)
            })
            drop(token("\""))
        }
        "Negate" {
            drop(token("!"))
            "inner" to realSequenceItem
        }
        direct {
            drop(token("("))
            keep(expr)
            drop(token(")"))
        }
        "Repeat" {
            drop(token("*"))
            "inner" to realSequenceItem
        }
        "Repeat1" {
            drop(token("+"))
            "inner" to realSequenceItem
        }
        "CharRange" {
            drop(token("["))
            "from" to AnyChar
            drop(token("-"))
            "to" to AnyChar
            drop(token("]"))
        }
        "AnyChar" {
            // TODO: Implement support for object cases to avoid this dummy token
            "dummy" to token(".")
        }
        "Optional" {
            drop(token("?"))
            "inner" to realSequenceItem
        }
        "ToString" {
            drop(token("^"))
            "inner" to realSequenceItem
        }
        "Reference" {
            "target" to id
        }
    }

    wsChar.variants(Slice) {
        direct { keep(" ") }
        direct { keep("\n") }
    }

    ws.alias(Slice) {
        keep(AsString(Repeat(wsChar)))
    }
}