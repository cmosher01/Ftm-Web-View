#!/bin/sh -e
readlinkf() { perl -MCwd -e 'print Cwd::abs_path shift' "$1" ; }
sqlite3 -version
me="$(readlinkf "$0")"
here="$(dirname "$me")"
cd "$here"
cat TestTree.ftm.sqlite.db.sql | sqlite3 TestTree.ftm
ls -l
