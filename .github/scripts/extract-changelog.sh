#!/usr/bin/env bash
# Extract a single version's section from CHANGELOG.md.
# Prints the body of the section (without the heading line) to stdout.
#
# Usage:
#   extract-changelog.sh <VERSION>
#   extract-changelog.sh 0.2.0
#
# The script looks for a heading of the form:
#   ## [0.2.0] ...
# or (for the unreleased section):
#   ## [Unreleased]
# and prints everything up to the next ## heading.
set -euo pipefail

VERSION="${1:?Usage: extract-changelog.sh <VERSION>}"

awk \
  "/^## \[${VERSION}\]/{found=1; next} \
   found && /^## /{exit} \
   found{print}" \
  CHANGELOG.md
