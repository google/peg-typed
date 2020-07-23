package(default_visibility = ["//visibility:public"])

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

kt_jvm_library(
    name = "generated",
    srcs = [
        ":grammar_parser",
    ],
)

genrule(
    name = "grammar_parser",
    outs = ["Parser.kt"],
    cmd = "'$(location //kotlin/com/google/pegtyped:grammar_parser_generator)' $@",
    tools = ["//kotlin/com/google/pegtyped:grammar_parser_generator"],
)
