// Shared Jenkins Pipeline for all microservices
// This reduces code duplication across services

def call(Map config) {
    pipeline {
        agent any
        
        environment {
            DOCKER_REGISTRY = config.dockerRegistry ?: 'docker.io'
            DOCKER_CREDENTIALS_ID = config.dockerCredentialsId ?: 'dockerhub-credentials'
            K8S_CREDENTIALS_ID = config.k8sCredentialsId ?: 'kubeconfig-credentials'
            IMAGE_NAME = config.imageName
            IMAGE_TAG = "${env.BUILD_NUMBER}"
            NAMESPACE = config.namespace ?: 'bi-course-dev'
            DEPLOYMENT_NAME = config.deploymentName
            CONTAINER_NAME = config.containerName
            MAVEN_OPTS = '-Xmx1024m'
            PORT = config.port ?: '8080'
        }
        
        tools {
            maven 'Maven-3.9'
            jdk 'JDK-17'
        }
        
        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                    script {
                        env.GIT_COMMIT_SHORT = sh(
                            script: "git rev-parse --short HEAD",
                            returnStdout: true
                        ).trim()
                        env.IMAGE_TAG_FULL = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    }
                }
            }
            
            stage('Build') {
                steps {
                    echo "Building ${config.imageName}..."
                    sh 'mvn clean package -DskipTests'
                }
            }
            
            stage('Unit Tests') {
                steps {
                    echo 'Running unit tests...'
                    sh 'mvn test'
                }
                post {
                    always {
                        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    }
                }
            }
            
            stage('Code Quality') {
                when {
                    expression { config.sonarEnabled == true }
                }
                steps {
                    withSonarQubeEnv('SonarQube') {
                        sh 'mvn sonar:sonar'
                    }
                }
            }
            
            stage('Build Docker Image') {
                steps {
                    script {
                        echo "Building Docker image: ${IMAGE_NAME}:${IMAGE_TAG_FULL}"
                        docker.build("${IMAGE_NAME}:${IMAGE_TAG_FULL}")
                        docker.build("${IMAGE_NAME}:latest")
                    }
                }
            }
            
            stage('Security Scan') {
                when {
                    expression { config.securityScanEnabled == true }
                }
                steps {
                    script {
                        sh "trivy image ${IMAGE_NAME}:${IMAGE_TAG_FULL}"
                    }
                }
            }
            
            stage('Push Docker Image') {
                steps {
                    script {
                        docker.withRegistry("https://${DOCKER_REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {
                            docker.image("${IMAGE_NAME}:${IMAGE_TAG_FULL}").push()
                            docker.image("${IMAGE_NAME}:latest").push()
                        }
                        echo "Pushed ${IMAGE_NAME}:${IMAGE_TAG_FULL}"
                    }
                }
            }
            
            stage('Deploy to Dev') {
                when {
                    branch 'dev'
                }
                steps {
                    script {
                        deployToKubernetes('dev', IMAGE_TAG_FULL)
                    }
                }
            }
            
            stage('Deploy to Staging') {
                when {
                    branch 'staging'
                }
                steps {
                    script {
                        deployToKubernetes('staging', IMAGE_TAG_FULL)
                    }
                }
            }
            
            stage('Deploy to Production') {
                when {
                    branch 'main'
                }
                steps {
                    input message: 'Deploy to Production?', ok: 'Deploy'
                    script {
                        deployToKubernetes('prod', IMAGE_TAG_FULL)
                    }
                }
            }
            
            stage('Health Check') {
                steps {
                    script {
                        withKubeConfig([credentialsId: "${K8S_CREDENTIALS_ID}"]) {
                            sh """
                                kubectl wait --for=condition=ready pod \
                                    -l app=${CONTAINER_NAME} \
                                    -n ${NAMESPACE} \
                                    --timeout=300s
                                
                                echo "Service is healthy!"
                            """
                        }
                    }
                }
            }
        }
        
        post {
            success {
                echo "✅ Pipeline succeeded for ${config.imageName}"
                // Add Slack/Email notification
            }
            failure {
                echo "❌ Pipeline failed for ${config.imageName}"
                // Add Slack/Email notification
            }
            always {
                cleanWs()
            }
        }
    }
}

def deployToKubernetes(String environment, String imageTag) {
    withKubeConfig([credentialsId: "${K8S_CREDENTIALS_ID}"]) {
        sh """
            kubectl set image deployment/${environment}-${DEPLOYMENT_NAME} \
                ${CONTAINER_NAME}=${IMAGE_NAME}:${imageTag} \
                -n ${NAMESPACE}
            
            kubectl rollout status deployment/${environment}-${DEPLOYMENT_NAME} \
                -n ${NAMESPACE} \
                --timeout=5m
        """
    }
}

return this
