// dev branch only: every 30 minutes at night (1:00 - 5:00)
String cronSchedule = BRANCH_NAME == 'dev' ? '*/30 1-5 * * *' : ''
String buildsToKeep = '500'

String gradleArgs = '--stacktrace'
boolean isPublish = BRANCH_NAME == 'publish'
String versionPostfix = isPublish ? '' : BRANCH_NAME // Build script detects empty string as not set.

// Note: using single quotes to avoid Groovy String interpolation leaking secrets.
def gitlabRepoArgs = '-PgitlabUrl=$GITLAB_URL -PgitlabPrivateToken=$GITLAB_TOKEN'
def uploadRepoArgsCentral = '-PsonatypeUsername=$OSSRH_LOGIN_USR -PsonatypePassword=$OSSRH_LOGIN_PSW'

// https://jenkins.io/doc/book/pipeline/syntax/
pipeline {
    agent { label 'java' }
    
    environment {
        GITLAB_URL = credentials('gitlab_url')
        GITLAB_TOKEN = credentials('GITLAB_TOKEN_ALL')
        // Note: for key use Jenkins secret file with PGP key as text in ASCII-armored format.
        ORG_GRADLE_PROJECT_signingKeyFile = credentials('objectbox_signing_key')
        ORG_GRADLE_PROJECT_signingKeyId = credentials('objectbox_signing_key_id')
        ORG_GRADLE_PROJECT_signingPassword = credentials('objectbox_signing_key_password')
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: buildsToKeep, artifactNumToKeepStr: buildsToKeep))
        timeout(time: 1, unit: 'HOURS') // If build hangs (regular build should be much quicker)
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
                sh 'chmod +x gradlew'
                sh 'chmod +x ci/test-with-asan.sh'
                sh './gradlew -version'

                // "|| true" for an OK exit code if no file is found
                sh 'rm tests/objectbox-java-test/hs_err_pid*.log || true'
            }
        }

        stage('build-java') {
            steps {
                sh "./ci/test-with-asan.sh $gradleArgs $gitlabRepoArgs -Dextensive-tests=true clean test spotbugsMain assemble"
            }
        }

        stage('upload-to-internal') {
            steps {
                sh "./gradlew $gradleArgs $gitlabRepoArgs -PversionPostFix=$versionPostfix publishMavenJavaPublicationToGitLabRepository"
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
                sh "./gradlew $gradleArgs $gitlabRepoArgs $uploadRepoArgsCentral publishMavenJavaPublicationToSonatypeRepository closeAndReleaseStagingRepository"

                googlechatnotification url: 'id:gchat_java',
                    message: "Published ${currentBuild.fullDisplayName} successfully to Central - check https://repo1.maven.org/maven2/io/objectbox/ in a few minutes.\n${env.BUILD_URL}"
            }
        }

    }

    // For global vars see /jenkins/pipeline-syntax/globals
    post {
        always {
            junit '**/build/test-results/**/TEST-*.xml'
            archiveArtifacts artifacts: 'tests/*/hs_err_pid*.log', allowEmptyArchive: true  // Only on JVM crash.
            recordIssues(tool: spotBugs(pattern: '**/build/reports/spotbugs/*.xml', useRankAsPriority: true))

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
