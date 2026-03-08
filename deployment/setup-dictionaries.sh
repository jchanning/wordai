#!/bin/bash
# Extract dictionary files from JAR for cloud deployment to Oracle Cloud Infrastructure (OCI)
# This script should be run on the server after uploading the JAR

set -euo pipefail

echo "=== Setting up WordAI Dictionaries ==="
echo ""

# Ensure we're in the app directory
cd ~/wordai-app || exit 1

ARCHIVE="wordai.jar"
if [ ! -f "$ARCHIVE" ]; then
	ARCHIVE=$(find . -maxdepth 1 -type f -name 'wordai-*.jar' | head -n 1)
fi

if [ -z "${ARCHIVE:-}" ] || [ ! -f "$ARCHIVE" ]; then
	echo "ERROR: Could not find a WordAI JAR in ~/wordai-app" >&2
	exit 1
fi

# Create dictionaries directory
echo "Creating dictionaries directory..."
mkdir -p dictionaries

# Extract dictionaries from JAR
echo "Extracting dictionaries from JAR..."
rm -f dictionaries/*.txt

if command -v unzip >/dev/null 2>&1; then
	unzip -oq "$ARCHIVE" 'BOOT-INF/classes/dictionaries/*.txt' -d .
elif command -v jar >/dev/null 2>&1; then
	jar xf "$ARCHIVE" BOOT-INF/classes/dictionaries/4_letter_words.txt
	jar xf "$ARCHIVE" BOOT-INF/classes/dictionaries/5_letter_words.txt
	jar xf "$ARCHIVE" BOOT-INF/classes/dictionaries/6_letter_words.txt
	if jar tf "$ARCHIVE" | grep -q 'BOOT-INF/classes/dictionaries/7_letter_words.txt'; then
		jar xf "$ARCHIVE" BOOT-INF/classes/dictionaries/7_letter_words.txt
	fi
else
	echo "ERROR: Neither unzip nor jar is available to extract dictionaries" >&2
	exit 1
fi

# Move to dictionaries folder
echo "Moving dictionaries to dictionaries/ folder..."
if compgen -G 'BOOT-INF/classes/dictionaries/*.txt' > /dev/null; then
	mv BOOT-INF/classes/dictionaries/*.txt dictionaries/
fi

# Clean up
echo "Cleaning up temporary files..."
rm -rf BOOT-INF

# Verify
echo ""
echo "Dictionary files:"
ls -lh dictionaries/

echo ""
echo "✓ Dictionary setup complete!"
