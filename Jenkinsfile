pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Building project"
            }
        }

        stage('Test Environment') {
            when {
                branch 'develop'
            }
            steps {
                echo "Deploy to TEST"
            }
        }

        stage('Staging Environment') {
            when {
                branch 'release/*'
            }
            steps {
                echo "Deploy to STAGING"
            }
        }

        stage('Production Environment') {
            when {
                branch 'main'
            }
            steps {
                echo "Deploy to PRODUCTION"
            }
        }

    }
}