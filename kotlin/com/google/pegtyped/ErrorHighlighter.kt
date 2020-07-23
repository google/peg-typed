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

class ErrorHighlighter(val source: String) {
    val lineStartPos = mutableListOf(0)

    init {
        if (source.isEmpty()) {
            throw RuntimeException("what are you going to search for in empty string?")
        }
        for (i in source.indices) {
            if (source[i] == '\n') {
                lineStartPos.add(i + 1)
            }
        }
        if (lineStartPos.last() != source.length) {
            lineStartPos.add(source.length)
        }
    }

    private fun lineIndex(pos: Int): Int {
        if (pos < 0 || pos >= source.length) {
            throw RuntimeException("wrong $pos: should be in [0; ${source.length})")
        }

        var result = 0
        while (lineStartPos[result + 1] <= pos) {
            result++
        }

        return result
    }

    fun renderHighlight(pos: Int): String {
        val lineNo = lineIndex(pos)
        val line = source.substring(lineStartPos[lineNo], lineStartPos[lineNo + 1])
        val prefix = "${lineNo + 1} | "
        val highlight = StringBuilder(prefix)
        highlight.append(line)
        for (i in 0 until pos - lineStartPos[lineNo] + prefix.length) {
            highlight.append(' ')
        }
        highlight.append("^\n")
        return highlight.toString()
    }
}