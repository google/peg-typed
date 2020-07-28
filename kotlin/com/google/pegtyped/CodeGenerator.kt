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

import java.io.Writer

val prePackageHeader = """
    @file:Suppress("NAME_SHADOWING", "ConvertTwoComparisonsToRangeCheck", "MemberVisibilityCanBePrivate", "UNUSED_VARIABLE", "unused", "UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UnnecessaryVariable", "ConvertToStringTemplate")
    
""".trimIndent()

val postPackageHeader = """
    import com.google.pegtyped.runtime.ParseError
    import com.google.pegtyped.runtime.Option
    import com.google.pegtyped.runtime.Some
    import com.google.pegtyped.runtime.None
    import com.google.pegtyped.runtime.LocationInfo

""".trimIndent()

val parserClassHeader = """
    var pos = 0
    var furthestErrorPos = 0
    fun error(errorPos: Int): Nothing {
        furthestErrorPos = kotlin.math.max(furthestErrorPos, errorPos)
        throw ParseError(errorPos)
    }
"""

fun rootMethodBody(startRule: String): String = """
        val result = parse$startRule()
        if (pos < source.length) {
            throw ParseError(kotlin.math.max(pos, furthestErrorPos))
        }
        return result
"""

class CodeGenerator(val indentWidth: Int, val grammar: Grammar) {
    var indentLevel = 0
    val builder = StringBuilder()
    var lineStarted = false
    var variableNo = 0

    fun fresh(): String {
        return "a${variableNo++}"
    }

    fun withIndent(line: String, closure: String = "}", fn: CodeGenerator.() -> Unit) {
        writeLine(line)
        withIndent(fn)
        writeLine(closure)
    }

    fun withIndent(fn: CodeGenerator.() -> Unit) {
        indentLevel++
        fn()
        indentLevel--
    }

    private fun startLine() {
        if (!lineStarted) {
            for (i in 0 until indentLevel * indentWidth) {
                builder.append(' ')
            }
            lineStarted = true
        }
    }

    fun writeDirect(str: String) {
        builder.append(str)
    }

    fun writeLine(line: String) {
        startLine()
        builder.append(line)
        builder.append('\n')
        lineStarted = false
    }

    fun write(line: String) {
        startLine()
        builder.append(line)
    }

    fun generateKotlinClasses(): String {
        val result = StringBuilder()

        val rulesByType = mutableMapOf<String, MutableList<Constructing>>()
        for (rule in grammar.rules) {
            for (variant in rule.expansion) {
                if (variant is Constructing) {
                    val typeName = when (rule.type) {
                        is Defined -> rule.type.name
                        Slice -> throw RuntimeException("constructing variant should not be in rule of Slice type")
                    }
                    rulesByType.computeIfAbsent(typeName) { mutableListOf() }.add(variant)
                }
            }
        }

        for ((type, rules) in rulesByType.entries) {
            val sameName = rules.firstOrNull { it.name == type }
            if (sameName != null && rules.size > 1) {
                throw RuntimeException("should be only one constructing variant with the same name")
            }

            if (sameName != null) {
                result.append("data class ${type}(")
                generateFields(sameName, result)
                result.append(")\n")
                continue
            }

            result.append("sealed class ${type}\n")
            for (rule in rules) {
                result.append("data class ${rule.name}(")
                generateFields(rule, result)
                result.append("): $type()\n")
            }
        }

        return result.toString()
    }

