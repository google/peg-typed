load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library")

def peg_parser(name, grammar_source):
    source_rule = name + "_src"
    native.genrule(
        name = source_rule,
        outs = ["Parser.kt"],
        tools = [
            "//kotlin/com/google/pegtyped:parser_generator",
        ],
        srcs = [grammar_source],
        cmd = "'$(location //kotlin/com/google/pegtyped:parser_generator)' $(location %s) $@" % grammar_source,
    )

    kt_jvm_library(
        name = name,
        srcs = [
            source_rule,
        ],
    )
