#!/bin/bash -e

#
# Test assigning packages to actions, after using a scan-ea-anno command to build a build.bml file.
#

echo "Show all actions, with only default packages"
bmladmin show-actions -p

echo
echo "Create a new package"
bmladmin add-pkg zlib

echo
echo "Add actions 18-32 to the package"
bmladmin set-action-pkg zlib 18:19:20:21:22:23:24:25
bmladmin set-action-pkg zlib 26:27:28:29:30:31:32

echo
echo "Show all actions, with their packages"
bmladmin show-actions -p

echo
echo "Show only those actions in the zlib package"
bmladmin show-actions -p -f %p/zlib

echo
echo "Show only those actions outside the zlib package"
bmladmin show-actions -p -f %np/zlib

echo
echo "Try to delete the zlib package (should fail)"
set +e
bmladmin rm-pkg zlib
echo Status is $?
set -e

echo
echo "Set the package of all files (under action 2) to be <import>"
bmladmin set-action-pkg "<import>" 2/

echo
echo "Show only those actions in the zlib package (empty)"
bmladmin show-actions -p -f %p/zlib

echo
echo "Try again to delete the zlib package (should succeed)"
bmladmin rm-pkg zlib