    fun escape(string: String): String {
        return "\"${string.replace("\n", "\\n").replace("\"", "\\\"")}\""
    }

    fun generateExpr(expr: Expr): String {
        return when (expr) {
            is Literal -> {
                val result = fresh()
                writeLine("val $result: LocationInfo")
                val str = escape(expr.s)
                withIndent("if (source.startsWith($str, pos)) {") {
                    writeLine("$result = LocationInfo(source, pos, pos + $str.length)")
                    writeLine("pos += $str.length")
                }
                withIndent("else {") {
                    writeLine("error(pos)")
                }
                result
            }
            is CharRange -> {
                val result = fresh()
                writeLine("val $result: LocationInfo")
                withIndent("if (pos >= source.length) {") {
                    writeLine("error(pos)")
                }
                withIndent("if (source[pos] >= '${expr.from}' && source[pos] <= '${expr.to}') {") {
                    writeLine("$result = LocationInfo(source, pos, pos + 1)")
                    writeLine("pos++")
                }
                withIndent("else {") {
                    writeLine("error(pos)")
                }
                result
            }
            is Reference -> {
                val result = fresh()
                writeLine("val $result = parse${expr.target}()")
                result
            }
            is Repeat -> {
                val result = fresh()
                writeLine("val $result = mutableListOf<${inferType(expr.item)}>()")
                withIndent("while (true) {") {
                    val savedPos = fresh()
                    writeLine("val $savedPos = pos")
                    withIndent("try {") {
                        val item = generateExpr(expr.item)
                        writeLine("$result.add($item)")
                    }
                    withIndent("catch (e: ParseError) {") {
                        writeLine("pos = $savedPos")
                        writeLine("break")
                    }
                }
                withIndent("if (${result}.size < ${expr.minCount}) {") {
                    writeLine("error(pos)")
                }
                result
            }
            is Sequence -> {
                val items = expr.children.map { generateExpr(it) }
                if (!items.indices.contains(expr.keepIndex)) {
                    throw RuntimeException("illegal keepIndex: $expr")
                }
                items[expr.keepIndex]
            }
            is AsString -> {
                writeLine("val startPos = pos")
                generateExpr(expr.inner)
                val result = fresh()
                writeLine("val $result = LocationInfo(source, startPos, pos)")
                result
            }
            AnyChar -> {
                val result = fresh()
                withIndent("if (pos >= source.length) {") {
                    writeLine("error(pos)")
                }
                writeLine("val $result = LocationInfo(source, pos, pos + 1)")
                writeLine("pos++")
                result
            }
            is Negate -> {
                val result = fresh()
                writeLine("val startPos = pos")
                writeLine("var success = false")
                withIndent("try {") {
                    generateExpr(expr.inner)
                }
                withIndent("catch (e: ParseError) {") {
                    writeLine("success = true")
                    writeLine("pos = startPos")
                }
                withIndent("if (!success) {") {
                    writeLine("error(pos)")
                }
                writeLine("val $result: Any? = null")
                result
            }
            is Choice -> {
                val savedPos = fresh()
                val error = fresh()
                writeLine("val $savedPos = pos")
                writeLine("var $error: ParseError? = null")

                val intermediateResult = fresh()
                val type = inferType(expr)
                writeLine("var $intermediateResult: $type? = null")

                for (variant in expr.options) {
                    withIndent("if ($intermediateResult == null) {") {
                        withIndent("try {") {
                            writeLine("pos = $savedPos")
                            val variantLabel = generateExpr(variant)
                            writeLine("$intermediateResult = $variantLabel")
                        }
                        withIndent("catch (e: ParseError) {") {
                            writeLine("$error = $error?.ingest(e) ?: e")
                        }
                    }
                }

                val result = fresh()
                writeLine("val $result: $type")
                withIndent("if ($intermediateResult == null) {") {
                    withIndent("if ($error == null) {") {
                        writeLine("throw RuntimeException(\"internal failure: error should not be null\")")
                    }
                    writeLine("throw $error")
                }
                writeLine("$result = $intermediateResult")
                result
            }
            is Optional -> {
                val savedPos = fresh()
                val result = fresh()
                writeLine("val $savedPos = pos")
                writeLine("var $result: ${inferType(expr)} = None")

                withIndent("try {") {
                    val inner = generateExpr(expr.inner)
                    writeLine("$result = Some($inner)")
                }
                withIndent("catch (e: ParseError) {") {
                    writeLine("pos = $savedPos")
                }

                result
            }
        }
    }

    fun generateItem(item: Item, sink: MutableList<String>) {
        when (item) {
            is KeepItem -> {
                sink.add(generateExpr(item.expr))
            }
            is DropItem -> {
                generateExpr(item.expr)
            }
        }
    }

    fun generateVariant(variant: Variant) {
        when (variant) {
            is Constructing -> {
                val names = mutableListOf<String>()

                for (item in variant.items) {
                    generateItem(item, names)
                }

                writeLine("return ${variant.name}(${names.joinToString()})")
            }
            is NonConstructing -> {
                val name = generateExpr(variant.expr)
                writeLine("return $name")
            }
        }
    }

    fun generateKotlinCode(): String {
        withIndent("class Parser(val source: String) {") {
            writeDirect(parserClassHeader)

            withIndent("fun parse(): ${grammar.startRule.type.kotlinName()} {") {
                writeDirect(rootMethodBody(grammar.startName))
            }

            for (rule in grammar.rules) {
                val multiple = rule.expansion.size > 1
                withIndent("fun parse${rule.name}(): ${rule.type.kotlinName()} {") {
                    val savedPos = fresh()
                    val error = fresh()
                    if (multiple) {
                        writeLine("val $savedPos = pos")
                        writeLine("var $error: ParseError? = null")
                    }

                    for (variant in rule.expansion) {
                        if (multiple) {
                            withIndent("try {") {
                                writeLine("pos = $savedPos")
                                generateVariant(variant)
                            }
                            withIndent("catch (e: ParseError) {") {
                                writeLine("$error = $error?.ingest(e) ?: e")
                            }
                        } else {
                            generateVariant(variant)
                        }
                    }

                    if (multiple) {
                        withIndent("if ($error == null) {") {
                            writeLine("throw RuntimeException(\"internal failure: error should not be null\")")
                        }
                        writeLine("throw $error")
                    }
                }
            }
        }

        return builder.toString()
    }

    fun inferType(expr: Expr): String {
        return when (expr) {
            is Literal -> "LocationInfo"
            is CharRange -> "LocationInfo"
            is Reference -> {
                val rule = grammar.ruleByName[expr.target] ?: throw RuntimeException("unknown rule ${expr.target}")
                rule.type.kotlinName()
            }
            is Repeat -> "List<${inferType(expr.item)}>"
            is Sequence -> inferType(expr.children[expr.keepIndex])
            is AsString -> "LocationInfo"
            AnyChar -> "LocationInfo"
            is Negate -> "Any"
            is Choice -> {
                if (expr.options.isEmpty()) {
                    throw RuntimeException("choice should not be empty")
                }

                // TODO: maybe do a more proper type checking pass at some point
                return inferType(expr.options[0])
            }
            is Optional -> "Option<${inferType(expr.inner)}>"
        }
    }

    fun generateFields(direct: Constructing, sink: StringBuilder) {
        var first = true
        for (item in direct.items) {
            when (item) {
                is KeepItem -> {
                    if (!first) {
                        sink.append(", ")
                    }
                    first = false
                    sink.append("val ${item.name}: ${inferType(item.expr)}")
                }
                is DropItem -> {
                    // No field needed
                }
            }
        }
    }

    fun writeAll(writer: Writer) {
        writer.write(prePackageHeader)

        if (grammar.packageName != null) {
            writer.write("package ${grammar.packageName}\n")
        }

        writer.write(postPackageHeader)
        writer.write(generateKotlinClasses())
        writer.write(generateKotlinCode())
    }
}

