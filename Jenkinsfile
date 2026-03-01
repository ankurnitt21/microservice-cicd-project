pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Detect Branch') {
            steps {
                script {
                    echo "Current branch: ${env.GIT_BRANCH}"
                }
            }
        }

        stage('Build') {
            steps {
                echo "Building project"
            }
        }

        stage('Deploy to TEST') {
            when {
                expression { env.GIT_BRANCH.contains("develop") }
            }
            steps {
                echo "Deploying to TEST environment"
            }
        }

        stage('Deploy to STAGING') {
            when {
                expression { env.GIT_BRANCH.contains("release") }
            }
            steps {
                echo "Deploying to STAGING"
            }
        }

        stage('Deploy to PROD') {
            when {
                expression { env.GIT_BRANCH.contains("main") }
            }
            steps {
                echo "Deploying to PRODUCTION"
            }
        }

    }

}