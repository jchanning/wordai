#!/bin/bash
# Extract dictionary files from JAR for cloud deployment to Oracle Cloud Infrastructure (OCI)
# This script should be run on the server after uploading the JAR

echo "=== Setting up WordAI Dictionaries ==="
echo ""

# Ensure we're in the app directory
cd ~/wordai-app || exit 1

# Create dictionaries directory
echo "Creating dictionaries directory..."
mkdir -p dictionaries

# Extract dictionaries from JAR
echo "Extracting dictionaries from JAR..."
jar xf wordai-1.0-SNAPSHOT.jar BOOT-INF/classes/dictionaries/4_letter_words.txt
jar xf wordai-1.0-SNAPSHOT.jar BOOT-INF/classes/dictionaries/5_letter_words.txt
jar xf wordai-1.0-SNAPSHOT.jar BOOT-INF/classes/dictionaries/6_letter_words.txt

# Move to dictionaries folder
echo "Moving dictionaries to dictionaries/ folder..."
mv BOOT-INF/classes/dictionaries/*.txt dictionaries/

# Clean up
echo "Cleaning up temporary files..."
rm -rf BOOT-INF

# Verify
echo ""
echo "Dictionary files:"
ls -lh dictionaries/

echo ""
echo "✓ Dictionary setup complete!"
