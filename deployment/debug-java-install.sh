#!/bin/bash
# Script to debug and install Java on Oracle Linux 8 (OCI Free Tier)

echo "=== System Information ==="
cat /etc/os-release
echo ""

echo "=== Memory Status ==="
free -h
echo ""

echo "=== Available Java Packages ==="
echo "Checking for Java 21..."
yum list available | grep -i openjdk-21
echo ""

echo "Checking for Java 17..."
yum list available | grep -i openjdk-17
echo ""

echo "Checking for Java 11..."
yum list available | grep -i openjdk-11
echo ""

echo "=== Installed Java Packages ==="
rpm -qa | grep -i openjdk
java -version 2>&1 || echo "Java not currently installed"
echo ""

echo "=== Enabled Repositories ==="
yum repolist enabled
echo ""

echo "=== Disk Space ==="
df -h
echo ""

echo "=== Swap Status ==="
swapon --show
echo ""
