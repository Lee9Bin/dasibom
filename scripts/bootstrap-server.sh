#!/usr/bin/env bash
set -euo pipefail
# Run on the dedicated Amazon Linux 2023 EC2 as ec2-user.
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo install -d -m 755 /usr/local/lib/docker/cli-plugins
if ! sudo docker compose version >/dev/null 2>&1; then
  curl -fL https://github.com/docker/compose/releases/download/v2.39.4/docker-compose-linux-x86_64 -o /tmp/dasibom-docker-compose
  sudo install -m 755 /tmp/dasibom-docker-compose /usr/local/lib/docker/cli-plugins/docker-compose
fi
if ! sudo swapon --show=NAME --noheadings | grep -q '^/swapfile$'; then
  if [ ! -e /swapfile ]; then
    sudo fallocate -l 2G /swapfile
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile
  fi
  sudo swapon /swapfile
  if ! grep -q '^/swapfile ' /etc/fstab; then
    printf '/swapfile none swap sw 0 0\n' | sudo tee -a /etc/fstab >/dev/null
  fi
fi
sudo sysctl vm.swappiness=10
sudo install -d -m 700 -o ec2-user -g ec2-user /opt/dasibom
sudo docker compose version
free -m
