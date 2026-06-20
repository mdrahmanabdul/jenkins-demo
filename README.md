# Spring Boot CI/CD with Jenkins and AWS EC2

## Overview

This project demonstrates a complete CI/CD pipeline using:

* Spring Boot
* GitHub
* Jenkins (Docker)
* AWS EC2 (Ubuntu)
* systemd

The goal is to automatically build and deploy a Spring Boot application whenever changes are pushed to the `main` branch.

---

# Architecture

```text
GitHub
   ↓
Jenkins (Docker on EC2)
   ↓
Build Spring Boot JAR
   ↓
Deploy to EC2
   ↓
Restart systemd Service
   ↓
Application Available on Port 8080
```

---

# Infrastructure

## EC2

* Ubuntu 24/26
* OpenJDK 17
* Docker
* Jenkins (running in Docker)

## Application

* Spring Boot
* Maven Wrapper (`mvnw`)

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

# Create systemd Service

File:

```bash
sudo nano /etc/systemd/system/spring-app.service
```

Content:

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

Enable:

```bash
sudo systemctl daemon-reload
sudo systemctl enable spring-app
sudo systemctl start spring-app
```

Check:

```bash
sudo systemctl status spring-app
```

---

# Jenkins Setup

## Run Jenkins

```bash
docker run -d \
--name jenkins \
--restart unless-stopped \
-p 8081:8080 \
-p 50000:50000 \
-v jenkins_home:/var/jenkins_home \
jenkins/jenkins:lts-jdk17
```

Access:

```text
http://<EC2_PUBLIC_IP>:8081
```

---

# Jenkins SSH Access

Enter Jenkins container:

```bash
docker exec -it jenkins bash
```

Generate SSH key:

```bash
ssh-keygen -t rsa -b 4096
```

View public key:

```bash
cat ~/.ssh/id_rsa.pub
```

Copy the public key.

---

## Authorize Jenkins on EC2

On EC2:

```bash
mkdir -p ~/.ssh
nano ~/.ssh/authorized_keys
```

Paste Jenkins public key.

Permissions:

```bash
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

Verify:

```bash
ssh ubuntu@<EC2_PUBLIC_IP>
```

No password should be requested.

---

# Jenkins Credentials

Navigate:

```text
Manage Jenkins
→ Credentials
→ System
→ Global Credentials
```

Add:

```text
Kind: SSH Username with private key

Username: ubuntu

ID: aws-prod-ssh
```

Paste private key from:

```bash
cat ~/.ssh/id_rsa
```

inside Jenkins container.

---

# Pipeline Job

Create:

```text
New Item
→ Pipeline
```

Configuration:

```text
Definition:
Pipeline script from SCM

SCM:
Git

Repository URL:
https://github.com/<user>/<repo>.git

Branch:
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

# Security Group Rules

Inbound:

| Port | Purpose     |
| ---- | ----------- |
| 22   | SSH         |
| 8080 | Spring Boot |
| 8081 | Jenkins     |

---

# Troubleshooting Guide

## 1. Jenkinsfile Not Found

Error:

```text
Unable to find Jenkinsfile
```

Cause:

Wrong script path.

Fix:

```text
JenkinsDemo/Jenkinsfile
```

---

## 2. mvnw Not Found

Error:

```text
./mvnw: not found
```

Cause:

Pipeline running from repository root.

Fix:

```groovy
dir('JenkinsDemo') {
    sh './mvnw clean package'
}
```

---

## 3. JAR Not Found

Error:

```text
scp: stat local "target/*.jar": No such file
```

Cause:

Wrong target directory.

Fix:

```text
JenkinsDemo/target/*.jar
```

---

## 4. Permission Denied During SCP

Error:

```text
Permission denied
```

Cause:

Copying directly into protected directory.

Fix:

Ensure correct ownership:

```bash
sudo chown -R ubuntu:ubuntu /opt/spring-app
```

---

## 5. systemctl Restart Failed

Error:

```text
Job for spring-app.service failed
```

Cause:

Application startup issue.

Debug:

```bash
sudo systemctl status spring-app

sudo journalctl -xeu spring-app.service

java -jar /opt/spring-app/app.jar
```

---

## 6. Port 8080 Not Reachable

Cause:

Security Group missing port 8080.

Fix:

Add inbound rule:

```text
TCP 8080
0.0.0.0/0
```

---

## 7. Jenkins Container Exited (137)

Error:

```text
Exited (137)
```

Cause:

Out Of Memory (OOM)

Diagnosis:

```bash
docker ps -a
free -h
```

Observed:

```text
RAM: ~1 GB
Swap: 0
```

Solution:

Create swap:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

Persist:

```bash
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Verify:

```bash
free -h
```

---

## 8. Jenkins Node Offline

Error:

```text
Disk space below threshold
```

Observed:

```text
Only 422 MB free
```

Cause:

Root volume too small.

Fix:

Increase EBS volume:

```text
8 GB → 20 GB
```

Expand partition:

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
13 GB free
```

---

# Useful Commands

Check application:

```bash
curl localhost:8080
```

Restart application:

```bash
sudo systemctl restart spring-app
```

View logs:

```bash
sudo journalctl -xeu spring-app.service
```

Check Java process:

```bash
ps -ef | grep java
```

Check port:

```bash
sudo ss -tulpn | grep 8080
```

Check disk:

```bash
df -h
```

Check memory:

```bash
free -h
```

Check swap:

```bash
swapon --show
```

---

# Lessons Learned

1. CI/CD is not just Jenkins.
2. Infrastructure issues often appear as build failures.
3. Exit Code 137 usually means Out Of Memory.
4. Jenkins requires adequate disk space and memory.
5. systemd is the correct way to manage Spring Boot services.
6. SSH-based deployments are simple and effective for learning.
7. Debugging is a core DevOps skill.

---

# Final Result

Successful deployment flow:

```text
git push
    ↓
GitHub
    ↓
Jenkins
    ↓
Build Spring Boot JAR
    ↓
Copy JAR to EC2
    ↓
Restart Service
    ↓
Application Updated
```
testing
