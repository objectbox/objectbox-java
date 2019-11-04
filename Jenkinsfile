def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

// dev branch only: every 30 minutes at night (1:00 - 5:00)
String cronSchedule = BRANCH_NAME == 'dev' ? '*/30 1-5 * * *' : ''
String buildsToKeep = '500'

String gradleArgs = '-Dorg.gradle.daemon=false --stacktrace'
boolean isPublish = BRANCH_NAME == 'publish'
String versionPostfix = isPublish ? '' : BRANCH_NAME // Build script detects empty string as not set.

// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent { label 'java' }
    
    environment {
        GITLAB_URL = credentials('gitlab_url')
        MVN_REPO_LOGIN = credentials('objectbox_internal_mvn_user')
        MVN_REPO_URL = credentials('objectbox_internal_mvn_repo_http')
        MVN_REPO_ARGS = "-PinternalObjectBoxRepo=$MVN_REPO_URL " +
                        "-PinternalObjectBoxRepoUser=$MVN_REPO_LOGIN_USR " +
                        "-PinternalObjectBoxRepoPassword=$MVN_REPO_LOGIN_PSW"
        MVN_REPO_UPLOAD_URL = credentials('objectbox_internal_mvn_repo')
        MVN_REPO_UPLOAD_ARGS = "-PpreferredRepo=$MVN_REPO_UPLOAD_URL " +
                        "-PpreferredUsername=$MVN_REPO_LOGIN_USR " +
                        "-PpreferredPassword=$MVN_REPO_LOGIN_PSW " +
                        "-PversionPostFix=$versionPostfix"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: buildsToKeep, artifactNumToKeepStr: buildsToKeep))
        timeout(time: 1, unit: 'HOURS') // If build hangs (regular build should be much quicker)
        gitLabConnection("${env.GITLAB_URL}")
    }

    triggers {
        upstream(upstreamProjects: "ObjectBox-Linux/${env.BRANCH_NAME.replaceAll("/", "%2F")}",
                threshold: hudson.model.Result.SUCCESS)
        cron(cronSchedule)
    }

    stages {
        stage('init') {
            steps {
                sh 'chmod +x gradlew'

                // "|| true" for an OK exit code if no file is found
                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true'
            }
        }

        stage('build-java') {
            steps {
                sh "./test-with-asan.sh -Dextensive-tests=true $MVN_REPO_ARGS " +
                        "clean test " +
                        "--tests io.objectbox.FunctionalTestSuite " +
                        "--tests io.objectbox.test.proguard.ObfuscatedEntityTest " +
                        "--tests io.objectbox.rx.QueryObserverTest " +
                        "assemble"
            }
        }

        stage('upload-to-internal') {
            steps {
                sh "./gradlew $gradleArgs $MVN_REPO_ARGS $MVN_REPO_UPLOAD_ARGS uploadArchives"
            }
        }

        stage('upload-to-bintray') {
            when { expression { return isPublish } }
            environment {
                BINTRAY_URL = credentials('bintray_url')
                BINTRAY_LOGIN = credentials('bintray_login')
            }
            steps {
                googlechatnotification url: 'id:gchat_java',
                    message: "*Publishing* ${currentBuild.fullDisplayName} to Bintray...\n${env.BUILD_URL}"

                // Note: supply internal Maven repo as tests use native dependencies (can't publish those without the Java libraries).
                // Note: add quotes around URL parameter to avoid line breaks due to semicolon in URL.
                sh "./gradlew $gradleArgs $MVN_REPO_ARGS " +
                   "\"-PpreferredRepo=${BINTRAY_URL}\" -PpreferredUsername=${BINTRAY_LOGIN_USR} -PpreferredPassword=${BINTRAY_LOGIN_PSW} " +
                   "uploadArchives"

                googlechatnotification url: 'id:gchat_java',
                    message: "Published ${currentBuild.fullDisplayName} successfully to Bintray - check https://bintray.com/objectbox/objectbox\n${env.BUILD_URL}"
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
            archiveArtifacts artifacts: 'tests/*/hs_err_pid*.log', allowEmptyArchive: true  // Only on JVM crash.
            // currently unused: archiveArtifacts '**/build/reports/findbugs/*'

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
        }
    }
}
