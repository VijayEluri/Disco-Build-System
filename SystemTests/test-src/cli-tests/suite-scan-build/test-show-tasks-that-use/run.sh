#!/bin/bash -e

. $TEST_SRC/../functions.sh

# build the small test program
disco scan-build make -f $TEST_SRC/Makefile >/dev/null 2>&1

# the find command should not be shown in this output,
# since it doesn't access "myfile". This only happened
# because of bug #19 where it the show-tasks-that-use
# command was include parent paths too.
not_match "disco show-tasks-that-use myfile" "find"