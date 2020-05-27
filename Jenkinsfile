pipeline {
    agent {
            label 'Plugins'
    }
    parameters {

        string(
            name: 'commitId',
            defaultValue: '*/master',
            description: 'Commit id to build.'
        )
    }

    stages {
        stage('Checkout') {
            steps {
                echo "checkout branch: ${env.BRANCH_NAME}"
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    bat "mvn clean process-resources install verify"
                }
            }
        }
    }
}