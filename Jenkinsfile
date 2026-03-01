pipeline {

agent any

environment {
    IMAGE_NAME = "eureka-server"
}

stages {

    stage('Checkout') {
        steps {
            checkout scm
        }
    }

    stage('Build (Maven)') {
        steps {
            dir('eureka-server') {
                sh 'mvn clean package -DskipTests'
            }
        }
    }

    stage('Build Docker Image') {
        steps {
            sh '''
            docker build -t $IMAGE_NAME:latest ./eureka-server
            '''
        }
    }

    stage('Deploy to TEST') {
        when {
            branch 'develop'
        }
        steps {
            sh '''
            docker stop eureka-test || true
            docker rm eureka-test || true

            docker run -d -p 8761:8761 \
            --name eureka-test \
            $IMAGE_NAME:latest
            '''
        }
    }

    stage('Deploy to STAGING') {
        when {
            branch pattern: "release/.*", comparator: "REGEXP"
        }
        steps {
            sh '''
            docker stop eureka-staging || true
            docker rm eureka-staging || true

            docker run -d -p 8762:8761 \
            --name eureka-staging \
            $IMAGE_NAME:latest
            '''
        }
    }

    stage('Deploy to PRODUCTION') {
        when {
            branch 'main'
        }
        steps {
            sh '''
            docker stop eureka-prod || true
            docker rm eureka-prod || true

            docker run -d -p 8763:8761 \
            --name eureka-prod \
            $IMAGE_NAME:latest
            '''
        }
    }

}

}
