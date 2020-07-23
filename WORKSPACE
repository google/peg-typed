load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

rules_kotlin_version = "legacy-1.3.0"

rules_kotlin_sha = "4fd769fb0db5d3c6240df8a9500515775101964eebdf85a3f9f0511130885fde"

http_archive(
    name = "io_bazel_rules_kotlin",
    sha256 = rules_kotlin_sha,
    strip_prefix = "rules_kotlin-%s" % rules_kotlin_version,
    type = "zip",
    urls = ["https://github.com/bazelbuild/rules_kotlin/archive/%s.zip" % rules_kotlin_version],
)

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")

kotlin_repositories()

kt_register_toolchains()
