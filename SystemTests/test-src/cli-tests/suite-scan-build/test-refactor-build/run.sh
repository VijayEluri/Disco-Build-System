#!/bin/bash -e

. $TEST_SRC/../functions.sh

# build the small test program
make -C $TEST_SRC/../examples/small-c-makefile clean >/dev/null 2>&1
bmladmin scan-build make -C $TEST_SRC/../examples/small-c-makefile all >/dev/null 2>&1

# make all the gcc commands atomic
bmladmin make-atomic 3
bmladmin make-atomic 6
bmladmin make-atomic 11
bmladmin make-atomic 14
bmladmin make-atomic 20

# remove unnecessary actions
bmladmin rm-action 1
bmladmin rm-action 2
bmladmin rm-action 10
bmladmin rm-action 18
bmladmin rm-action 19

# merge some actions
bmladmin merge-actions 3:6
bmladmin merge-actions 11:14

# Test the gcc lines - should be actions 3 and 11
match "bmladmin show-actions -f 3" "-c file1.c"
match "bmladmin show-actions -f 3" "-c file2.c"
match "bmladmin show-actions -f 11" "-c file3.c"
match "bmladmin show-actions -f 11" "-c file4.c"

# Test the ar lines - should be 9 and 17
match "bmladmin show-actions -f 9" "ar cq lib1.a file1.o file2.o"
match "bmladmin show-actions -f 17" "ar cq lib2.a file3.o file4.o"

# Test the final link line - should be action 20
match "bmladmin show-actions -f 20" "-o prog main.c subdir1/lib1.a subdir2/lib2.a"

# make sure the delete actions are truly gone
match_empty "bmladmin show-actions -f 1"
match_empty "bmladmin show-actions -f 2"
match_empty "bmladmin show-actions -f 10"
match_empty "bmladmin show-actions -f 18"
match_empty "bmladmin show-actions -f 19"

