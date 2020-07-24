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

import com.google.pegtyped.runtime.ParseError
import java.io.File

fun main(args: Array<String>) {
    val content = File(args[0]).readText()
    val highlighter = ErrorHighlighter(content)
    val grammar: Grammar
    try {
        grammar = convertGrammar(com.google.pegtyped.generated.Parser(content).parse())
    } catch (e: ParseError) {
        println(highlighter.renderHighlight(e.pos))
        throw e
    }

    for (rule in grammar.rules) {
        println(rule)
    }

    writeGrammar(args[1], grammar)
}
