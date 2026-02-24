#!/bin/bash

# Script to configure /etc/hosts file for microservices domains
# 
# Usage:
#   Linux/macOS: sudo ./setup-hosts.sh
#   Windows: Not supported - edit hosts file manually (see README.md)
#
# Hosts file locations:
#   Linux:   /etc/hosts
#   macOS:   /etc/hosts
#   Windows: C:\Windows\System32\drivers\etc\hosts

set -e

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Linux*)     HOSTS_FILE="/etc/hosts" ;;
    Darwin*)    HOSTS_FILE="/etc/hosts" ;;
    *)          echo "Error: Unsupported OS. This script works on Linux and macOS only."
                echo "For Windows, please edit the hosts file manually:"
                echo "  C:\\Windows\\System32\\drivers\\etc\\hosts"
                echo "See README.md for detailed instructions."
                exit 1 ;;
esac

DOMAINS=(
    "api.microservices"
    "app.microservices"
    "monitoring.microservices"
)

echo "========================================="
echo "Configuring /etc/hosts for Microservices"
echo "========================================="
echo "OS: ${OS}"
echo "Hosts file: ${HOSTS_FILE}"
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "Error: Please run with sudo"
    echo "Usage: sudo ./setup-hosts.sh"
    exit 1
fi

# Backup hosts file
if [ ! -f "${HOSTS_FILE}.backup" ]; then
    cp "$HOSTS_FILE" "${HOSTS_FILE}.backup"
    echo "✓ Backed up hosts file to ${HOSTS_FILE}.backup"
fi

# Remove existing microservices entries
echo "Removing existing microservices entries..."
sed -i.bak '/\.microservices$/d' "$HOSTS_FILE"

# Add new entries
echo "Adding microservices domains..."
for domain in "${DOMAINS[@]}"; do
    if grep -q "127.0.0.1.*${domain}" "$HOSTS_FILE"; then
        echo "  ⚠ ${domain} already exists"
    else
        echo "127.0.0.1 ${domain}" >> "$HOSTS_FILE"
        echo "  ✓ Added ${domain}"
    fi
done

echo ""
echo "========================================="
echo "Configuration Complete!"
echo "========================================="
echo ""
echo "Added domains:"
for domain in "${DOMAINS[@]}"; do
    echo "  - ${domain}"
done
echo ""
echo "Test with:"
echo "  ping api.microservices"
echo "  ping app.microservices"
echo "  ping monitoring.microservices"
echo ""
echo "To remove entries, run:"
echo "  sudo sed -i.bak '/\.microservices$/d' /etc/hosts"
echo ""
