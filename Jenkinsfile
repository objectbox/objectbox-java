def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

// dev branch only: every 30 minutes at night (1:00 - 5:00)
String cronSchedule = BRANCH_NAME == 'dev' ? '*/30 1-5 * * *' : ''
String buildsToKeep = '500'

// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: buildsToKeep, artifactNumToKeepStr: buildsToKeep))
    }

    triggers {
        upstream(upstreamProjects: "ObjectBox-Linux/${env.BRANCH_NAME.replaceAll("/", "%2F")}",
                threshold: hudson.model.Result.SUCCESS)
        cron(cronSchedule)
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
                sh './test-with-asan.sh -Dextensive-tests=true clean test --tests io.objectbox.FunctionalTestSuite --tests io.objectbox.test.proguard.ObfuscatedEntityTest assemble'
            }
        }

        stage('upload-to-repo') {
            // Note: to avoid conflicts between snapshot versions, add the branch name
            // before '-SNAPSHOT' to the version string, like '1.2.3-branch-SNAPSHOT'
            when { expression { return BRANCH_NAME != 'publish' } }
            steps {
                sh './gradlew --stacktrace -PpreferedRepo=local uploadArchives'
            }
        }

        stage('upload-to-bintray') {
            when { expression { return BRANCH_NAME == 'publish' } }
            environment {
                BINTRAY_URL = credentials('bintray_url')
                BINTRAY_LOGIN = credentials('bintray_login')
            }
            steps {
                script {
                    slackSend color: "#42ebf4",
                            message: "Publishing ${currentBuild.fullDisplayName} to Bintray...\n${env.BUILD_URL}"
                }
                sh './gradlew --stacktrace -PpreferedRepo=${BINTRAY_URL} -PpreferedUsername=${BINTRAY_LOGIN_USR} -PpreferedPassword=${BINTRAY_LOGIN_PSW} uploadArchives'
                script {
                    slackSend color: "##41f4cd",
                            message: "Published ${currentBuild.fullDisplayName} successfully to Bintray - check https://bintray.com/objectbox/objectbox\n${env.BUILD_URL}"
                }
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
            slackSend color: COLOR_MAP[currentBuild.currentResult],
                    message: "Changed to ${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }

        failure {
            slackSend color: "danger",
                    message: "Failed: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}"
        }
    }
}
