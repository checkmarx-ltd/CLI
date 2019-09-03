#!/bin/bash
cd "$(dirname "$0")"
java -Xmx1024m -jar cx_console.jar "$@"