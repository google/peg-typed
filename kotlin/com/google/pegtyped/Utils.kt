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

fun <T, S> List<T>.indexBy(fn: (T) -> S): Map<S, List<T>> {
    val result = mutableMapOf<S, MutableList<T>>()

    for (item in this) {
        result.computeIfAbsent(fn(item)) { mutableListOf() }.add(item)
    }

    return result
}

fun Any.runtimeException(message: String): Nothing {
    throw RuntimeException("${this.javaClass.name}: $message")
}

fun <T> cons(head: T, tail: List<T>): List<T> {
    val result = mutableListOf(head)
    result.addAll(tail)
    return result
}
