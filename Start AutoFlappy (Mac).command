#!/bin/bash
cd "$(dirname "$0")"
if ! java -version >/dev/null 2>&1; then
    echo "Java is not installed yet."
    echo "Opening the download page: install it, then double-click this file again."
    open https://adoptium.net/
    read -r -p "Press Enter to close."
    exit 1
fi
java -jar AutoFlappy.jar
