pipeline {
  agent any
  options { timestamps(); ansiColor('xterm') }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Test') {
      steps { sh './gradlew clean test jacocoTestReport --no-daemon' }
      post {
        always {
          junit 'build/test-results/test/*.xml'
        }
        success {
          // Archive the JaCoCo HTML report so you can view it from Jenkins
          archiveArtifacts artifacts: 'build/reports/jacoco/test/html/**', fingerprint: false
        }
      }
    }

    stage('Package') {
      steps { sh './gradlew jar --no-daemon' }
      post {
        success { archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true }
      }
    }
  }

  post {
    always {
      echo "Build finished: ${currentBuild.currentResult}"
    }
  }
}
