# Spring Boot CI/CD Pipeline with Jenkins, GitHub and AWS EC2

## Overview

This project demonstrates a complete CI/CD pipeline using:

* Spring Boot
* Maven Wrapper (mvnw)
* GitHub
* Jenkins (running inside Docker)
* AWS EC2 (Ubuntu)
* systemd

The objective is to automatically build and deploy a Spring Boot application whenever code is pushed to the `main` branch.

---

# Final Architecture

```text
Developer
    ↓
git push origin main
    ↓
GitHub Webhook
    ↓
Jenkins Pipeline
    ↓
Maven Build
    ↓
Generate Spring Boot JAR
    ↓
Copy JAR to EC2
    ↓
Restart Spring Boot Service
    ↓
Application Updated
```

---

# Infrastructure

## EC2 Instance

Operating System:

```text
Ubuntu
```

Installed Software:

```text
OpenJDK 17
Docker
Jenkins (Docker Container)
```

## Application

```text
Spring Boot
Java 17
Maven Wrapper (mvnw)
```

---

# EC2 Setup

## Install Java

```bash
sudo apt update
sudo apt install openjdk-17-jdk -y
```

Verify:

```bash
java -version
```

---

## Create Deployment Directory

```bash
sudo mkdir -p /opt/spring-app
sudo chown -R ubuntu:ubuntu /opt/spring-app
```

---

# Configure Spring Boot Service

Create:

```bash
sudo nano /etc/systemd/system/spring-app.service
```

Contents:

```ini
[Unit]
Description=Spring Boot App
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/spring-app

ExecStart=/usr/bin/java -jar /opt/spring-app/app.jar

SuccessExitStatus=143

Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Enable service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable spring-app
```

Start service:

```bash
sudo systemctl start spring-app
```

Verify:

```bash
sudo systemctl status spring-app
```

---

# Install Jenkins Using Docker

Start Jenkins:

```bash
docker run -d \
--name jenkins \
--restart unless-stopped \
-p 8081:8080 \
-p 50000:50000 \
-v jenkins_home:/var/jenkins_home \
jenkins/jenkins:lts-jdk17
```

Access Jenkins:

```text
http://<EC2_PUBLIC_IP>:8081
```

---

# Configure SSH Access For Deployment

## Enter Jenkins Container

```bash
docker exec -it jenkins bash
```

---

## Generate SSH Key

```bash
ssh-keygen -t rsa -b 4096
```

View public key:

```bash
cat ~/.ssh/id_rsa.pub
```

---

## Authorize Jenkins On EC2

Append the Jenkins public key to:

```bash
~/.ssh/authorized_keys
```

Set permissions:

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

Verify:

```bash
ssh ubuntu@<EC2_PUBLIC_IP>
```

Expected:

```text
Login without password
```

---

# Jenkins Credentials

Navigate to:

```text
Manage Jenkins
→ Credentials
→ System
→ Global Credentials
```

Add:

```text
Kind:
SSH Username with private key

Username:
ubuntu

ID:
aws-prod-ssh
```

Paste contents of:

```bash
cat ~/.ssh/id_rsa
```

from inside the Jenkins container.

---

# Create Jenkins Pipeline Job

Navigate to:

```text
New Item
→ Pipeline
```

Configure:

```text
Definition:
Pipeline script from SCM

SCM:
Git

Repository URL:
https://github.com/<user>/<repo>.git

Branch Specifier:
*/main

Script Path:
JenkinsDemo/Jenkinsfile
```

---

# Jenkinsfile

```groovy
pipeline {

    agent any

    stages {

        stage('Build') {
            steps {
                dir('JenkinsDemo') {
                    sh 'chmod +x mvnw'
                    sh './mvnw clean package'
                }
            }
        }

        stage('Deploy') {
            steps {

                sshagent(['aws-prod-ssh']) {

                    sh '''
                    scp -o StrictHostKeyChecking=no \
                    JenkinsDemo/target/*.jar \
                    ubuntu@<EC2_PUBLIC_IP>:/opt/spring-app/app.jar

                    ssh -o StrictHostKeyChecking=no \
                    ubuntu@<EC2_PUBLIC_IP> \
                    "sudo systemctl restart spring-app"
                    '''
                }
            }
        }
    }
}
```

---

# Configure GitHub Webhook

## Jenkins Configuration

Open:

```text
Pipeline Job
→ Configure
→ Build Triggers
```

Enable:

```text
GitHub hook trigger for GITScm polling
```

Save.

---

## GitHub Configuration

Navigate to:

```text
GitHub Repository
→ Settings
→ Webhooks
→ Add webhook
```

Configure:

```text
Payload URL:
http://<JENKINS_PUBLIC_IP>:8081/github-webhook/

Content Type:
application/json

Events:
Just the push event
```

Example:

```text
http://32.236.142.186:8081/github-webhook/
```

Important:

```text
The URL must end with a trailing slash (/)
```

Correct:

```text
http://<JENKINS_IP>:8081/github-webhook/
```

Incorrect:

