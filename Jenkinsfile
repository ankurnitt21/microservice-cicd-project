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
            sh '''
            echo "Current workspace: ${WORKSPACE}"
            echo "Building eureka-server with Maven using tar copy method..."
            cd ${WORKSPACE}/eureka-server
            # Copy files into container, build, and copy target directory back using tar streams
            # Maven stdout is redirected to stderr (1>&2) to keep stdout clean for the tar pipe
            tar -czf - . | docker run --rm -i maven:3.9.9-eclipse-temurin-17 sh -c "tar -xzf - && mvn clean package -DskipTests 1>&2 && tar -czf - target/" | tar -xzf - -C ${WORKSPACE}/eureka-server/
            echo "Maven build completed. Checking for JAR file..."
            ls -lh ${WORKSPACE}/eureka-server/target/*.jar || echo "JAR file not found"
            '''
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

            docker run -d -p 8761:8761 --name eureka-test $IMAGE_NAME:latest
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

            docker run -d -p 8762:8761 --name eureka-staging $IMAGE_NAME:latest
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

            docker run -d -p 8763:8761 --name eureka-prod $IMAGE_NAME:latest
            '''
        }
    }

}

}
