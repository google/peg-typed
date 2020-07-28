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

package com.google.pegtyped.imp

import imp.*

val tests = listOf(
        """
            count := 10;
            curr := 0;
            next := 1;
            while (count != 0) {
              print curr;
              temp := curr;
              curr := next;
              next := next + temp;
              count := count - 1;
            }
        """.trimIndent()
)

fun main() {
    for (test in tests) {
        val expr = Parser(test).parse()
        println(expr)
        Evaluator().eval(expr)
    }
}

fun simplify(e: Expr): Expr {
    return when (e) {
        is Sum -> {
            if (e.rest.isEmpty()) {
                simplify(e.first)
            } else {
                Sum(simplify(e.first), e.rest.map(::simplify))
            }
        }
        is Prod -> {
            if (e.rest.isEmpty()) {
                simplify(e.first)
            } else {
                Prod(simplify(e.first), e.rest.map(::simplify))
            }
        }
        is Literal -> e
        is Reference -> e
    }
}

fun simplify(e: SumCont): SumCont {
    return SumCont(e.op, simplify(e.expr))
}

fun simplify(e: ProdCont): ProdCont {
    return ProdCont(e.op, simplify(e.expr))
}

class Evaluator() {
    val env = mutableMapOf<String, Int>()

    fun eval(e: Program) {
        for (stmt in e.stmts) {
            eval(stmt)
        }
    }

    fun eval(e: Stmt) {
        when (e) {
            is Print -> println(eval(e.e))
            is If -> {
                if (eval(e.cond)) {
                    eval(e.block)
                }
            }
            is While -> {
                while (eval(e.cond)) {
                    eval(e.block)
                }
            }
            is Assign -> {
                env[e.dest.toString()] = eval(e.expr)
            }
        }
    }

    fun eval(e: Cond): Boolean {
        val lhs = eval(e.lhs)
        val rhs = eval(e.rhs)
        return when (e.op.toString()) {
            "=" -> lhs == rhs
            "!=" -> lhs != rhs
            else -> throw RuntimeException("unknown operator ${e.op}")
        }
    }

    fun eval(e: Expr): Int {
        return when (e) {
            is Sum -> {
                var result = eval(e.first)
                for (cont in e.rest) {
                    when (cont.op.toString()) {
                        "+" -> result += eval(cont.expr)
                        "-" -> result -= eval(cont.expr)
                        else -> throw RuntimeException("unknown operator ${cont.op}")
                    }
                }
                result
            }
            is Prod -> {
                var result = eval(e.first)
                for (cont in e.rest) {
                    when (cont.op.toString()) {
                        "*" -> result *= eval(cont.expr)
                        "/" -> result /= eval(cont.expr)
                        else -> throw RuntimeException("unknown operator ${cont.op}")
                    }
                }
                result
            }
            is Literal -> e.literal.toString().toInt()
            is Reference -> env[e.target.toString()] ?: throw RuntimeException("unknown variable ${e.target.toString()}")
        }
    }
}