#!/bin/bash

# Setup script for GitLab Runner with staging tag
# This script helps configure a GitLab Runner for CI/CD pipeline execution

set -e

echo "========================================="
echo "GitLab Runner Setup Script"
echo "========================================="

# Check if running as root or with sudo
if [ "$EUID" -ne 0 ]; then 
    echo "Please run as root or with sudo"
    exit 1
fi

# Detect OS
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$ID
else
    echo "Cannot detect OS"
    exit 1
fi

echo "Detected OS: $OS"

# Install GitLab Runner based on OS
install_gitlab_runner() {
    case $OS in
        ubuntu|debian)
            echo "Installing GitLab Runner for Ubuntu/Debian..."
            curl -L "https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.deb.sh" | bash
            apt-get install gitlab-runner -y
            ;;
        centos|rhel|fedora)
            echo "Installing GitLab Runner for CentOS/RHEL/Fedora..."
            curl -L "https://packages.gitlab.com/install/repositories/runner/gitlab-runner/script.rpm.sh" | bash
            yum install gitlab-runner -y
            ;;
        *)
            echo "Unsupported OS. Please install GitLab Runner manually."
            exit 1
            ;;
    esac
}

# Install Docker (if not installed)
install_docker() {
    if command -v docker &> /dev/null; then
        echo "Docker is already installed"
    else
        echo "Installing Docker..."
        case $OS in
            ubuntu|debian)
                apt-get update
                apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
                curl -fsSL https://download.docker.com/linux/$OS/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
                echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/$OS $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
                apt-get update
                apt-get install -y docker-ce docker-ce-cli containerd.io
                ;;
            centos|rhel)
                yum install -y yum-utils
                yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
                yum install -y docker-ce docker-ce-cli containerd.io
                ;;
            *)
                echo "Please install Docker manually for your OS"
                ;;
        esac
    fi
    
    # Start and enable Docker
    systemctl start docker
    systemctl enable docker
    
    # Add gitlab-runner user to docker group
    usermod -aG docker gitlab-runner
    
    echo "Docker installed and configured"
}

# Install Docker Compose (if not installed)
install_docker_compose() {
    if command -v docker-compose &> /dev/null; then
        echo "Docker Compose is already installed"
    else
        echo "Installing Docker Compose..."
        curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        chmod +x /usr/local/bin/docker-compose
        echo "Docker Compose installed"
    fi
}

# Register GitLab Runner
register_runner() {
    echo ""
    echo "========================================="
    echo "GitLab Runner Registration"
    echo "========================================="
    echo "You will need the following information:"
    echo "  - GitLab URL (e.g., https://gitlab.com/)"
    echo "  - Registration Token (from GitLab project: Settings > CI/CD > Runners)"
    echo ""
    read -p "GitLab URL: " GITLAB_URL
    read -p "Registration Token: " REGISTRATION_TOKEN
    read -p "Runner Description [staging-runner]: " RUNNER_DESCRIPTION
    RUNNER_DESCRIPTION=${RUNNER_DESCRIPTION:-staging-runner}
    
    echo "Registering GitLab Runner..."
    gitlab-runner register \
        --non-interactive \
        --url "$GITLAB_URL" \
        --registration-token "$REGISTRATION_TOKEN" \
        --executor "docker" \
        --docker-image "docker:24" \
        --description "$RUNNER_DESCRIPTION" \
        --tag-list "staging" \
        --run-untagged="false" \
        --locked="false" \
        --access-level="not_protected"
    
    echo "GitLab Runner registered successfully!"
}

# Configure runner for Docker-in-Docker
configure_dind() {
    echo "Configuring Docker-in-Docker support..."
    CONFIG_FILE="/etc/gitlab-runner/config.toml"
    
    if [ -f "$CONFIG_FILE" ]; then
        # Backup original config
        cp "$CONFIG_FILE" "$CONFIG_FILE.backup"
        
        # Add Docker-in-Docker configuration
        sed -i '/\[runners.docker\]/a\    privileged = true' "$CONFIG_FILE" 2>/dev/null || true
        sed -i '/\[runners.docker\]/a\    volumes = ["/certs/client", "/cache"]' "$CONFIG_FILE" 2>/dev/null || true
        
        echo "Docker-in-Docker configured"
    else
        echo "Config file not found. Manual configuration may be needed."
    fi
}

# Main execution
main() {
    echo ""
    echo "Starting installation..."
    
    # Check if GitLab Runner is already installed
    if command -v gitlab-runner &> /dev/null; then
        echo "GitLab Runner is already installed"
        read -p "Do you want to register a new runner? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            register_runner
            configure_dind
        fi
    else
        install_gitlab_runner
        install_docker
        install_docker_compose
        register_runner
        configure_dind
    fi
    
    # Restart GitLab Runner
    echo "Restarting GitLab Runner..."
    gitlab-runner restart
    
    echo ""
    echo "========================================="
    echo "Setup Complete!"
    echo "========================================="
    echo "GitLab Runner is installed and configured with tag: staging"
    echo ""
    echo "Verify runner status:"
    echo "  sudo gitlab-runner verify"
    echo ""
    echo "Check runner logs:"
    echo "  sudo gitlab-runner --debug run"
    echo ""
    echo "List registered runners:"
    echo "  sudo gitlab-runner list"
}

# Run main function
main

