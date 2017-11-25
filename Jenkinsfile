// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent any

    triggers {
        upstream(upstreamProjects: "ObjectStore/${env.BRANCH_NAME.replaceAll("/", "%2F")}",
                threshold: hudson.model.Result.FAILURE)
        cron ("*/20 0-6 * * *") // every 20 minutes at night (0:00 - 6:00)
    }

    stages {
        stage('init') {
            steps {
                // Copied file exists on CI server only
                sh 'cp /var/my-private-files/private.properties ./gradle.properties'

                sh 'chmod +x gradlew'

                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true' // "|| true" for an OK exit code if no file is found
            }
        }

        stage('build-java') {
            steps {
                sh './test-with-asan.sh -Dextensive-tests=true -PpreferedRepo=local clean build uploadArchives'
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
            archive 'tests/*/hs_err_pid*.log'
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
