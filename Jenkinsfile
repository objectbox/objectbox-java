def COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']

// dev branch only: every 30 minutes at night (1:00 - 5:00)
String cronSchedule = BRANCH_NAME == 'dev' ? '*/30 1-5 * * *' : ''
String buildsToKeep = '500'

String gradleArgs = '-Dorg.gradle.daemon=false --stacktrace'
def publishBranch = 'publish'
String versionPostfix = BRANCH_NAME == 'dev' ? '' : BRANCH_NAME // build script detects empty string as not set

// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent { label 'java' }
    
    environment {
        GITLAB_URL = credentials('gitlab_url')
        MVN_REPO_URL = credentials('objectbox_internal_mvn_repo_http')
        MVN_REPO_URL_PUBLISH = credentials('objectbox_internal_mvn_repo')
        MVN_REPO_LOGIN = credentials('objectbox_internal_mvn_user')
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
                sh "./test-with-asan.sh -Dextensive-tests=true " +
                        "-PinternalObjectBoxRepo=${MVN_REPO_URL} -PinternalObjectBoxRepoUser=${MVN_REPO_LOGIN_USR} -PinternalObjectBoxRepoPassword=${MVN_REPO_LOGIN_PSW} " +
                        "clean test " +
                        "--tests io.objectbox.FunctionalTestSuite " +
                        "--tests io.objectbox.test.proguard.ObfuscatedEntityTest " +
                        "--tests io.objectbox.rx.QueryObserverTest " +
                        "assemble"
            }
        }

        stage('upload-to-repo') {
            when { expression { return BRANCH_NAME != publishBranch } }
            steps {
                sh "./gradlew $gradleArgs " +
                   "-PversionPostFix=${versionPostfix} " +
                   "-PpreferredRepo=${MVN_REPO_URL_PUBLISH} -PpreferredUsername=${MVN_REPO_LOGIN_USR} -PpreferredPassword=${MVN_REPO_LOGIN_PSW} " +
                   "uploadArchives"
            }
        }

        stage('upload-to-bintray') {
            when { expression { return BRANCH_NAME == publishBranch } }
            environment {
                BINTRAY_URL = credentials('bintray_url')
                BINTRAY_LOGIN = credentials('bintray_login')
            }
            steps {
                googlechatnotification url: 'id:gchat_java',
                    message: "*Publishing* ${currentBuild.fullDisplayName} to Bintray...\n${env.BUILD_URL}"

                sh "./gradlew $gradleArgs -PpreferredRepo=${BINTRAY_URL} -PpreferredUsername=${BINTRAY_LOGIN_USR} -PpreferredPassword=${BINTRAY_LOGIN_PSW} uploadArchives"

                googlechatnotification url: 'id:gchat_java',
                    message: "Published ${currentBuild.fullDisplayName} successfully to Bintray - check https://bintray.com/objectbox/objectbox\n${env.BUILD_URL}"
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
            archive 'tests/*/hs_err_pid*.log'
            archive '**/build/reports/findbugs/*'

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
