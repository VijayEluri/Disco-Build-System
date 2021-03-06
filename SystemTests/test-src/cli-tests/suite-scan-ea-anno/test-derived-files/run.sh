#!/bin/bash -e

#
# Test commands that show the "derived" relationship between files, after using a scan-ea-anno 
# command to build a build.bml file.
#

echo
echo "Show files immediately derived from trees.c"
bmladmin show-derived-files trees.c

echo
echo "Show all files derived from tree.c"
bmladmin show-derived-files -a trees.c

echo
echo "Show all files derived from config.h"
bmladmin show-derived-files -a config.h

echo
echo "Show all .a files derived from config.h"
bmladmin show-derived-files -a -f "*.a" config.h

echo
echo "Show all files derived from zlib.c"
bmladmin show-derived-files -a zlib.c

echo
echo "Show all files derived from zlib.c (using full path name)"
bmladmin show-derived-files -a /home/psmith/t/cvs-1.11.23/src/zlib.c

echo
echo "Show files used to create zlib.h (none)"
bmladmin show-input-files zlib.h

echo
echo "Show files used to create zlib.o"
bmladmin show-input-files zlib.o

echo
echo "Show files directly used to create libz.a"
bmladmin show-input-files libz.a

echo
echo "Show all files used to create libz.a"
bmladmin show-input-files -a libz.a

echo
echo "Show all .h files used to create libz.a"
bmladmin show-input-files -a -f "*.h" libz.a

echo
echo "Show all .h files used to create libz.a (using full path name)"
bmladmin show-input-files -a -f "*.h" /home/psmith/t/cvs-1.11.23/zlib/libz.a

echo
echo "Show actions that use zlib.o"
bmladmin show-actions-that-use zlib.o

echo
echo "Show actions that read zlib.o"
bmladmin show-actions-that-use --read zlib.o

echo
echo "Show actions that write zlib.o"
bmladmin show-actions-that-use --write zlib.o

echo
echo "Show actions that use trees.c"
bmladmin show-actions-that-use trees.c

echo
echo "Show actions that read trees.c"
bmladmin show-actions-that-use -r trees.c

echo
echo "Show actions that write trees.c (none)"
bmladmin show-actions-that-use -w trees.c

echo
echo "Show actions that use libdiff.a"
bmladmin show-actions-that-use libdiff.a

echo
echo "Show actions that read libdiff.a"
bmladmin show-actions-that-use -r libdiff.a

echo
echo "Show actions that write libdiff.a"
bmladmin show-actions-that-use -w libdiff.a

echo
echo "Show files used by action 1 (none)"
bmladmin show-files-used-by 1

echo
echo "Show files used by action 4"
bmladmin show-files-used-by 4

echo
echo "Show files read by action 4"
bmladmin show-files-used-by -r 4

echo
echo "Show files written by action 4"
bmladmin show-files-used-by -w 4

echo
echo "Show files used by action 4, 5 or 6"
bmladmin show-files-used-by 4:5:6

echo
echo "Show files written by action 2's children"
bmladmin show-files-used-by -w 2/

echo
echo "Show .h files read by action 2's children"
bmladmin show-files-used-by --read -f "*.h" 2/

echo
echo "Show .o files read by action 102"
bmladmin show-files-used-by --read -f "*.o" 102

echo
echo "Show .o files read by action 102, with packages"
bmladmin show-files-used-by --read -f "*.o" -p 102
