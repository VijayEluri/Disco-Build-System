#!/bin/bash -e

#
# This shell script sets up the initial test environment for all the tests in the test-*
# sub-directories. It is executed once per test-* directory.
#

rm -f build.bml
bmladmin create
bmladmin scan-tree $TEST_SRC/../build-tree

