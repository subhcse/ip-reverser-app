# IP Reverser Application

This project is a simple web application that receives requests and returns the origin public IP address in reverse order. For example, if your IP is `1.2.3.4`, the application will display it as `4.3.2.1`.

## Project Overview

The application is built using Flask, containerized with Docker, deployed on AWS ECS, and includes a complete CI/CD pipeline using Jenkins.

### Key Technologies

- **Backend**: Python with Flask
- **Containerization**: Docker
- **Cloud Provider**: AWS (ECS, ECR, VPC, CloudWatch)
- **Infrastructure as Code**: Terraform
- **CI/CD**: Jenkins
- **Security Scanning**: Bandit, TruffleHog, Trivy, tfsec

## Project Structure

```
.
├── app.py                     # The Flask application
├── Dockerfile                 # Docker container definition
├── requirements.txt           # Python dependencies
├── test_app.py                # Unit tests
├── Jenkinsfile                # Jenkins pipeline definition
└── terraform/                 # IaC for AWS
    ├── main.tf                # Main Terraform configuration
    └── variables.tf           # Terraform variables
```

## Local Development

### Prerequisites

- Python 3.9+
- Docker
- AWS CLI (configured with appropriate credentials)
- Terraform (for infrastructure provisioning)

### Setup

1. Create a virtual environment and install dependencies:
   ```
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   pip install -r requirements.txt
   ```

2. Run the application locally:
   ```
   python app.py
   ```

3. The application will be available at http://localhost:8080

### Running Tests

```
pytest
```

## Building and Running with Docker

```
docker build -t ip-reverser .
docker run -p 8080:8080 ip-reverser
```

Visit http://localhost:8080 in your browser.

## Infrastructure Setup

The application is deployed to AWS using Terraform:

1. **VPC and Networking**: A dedicated VPC with public subnet, Internet Gateway, and appropriate route tables
2. **Container Registry**: Amazon ECR to store Docker images
3. **Compute**: Amazon ECS Fargate for running the containerized application
4. **Monitoring**: CloudWatch for logs and monitoring

To deploy the infrastructure manually:

```
cd terraform
terraform init
terraform plan
terraform apply
```

## CI/CD Pipeline

The Jenkins pipeline automates the entire build, test, security scan, and deployment process:

1. **Code Checkout**: Retrieves the code from the repository
2. **Security Scanning**: Performs security analysis on the code using Bandit and checks for leaked secrets with TruffleHog
3. **Code Quality**: Runs SonarQube analysis
4. **Testing**: Executes unit tests with coverage reports
5. **Docker Build**: Creates the Docker image
6. **Container Security**: Scans the Docker image for vulnerabilities with Trivy
7. **Infrastructure Validation**: Runs security checks on Terraform code with tfsec
8. **ECR Push**: Pushes the Docker image to Amazon ECR
9. **Infrastructure Deployment**: Applies the Terraform configuration
10. **Application Deployment**: Updates the ECS service to use the new image

### Jenkins Setup Requirements

1. Install the following plugins in Jenkins:
   - AWS Credentials Plugin
   - Docker Pipeline
   - Pipeline: AWS Steps
   - SonarQube Scanner
   - Terraform

2. Configure the following tools in Jenkins:
   - Docker
   - Terraform
   - SonarQubeScanner

3. Add AWS credentials in Jenkins with ID 'aws-credentials'

## Security Features

- **Code Scanning**: Bandit for Python security vulnerabilities
- **Secret Detection**: TruffleHog to identify potential leaked secrets
- **Image Scanning**: Trivy for container vulnerability detection
- **Infrastructure Scanning**: tfsec for Terraform security best practices
- **Least Privilege**: IAM roles with minimum required permissions
- **Network Security**: VPC with proper security groups

## Monitoring and Logging

- AWS CloudWatch is configured to capture and store application logs
- CloudWatch metrics are available for monitoring the ECS service

## Deployment Instructions

The application will be automatically deployed through the Jenkins pipeline. To trigger a deployment:

1. Push changes to the repository
2. Jenkins will detect the changes and start the pipeline
3. Once the pipeline completes successfully, the application will be deployed to AWS ECS
4. Access the application using the public IP of the ECS service on port 8080

## Troubleshooting

If you encounter issues:

1. Check the CloudWatch logs for application errors
2. Review the Jenkins pipeline execution for build or deployment failures
3. Verify the ECS service status in the AWS Management Console
4. Ensure that security groups allow traffic on port 8080

---

Feel free to reach out if you have any questions or need further clarification.
