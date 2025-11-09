# Jenkins CI/CD Setup Guide

## Prerequisites

1. Jenkins Server installed
2. Required Jenkins Plugins:
   - Docker Pipeline
   - Kubernetes CLI
   - Maven Integration
   - Git
   - Pipeline
   - Credentials Binding

## Jenkins Configuration

### 1. Configure Tools

**Go to: Manage Jenkins → Global Tool Configuration**

#### Maven Configuration

- Name: `Maven-3.9`
- Install automatically: Yes
- Version: 3.9.x

#### JDK Configuration

- Name: `JDK-17`
- Install automatically: Yes
- Version: 17

### 2. Configure Credentials

**Go to: Manage Jenkins → Credentials → System → Global credentials**

#### Docker Hub Credentials

- Kind: Username with password
- ID: `dockerhub-credentials`
- Username: Your Docker Hub username
- Password: Your Docker Hub password or access token

#### Kubernetes Config

- Kind: Secret file
- ID: `kubeconfig-credentials`
- File: Upload your `~/.kube/config` file

### 3. Create Pipeline Jobs

For each service, create a **Multibranch Pipeline** job:

#### Auth Service

1. New Item → Enter name: `bi-auth-service` → Multibranch Pipeline
2. Branch Sources → Add source → Git
   - Project Repository: Your Git URL for bi-auth-service
   - Credentials: Add your Git credentials
3. Build Configuration
   - Mode: by Jenkinsfile
   - Script Path: `Jenkinsfile`
4. Scan Multibranch Pipeline Triggers
   - Periodically if not otherwise run: 5 minutes

#### API Gateway Service

- Name: `bi-api-gateway-service`
- Follow same steps as above with appropriate repository

#### Course Command Service

- Name: `bi-course-command-service`
- Follow same steps as above with appropriate repository

#### Course Query Service

- Name: `bi-course-query-service`
- Follow same steps as above with appropriate repository

#### Enrollment Service

- Name: `bi-enrollment-service`
- Follow same steps as above with appropriate repository

## Pipeline Stages Explained

### 1. Checkout

- Clones the repository
- Gets the Git commit hash

### 2. Build

- Compiles the code using Maven
- Packages the application (JAR file)

### 3. Test

- Runs unit tests
- Publishes test results

### 4. Code Quality Analysis (Optional)

- Runs SonarQube analysis if configured

### 5. Build Docker Image

- Creates Docker image with build number tag
- Creates Docker image with latest tag

### 6. Push Docker Image

- Pushes images to Docker registry

### 7. Deploy to Kubernetes

- Updates the deployment with new image
- Waits for rollout to complete

## Environment Variables to Customize

Edit the `environment` section in each Jenkinsfile:

```groovy
environment {
    DOCKER_REGISTRY = 'docker.io'              // Change to your registry
    DOCKER_CREDENTIALS_ID = 'dockerhub-credentials'
    K8S_CREDENTIALS_ID = 'kubeconfig-credentials'
    IMAGE_NAME = 'your-org/bi-auth-service'    // Add your org/username
}
```

## Triggering Builds

### Automatic Triggers

1. **Webhook** (Recommended):

   - In your Git repository settings, add Jenkins webhook
   - URL: `http://your-jenkins-server/multibranch-webhook-trigger/invoke?token=your-token`

2. **Polling**:
   - Jenkins will check for changes every 5 minutes (configured in job)

### Manual Triggers

- Go to your pipeline job
- Click "Build Now" or "Scan Multibranch Pipeline Now"

## Monitoring

### Build Status

- Jenkins Dashboard shows build status for all jobs
- Blue Ocean plugin provides better visualization

### Kubernetes Deployment

```bash
# Check deployment status
kubectl get deployments -n bi-course-dev

# Check pods
kubectl get pods -n bi-course-dev

# Check rollout history
kubectl rollout history deployment/dev-auth-service -n bi-course-dev
```

## Rollback

If a deployment fails, rollback to previous version:

```bash
kubectl rollout undo deployment/dev-auth-service -n bi-course-dev
```

## Best Practices

1. **Branch Strategy**:

   - `main` branch → deploys to production
   - `dev` branch → deploys to development
   - Feature branches → no auto-deploy

2. **Version Tagging**:

   - Use build numbers for traceability
   - Tag images with Git commit hash

3. **Security**:

   - Never commit credentials
   - Use Jenkins credentials store
   - Rotate tokens regularly

4. **Testing**:

   - Always run tests before deployment
   - Use test coverage reports

5. **Notifications**:
   - Configure Slack/Email notifications
   - Alert on failures

## Troubleshooting

### Docker Build Fails

- Ensure Docker is installed on Jenkins agent
- Check Dockerfile syntax
- Verify base image availability

### Kubernetes Deployment Fails

- Check kubeconfig credentials
- Verify kubectl is installed on Jenkins agent
- Check Kubernetes cluster connectivity

### Maven Build Fails

- Check Maven configuration
- Verify JDK version
- Check internet connectivity for dependencies

### Tests Fail

- Review test reports in Jenkins
- Check application logs
- Verify test database connectivity

## Advanced Features (Optional)

### SonarQube Integration

1. Install SonarQube plugin in Jenkins
2. Configure SonarQube server in Jenkins
3. Uncomment SonarQube stage in Jenkinsfile

### Parallel Testing

Modify Test stage for parallel execution:

```groovy
stage('Test') {
    parallel {
        stage('Unit Tests') {
            steps {
                sh 'mvn test'
            }
        }
        stage('Integration Tests') {
            steps {
                sh 'mvn verify'
            }
        }
    }
}
```

### Multi-Environment Deployment

Add parameters to choose environment:

```groovy
parameters {
    choice(name: 'ENVIRONMENT', choices: ['dev', 'staging', 'prod'], description: 'Deployment environment')
}
```

## Jenkins Pipeline Visualization

```
Checkout → Build → Test → Build Docker → Push Docker → Deploy K8s
   ↓         ↓       ↓          ↓             ↓            ↓
  SCM     Maven   JUnit    Dockerfile    Registry    Kubernetes
```

## Support

For issues:

1. Check Jenkins console output
2. Review Kubernetes pod logs
3. Check Docker registry
4. Verify credentials and permissions
