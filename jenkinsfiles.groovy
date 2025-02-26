pipeline {
    agent any
    
    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPOSITORY = '${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/ip-reverser'
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        SCANNER_HOME = tool 'SonarQubeScanner'
        AWS_CREDENTIALS = 'aws-credentials'
    }
    
    tools {
        terraform 'Terraform'
        dockerTool 'Docker'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Security Scan - Code') {
            steps {
                script {
                    // Run Bandit for Python security scanning
                    sh 'pip install bandit'
                    sh 'bandit -r app.py'
                    
                    // Run TruffleHog for secrets scanning
                    sh 'pip install trufflehog'
                    sh 'trufflehog --regex --entropy=False .'
                }
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh "${SCANNER_HOME}/bin/sonar-scanner \
                      -Dsonar.projectKey=ip-reverser \
                      -Dsonar.sources=. \
                      -Dsonar.python.coverage.reportPaths=coverage.xml"
                }
            }
        }
        
        stage('Run Tests') {
            steps {
                sh 'pip install -r requirements.txt'
                sh 'pip install pytest pytest-cov'
                sh 'python -m pytest --cov=. --cov-report=xml'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh 'docker build -t ${ECR_REPOSITORY}:${IMAGE_TAG} .'
                    sh 'docker tag ${ECR_REPOSITORY}:${IMAGE_TAG} ${ECR_REPOSITORY}:latest'
                }
            }
        }
        
        stage('Security Scan - Container') {
            steps {
                script {
                    // Trivy scan for container vulnerabilities
                    sh 'docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:latest image ${ECR_REPOSITORY}:${IMAGE_TAG}'
                }
            }
        }
        
        stage('Terraform Init & Plan') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', 
                                  credentialsId: "${AWS_CREDENTIALS}", 
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID', 
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir('terraform') {
                        sh 'terraform init'
                        
                        // Run tfsec for Terraform security scanning
                        sh 'docker run --rm -v $(pwd):/src aquasec/tfsec:latest /src'
                        
                        // Check for compliance with terraform-compliance
                        sh 'pip install terraform-compliance'
                        sh 'terraform plan -out=plan.out'
                    }
                }
            }
        }
        
        stage('Push to ECR') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', 
                                  credentialsId: "${AWS_CREDENTIALS}", 
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID', 
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                        aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REPOSITORY}
                        docker push ${ECR_REPOSITORY}:${IMAGE_TAG}
                        docker push ${ECR_REPOSITORY}:latest
                    '''
                }
            }
        }
        
        stage('Terraform Apply') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', 
                                  credentialsId: "${AWS_CREDENTIALS}", 
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID', 
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir('terraform') {
                        sh 'terraform apply -auto-approve plan.out'
                    }
                }
            }
        }
        
        stage('Deploy to ECS') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', 
                                  credentialsId: "${AWS_CREDENTIALS}", 
                                  accessKeyVariable: 'AWS_ACCESS_KEY_ID', 
                                  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                        aws ecs update-service --cluster ip-reverser-cluster --service ip-reverser-service --force-new-deployment --region ${AWS_REGION}
                    '''
                }
            }
        }
    }
    
    post {
        always {
            // Clean up Docker images
            sh 'docker rmi ${ECR_REPOSITORY}:${IMAGE_TAG} ${ECR_REPOSITORY}:latest || true'
            
            // Archive artifacts
            archiveArtifacts artifacts: 'terraform/plan.out', allowEmptyArchive: true
            
            // Publish test results
            junit '**/test-results/*.xml'
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed.'
        }
    }
}
