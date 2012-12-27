#!/bin/bash -e

#
# This shell script sets up the initial test environment for all the tests in the test-*
# sub-directories. It is executed once per test-* directory.
#

rm -f build.bml
bmladmin scan-ea-anno $TEST_SRC/../emake.xml

