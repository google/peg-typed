load("//kotlin/com/google/pegtyped:peg.bzl", "peg_parser")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library")

peg_parser(
    name = "imp_parser",
    grammar_source = "imp.peg",
)

kt_jvm_binary(
    name = "imp",
    srcs = ["Main.kt"],
    main_class = "com.google.pegtyped.imp.MainKt",
    deps = [":imp_parser"],
)
