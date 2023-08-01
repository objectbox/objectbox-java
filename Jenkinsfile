// dev branch only: run every hour at 30th minute at night (1:00 - 5:00)
// Avoid running at the same time as integration tests: uses this projects snapshots
// so make sure to not run in the middle of uploading a new snapshot to avoid dependency resolution errors.
String cronSchedule = BRANCH_NAME == 'dev' ? '30 1-5 * * *' : ''
String buildsToKeep = '500'

String gradleArgs = '--stacktrace'
boolean isPublish = BRANCH_NAME == 'publish'
String versionPostfix = isPublish ? '' : BRANCH_NAME // Build script detects empty string as not set.

// Note: using single quotes to avoid Groovy String interpolation leaking secrets.
def signingArgs = '-PsigningKeyFile=$SIGNING_FILE -PsigningKeyId=$SIGNING_ID -PsigningPassword=$SIGNING_PWD'
def gitlabRepoArgs = '-PgitlabUrl=$GITLAB_URL -PgitlabPrivateToken=$GITLAB_TOKEN'
def uploadRepoArgsCentral = '-PsonatypeUsername=$OSSRH_LOGIN_USR -PsonatypePassword=$OSSRH_LOGIN_PSW'

boolean startedByTimer = currentBuild.getBuildCauses('hudson.triggers.TimerTrigger$TimerTriggerCause').size() > 0
def buildCauses = currentBuild.getBuildCauses()
echo "startedByTimer=$startedByTimer, build causes: $buildCauses"

// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent { label 'java' }
    
    environment {
        GITLAB_URL = credentials('gitlab_url')
        GITLAB_TOKEN = credentials('GITLAB_TOKEN_ALL')
        GITLAB_INTEG_TESTS_TRIGGER_URL = credentials('gitlab-trigger-java-integ-tests')
        // Note: for key use Jenkins secret file with PGP key as text in ASCII-armored format.
        SIGNING_FILE = credentials('objectbox_signing_key')
        SIGNING_ID = credentials('objectbox_signing_key_id')
        SIGNING_PWD = credentials('objectbox_signing_key_password')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: buildsToKeep, artifactNumToKeepStr: buildsToKeep))
        timeout(time: 1, unit: 'HOURS') // If build hangs (regular build should be much quicker)
        disableConcurrentBuilds() // limit to 1 job per branch
        gitLabConnection('objectbox-gitlab-connection')
    }

    triggers {
        upstream(upstreamProjects: "ObjectBox-Linux/${env.BRANCH_NAME.replaceAll("/", "%2F")}",
                threshold: hudson.model.Result.SUCCESS)
        cron(cronSchedule)
    }

    stages {
        stage('init') {
            steps {
                sh './gradlew -version'

                // "|| true" for an OK exit code if no file is found
                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true'
            }
        }

        stage('build-java') {
            steps {
                sh "./scripts/test-with-asan.sh $gradleArgs $signingArgs $gitlabRepoArgs clean build"
            }
            post {
                always {
                    junit '**/build/test-results/**/TEST-*.xml'
                    archiveArtifacts artifacts: 'tests/*/hs_err_pid*.log', allowEmptyArchive: true  // Only on JVM crash.
                    recordIssues(tool: spotBugs(pattern: '**/build/reports/spotbugs/*.xml', useRankAsPriority: true))
                }
            }
        }

        // Test oldest supported and a recent JDK.
        // Note: can not run these in parallel using a matrix configuration as Gradle would step over itself.
        // Also shouldn't add agent config to avoid that as it triggers a separate job which can easily cause deadlocks.
        stage("test-jdk-8") {
            environment {
                TEST_JDK = "8"
            }
            steps {
                // "|| true" for an OK exit code if no file is found
                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true'
                // Note: do not run check task as it includes SpotBugs.
                sh "./scripts/test-with-asan.sh $gradleArgs $gitlabRepoArgs clean :tests:objectbox-java-test:test"
            }
            post {
                always {
                    junit '**/build/test-results/**/TEST-*.xml'
                    archiveArtifacts artifacts: 'tests/*/hs_err_pid*.log', allowEmptyArchive: true  // Only on JVM crash.
                }
            }
        }
        stage("test-jdk-16") {
            environment {
                TEST_JDK = "16"
            }
            steps {
                // "|| true" for an OK exit code if no file is found
                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true'
                // Note: do not run check task as it includes SpotBugs.
                sh "./scripts/test-with-asan.sh $gradleArgs $gitlabRepoArgs clean :tests:objectbox-java-test:test"
            }
            post {
                always {
                    junit '**/build/test-results/**/TEST-*.xml'
                    archiveArtifacts artifacts: 'tests/*/hs_err_pid*.log', allowEmptyArchive: true  // Only on JVM crash.
                }
            }
        }

        stage('upload-to-internal') {
            steps {
                sh "./gradlew $gradleArgs $signingArgs $gitlabRepoArgs -PversionPostFix=$versionPostfix publishMavenJavaPublicationToGitLabRepository"
            }
        }

        stage('upload-to-central') {
            when { expression { return isPublish } }
            environment {
                OSSRH_LOGIN = credentials('ossrh-login')
            }
            steps {
                googlechatnotification url: 'id:gchat_java',
                    message: "*Publishing* ${currentBuild.fullDisplayName} to Central...\n${env.BUILD_URL}"

                // Note: supply internal repo as tests use native dependencies that might not be published, yet.
                sh "./gradlew $gradleArgs $signingArgs $gitlabRepoArgs $uploadRepoArgsCentral publishMavenJavaPublicationToSonatypeRepository closeAndReleaseStagingRepository"

                googlechatnotification url: 'id:gchat_java',
                    message: "Published ${currentBuild.fullDisplayName} successfully to Central - check https://repo1.maven.org/maven2/io/objectbox/ in a few minutes.\n${env.BUILD_URL}"
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            googlechatnotification url: 'id:gchat_java', message: "${currentBuild.currentResult}: ${currentBuild.fullDisplayName}\n${env.BUILD_URL}",
                                   notifyFailure: 'true', notifyUnstable: 'true', notifyBackToNormal: 'true'
        }

        failure {
            updateGitlabCommitStatus name: 'build', state: 'failed'

            emailext (
                subject: "${currentBuild.currentResult}: ${currentBuild.fullDisplayName}",
                mimeType: 'text/html',
                recipientProviders: [[$class: 'DevelopersRecipientProvider']],
                body: """
                    <p>${currentBuild.currentResult}:
                        <a href='${env.BUILD_URL}'>${currentBuild.fullDisplayName}</a>
                        (<a href='${env.BUILD_URL}/console'>console</a>)
                    </p>
                    <p>Git: ${GIT_COMMIT} (${GIT_BRANCH})
                    <p>Build time: ${currentBuild.durationString}
                """
            )
        }

        success {
            updateGitlabCommitStatus name: 'build', state: 'success'
            script {
                if (startedByTimer) {
                    echo "Started by timer, not triggering integration tests"
                } else {
                  // Trigger integration tests in GitLab
                  // URL configured like <host>/api/v4/projects/<id>/trigger/pipeline?token=<token>
                  // Note: do not fail on error in case ref does not exist, only output response
                  // --silent --show-error disable progress output but still show errors
                  sh 'curl --silent --show-error -X POST "$GITLAB_INTEG_TESTS_TRIGGER_URL&ref=$GIT_BRANCH"'
                }
            }
        }
    }
}