```text
http://<JENKINS_IP>:8081/github-webhook
```

---

# Security Group Configuration

Inbound Rules:

| Port | Purpose                 |
| ---- | ----------------------- |
| 22   | SSH                     |
| 8080 | Spring Boot Application |
| 8081 | Jenkins                 |
| 443  | HTTPS (Optional)        |
| 80   | HTTP (Optional)         |

---

# Testing The Pipeline

Make a change:

```bash
git add .
git commit -m "Testing CI/CD"
git push origin main
```

Expected flow:

```text
Git Push
    ↓
GitHub Webhook
    ↓
Jenkins Build
    ↓
Maven Package
    ↓
Deploy JAR
    ↓
Restart Spring Boot Service
    ↓
Production Updated
```

---

# Infrastructure Improvements

## Increase EBS Volume

Problem:

```text
Jenkins Node Offline
Disk space below threshold
```

Observed:

```text
Only 422 MB free
```

Solution:

Increase EBS Volume:

```text
8 GB → 20 GB
```

Extend partition:

```bash
sudo growpart /dev/nvme0n1 1
sudo resize2fs /dev/nvme0n1p1
```

Verify:

```bash
df -h
```

Result:

```text
19 GB total
13 GB available
```

---

## Add Swap Memory

Problem:

```text
Jenkins Container Exited (137)
```

Meaning:

```text
Out Of Memory (OOM)
```

Observed:

```text
RAM : 908 MB
Swap: 0 GB
```

Create swap:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

Persist after reboot:

```bash
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Verify:

```bash
free -h
swapon --show
```

Final state:

```text
RAM : 908 MB
Swap: 2 GB
```

---

# Troubleshooting Guide

## Jenkinsfile Not Found

Error:

```text
Unable to find Jenkinsfile
```

Fix:

```text
Script Path:
JenkinsDemo/Jenkinsfile
```

---

## Maven Wrapper Not Found

Error:

```text
./mvnw: not found
```

Fix:

```groovy
dir('JenkinsDemo') {
    sh './mvnw clean package'
}
```

---

## JAR Not Found

Error:

```text
scp: stat local "target/*.jar": No such file
```

Fix:

```text
JenkinsDemo/target/*.jar
```

---

## Permission Denied During SCP

Error:

```text
Permission denied
```

Fix:

```bash
sudo chown -R ubuntu:ubuntu /opt/spring-app
```

---

## Spring Boot Service Failed To Start

Error:

```text
Job for spring-app.service failed
```

Debug Commands:

```bash
sudo systemctl status spring-app

sudo journalctl -xeu spring-app.service

java -jar /opt/spring-app/app.jar
```

---

## Port 8080 Not Reachable

Cause:

```text
Missing Security Group Rule
```

Fix:

```text
TCP 8080
Source: 0.0.0.0/0
```

---

## Jenkins Container Exited (137)

Cause:

```text
Out Of Memory
```

Fix:

```text
Add 2 GB Swap
```

---

## Jenkins Node Offline

Cause:

```text
Low Disk Space
```

Fix:

```text
Increase EBS Volume
```

---

## GitHub Webhook Returning 302

Cause:

```text
Webhook URL missing trailing slash
```

Incorrect:

```text
http://<JENKINS_IP>:8081/github-webhook
```

Correct:

```text
http://<JENKINS_IP>:8081/github-webhook/
```

---

# Useful Commands

Check Application:

```bash
curl localhost:8080
```

Restart Service:

```bash
sudo systemctl restart spring-app
```

View Logs:

```bash
sudo journalctl -xeu spring-app.service
```

Check Running Java Processes:

```bash
ps -ef | grep java
```

Check Port Usage:

```bash
sudo ss -tulpn | grep 8080
```

Check Disk Space:

```bash
df -h
```

Check Memory:

```bash
free -h
```

Check Swap:

```bash
swapon --show
```

Check Jenkins Container:

```bash
docker ps
docker logs jenkins
```

---

# Lessons Learned

1. CI/CD involves much more than Jenkins.
2. Infrastructure issues often appear as build failures.
3. Exit code 137 usually indicates Out Of Memory.
4. Jenkins requires sufficient memory and disk space.
5. systemd is the preferred way to manage Spring Boot services.
6. SSH-based deployments are simple and effective for learning.
7. GitHub Webhooks eliminate manual deployments.
8. Debugging infrastructure is a critical DevOps skill.

---

# Future Improvements

1. Use an Elastic IP instead of a temporary public IP.
2. Remove hardcoded IP addresses from Jenkinsfile.
3. Store deployment targets in Jenkins environment variables.
4. Add a staging environment.
5. Use Nginx as a reverse proxy.
6. Enable HTTPS using Let's Encrypt.
7. Containerize the application using Docker.
8. Deploy using Kubernetes.
9. Add automated tests before deployment.

---

# Final Result

A fully automated CI/CD pipeline where:

```text
git push origin main
        ↓
GitHub Webhook
        ↓
Jenkins
        ↓
Build
        ↓
Deploy
        ↓
Production Updated
```
