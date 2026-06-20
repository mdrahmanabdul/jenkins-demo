# Jenkins → GitHub → EC2 Deployment Notes

## Objective

Automatically deploy a Spring Boot application to an AWS EC2 instance whenever Jenkins builds the project.

---

# Architecture

```text
GitHub Repository
        ↓
Jenkins Pipeline
        ↓
Build Spring Boot JAR
        ↓
SSH/SCP
        ↓
AWS EC2
        ↓
systemd Service Restart
        ↓
Application Live
```

---

# Prerequisites

## Local Machine

* Java 17
* Git
* Maven Wrapper (`mvnw`)

## Jenkins

* Running in Docker
* Pipeline support installed
* Git support installed
* SSH Agent plugin installed

## AWS

* Ubuntu EC2 Instance
* Security Group allowing:

  * Port 22 (SSH)
  * Port 8080 (Spring Boot App)

---

# Spring Boot Project Structure

Repository structure:

```text
jenkins-demo
└── JenkinsDemo
    ├── src
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    └── Jenkinsfile
```

---

# EC2 Setup

## Connect

```bash
chmod 400 jenkins-demo.pem

ssh -i jenkins-demo.pem ubuntu@<PUBLIC_IP>
```

Example:

```bash
ssh -i jenkins-demo.pem ubuntu@16.176.176.250
```

---

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

[Service]
User=ubuntu
WorkingDirectory=/opt/spring-app

ExecStart=/usr/bin/java -jar /opt/spring-app/app.jar

SuccessExitStatus=143

Restart=always

[Install]
WantedBy=multi-user.target
```

---

## Enable Service

```bash
sudo systemctl daemon-reload

sudo systemctl enable spring-app

sudo systemctl start spring-app
```

---

## Check Status

```bash
sudo systemctl status spring-app
```

---

# Jenkins → EC2 SSH Trust

---

## Enter Jenkins Container

```bash
docker ps

docker exec -it <container-id> bash
```

---

## Generate SSH Key

Inside Jenkins container:

```bash
ssh-keygen -t rsa -b 4096
```

---

## View Public Key

```bash
cat ~/.ssh/id_rsa.pub
```

Copy entire output.

---

## Add Public Key to EC2

SSH into EC2:

```bash
ssh -i jenkins-demo.pem ubuntu@<PUBLIC_IP>
```

Create SSH folder:

```bash
mkdir -p ~/.ssh

chmod 700 ~/.ssh
```

Edit:

```bash
nano ~/.ssh/authorized_keys
```

Paste Jenkins public key.

Permissions:

```bash
chmod 600 ~/.ssh/authorized_keys
```

---

## Verify Jenkins Can SSH

Inside Jenkins container:

```bash
ssh ubuntu@<PUBLIC_IP>
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
→ Add Credentials
```

---

## Credential Details

Kind:

```text
SSH Username with private key
```

Username:

```text
ubuntu
```

ID:

```text
aws-prod-ssh
```

Private Key:

Paste contents of:

```bash
cat ~/.ssh/id_rsa
```

from Jenkins container.

---

# GitHub Repository

Repository:

```text
https://github.com/mdrahmanabdul/jenkins-demo
```

---

# Jenkins Pipeline Job

Navigate:

```text
New Item
→ Pipeline
```

---

## Configure

Definition:

```text
Pipeline script from SCM
```

SCM:

```text
Git
```

Repository URL:

```text
https://github.com/mdrahmanabdul/jenkins-demo.git
```

Branch:

```text
*/main
```

Script Path:

```text
JenkinsDemo/Jenkinsfile
```

---

# Jenkinsfile

Location:

```text
JenkinsDemo/Jenkinsfile
```

Content:

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
                    ls -la JenkinsDemo/target

                    scp -o StrictHostKeyChecking=no \
                    JenkinsDemo/target/*.jar \
                    ubuntu@16.176.176.250:/opt/spring-app/app.jar

                    ssh -o StrictHostKeyChecking=no \
                    ubuntu@16.176.176.250 \
                    "sudo systemctl restart spring-app"
                    '''
                }
            }
        }
    }
}
```

---

# Useful Commands

## Build Locally

```bash
./mvnw clean package
```

---

## Manual Copy

```bash
scp -i jenkins-demo.pem \
target/*.jar \
ubuntu@16.176.176.250:/opt/spring-app/app.jar
```

---

## Start Application Manually

```bash
java -jar /opt/spring-app/app.jar
```

---

## Check Service

```bash
sudo systemctl status spring-app
```

---

## Restart Service

```bash
sudo systemctl restart spring-app
```

---

## View Logs

```bash
sudo journalctl -xeu spring-app.service
```

---

## Check Port Usage

```bash
sudo lsof -i :8080
```

or

```bash
sudo ss -tulpn | grep 8080
```

---

## Test Application

Inside EC2:

```bash
curl localhost:8080
```

From Browser:

```text
http://16.176.176.250:8080
```

---

# Common Issues

## Jenkinsfile Not Found

Cause:

```text
Wrong Script Path
```

Fix:

```text
JenkinsDemo/Jenkinsfile
```

---

## mvnw Not Found

Cause:

```text
Pipeline running from repository root
```

Fix:

```groovy
dir('JenkinsDemo') {
    sh './mvnw clean package'
}
```

---

## JAR Not Found

Cause:

```text
Wrong target directory
```

Fix:

```text
JenkinsDemo/target/*.jar
```

---

## Port 8080 Not Accessible

Cause:

```text
Security Group missing 8080
```

Fix:

Add inbound rule:

```text
TCP 8080
0.0.0.0/0
```

---

## SSH Authentication Failure

Verify:

```bash
ssh ubuntu@<PUBLIC_IP>
```

works from Jenkins container.

---

# Final Flow

```text
Developer
    ↓
git push
    ↓
GitHub
    ↓
Jenkins Pipeline
    ↓
Build Spring Boot JAR
    ↓
Copy JAR to EC2
    ↓
Restart systemd Service
    ↓
Application Updated
```
