// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent any

    triggers {
        upstream(upstreamProjects: "ObjectStore/${env.BRANCH_NAME.replaceAll("/", "%2F")}",
                threshold: hudson.model.Result.FAILURE)
    }

    stages {
        stage('build-java') {
            steps {
                // Copied file exists on CI server only
                sh 'cp /var/my-private-files/private.properties ./gradle.properties'

                sh 'chmod +x gradlew'

                sh './gradlew --stacktrace ' +
                        '-Dextensive-tests=true ' +
                        'clean build uploadArchives -PpreferedRepo=local'
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
        }

        changed {
            slackSend color: "good",
                    message: "Changed to ${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }

        failure {
            slackSend color: "danger",
                    message: "Failed: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }
    }
}
