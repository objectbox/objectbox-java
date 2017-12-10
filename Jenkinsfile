// dev branch only: every 30 minutes at night (0:00 - 6:00)
String cronSchedule = BRANCH_NAME == 'dev' ? '*/30 0-6 * * *' : ''

// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent any

    triggers {
        upstream(upstreamProjects: "ObjectStore/${env.BRANCH_NAME.replaceAll("/", "%2F")}",
                threshold: hudson.model.Result.FAILURE)
        cron (cronSchedule)
    }

    stages {
        stage('init') {
            steps {
                // Copied file exists on CI server only
                sh 'cp /var/my-private-files/private.properties ./gradle.properties'

                sh 'chmod +x gradlew'

                // "|| true" for an OK exit code if no file is found
                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true'
            }
        }

        stage('build-java') {
            steps {
                sh './test-with-asan.sh -Dextensive-tests=true clean build'
            }
        }

        stage('upload-to-repo') {
            // By default, only dev and master branches deploy to repo to avoid messing in the same SNAPSHOT version
            // (e.g. this avoids integration tests to pick it up the version).
            when { expression { return BRANCH_NAME == 'dev' || BRANCH_NAME == 'master' } }
            steps {
                sh './gradlew --stacktrace -PpreferedRepo=local uploadArchives'
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
            archive 'tests/*/hs_err_pid*.log'
            archive '**/build/reports/findbugs/*'
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
