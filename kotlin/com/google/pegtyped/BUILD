package(default_visibility = ["//visibility:public"])

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library")

kt_jvm_library(
    name = "peg_typed",
    srcs = [
        "CodeGenerator.kt",
        "Core.kt",
        "ErrorHighlighter.kt",
        "GrammarBuilder.kt",
        "SurfaceSyntax.kt",
        "Utils.kt",
    ],
)

kt_jvm_library(
    name = "surface_syntax",
    srcs = [
        "GeneratedParserBridge.kt",
    ],
    deps = [
        ":peg_typed",
        "//kotlin/com/google/pegtyped/generated",
        "//kotlin/com/google/pegtyped/runtime",
    ],
)

kt_jvm_library(
    name = "grammar_parser_generator_library",
    srcs = [
        "GenerateGrammarParser.kt",
    ],
    deps = [
        ":peg_typed",
    ],
)

java_binary(
    name = "grammar_parser_generator",
    main_class = "com.google.pegtyped.GenerateGrammarParserKt",
    runtime_deps = [
        ":grammar_parser_generator_library",
    ],
)

kt_jvm_library(
    name = "parser_generator_library",
    srcs = ["GenerateParser.kt"],
    deps = [
        ":peg_typed",
        ":surface_syntax",
        "//kotlin/com/google/pegtyped/runtime",
    ],
)

java_binary(
    name = "parser_generator",
    main_class = "com.google.pegtyped.GenerateParserKt",
    runtime_deps = [":parser_generator_library"],
)
