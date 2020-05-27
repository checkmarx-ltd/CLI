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
                script {
                    // Get code from git.
                    currentRevision = checkout([
                        $class: 'GitSCM',
                        branches: [[name : "${params.commitId}"]],
                        userRemoteConfigs: [[
                            url: 'https://github.com/checkmarx-ltd/CLI.git'
                        ]]
                    ])
                }
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