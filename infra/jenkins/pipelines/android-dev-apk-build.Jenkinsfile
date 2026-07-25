pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        gitLabConnection('GitLab')
    }

    parameters {
        booleanParam(name: 'FORCE_BUILD', defaultValue: false, description: 'Build APK regardless of Android changes')
    }

    environment {
        REPOSITORY_URL = 'https://lab.ssafy.com/s14-final/S14P31S207.git'
        GIT_CREDENTIALS_ID = 'gitlab-read-token'
        ANDROID_BUILDER_IMAGE = 's14p31s207/android-builder:api35'
        HOST_JENKINS_HOME = '/home/ubuntu/ops/jenkins/jenkins_home'
        GITLAB_API_URL = 'https://lab.ssafy.com/api/v4'
        GITLAB_PROJECT_ID = '1304877'
        GITLAB_PACKAGE_NAME = 'android-debug-apk'
        GITLAB_PACKAGE_CREDENTIALS_ID = 'gitlab-package-token'
    }

    stages {
        stage('Checkout Dev') {
            steps {
                git branch: 'dev',
                    credentialsId: env.GIT_CREDENTIALS_ID,
                    url: env.REPOSITORY_URL
            }
        }

        stage('Detect Android Changes') {
            steps {
                script {
                    env.DIFF_BASE = env.gitlabBefore?.trim()
                    if (!env.DIFF_BASE || env.DIFF_BASE ==~ /^0+$/) {
                        env.DIFF_BASE = 'HEAD~1'
                    }

                    env.DIFF_HEAD = env.gitlabAfter?.trim()
                    if (!env.DIFF_HEAD || env.DIFF_HEAD ==~ /^0+$/) {
                        env.DIFF_HEAD = 'HEAD'
                    }

                    env.ANDROID_CHANGED = sh(
                        script: '''
                            if git rev-parse "$DIFF_BASE" >/dev/null 2>&1 && git rev-parse "$DIFF_HEAD" >/dev/null 2>&1; then
                              git diff --name-only "$DIFF_BASE" "$DIFF_HEAD" > changed-files.txt
                            else
                              git diff-tree --no-commit-id --name-only -r HEAD > changed-files.txt
                            fi

                            cat changed-files.txt
                            grep '^android/' changed-files.txt >/dev/null && echo true || echo false
                        ''',
                        returnStdout: true
                    ).trim().split('\\n')[-1]

                    echo "android changed: ${env.ANDROID_CHANGED}"
                    echo "diff base: ${env.DIFF_BASE}"
                    echo "diff head: ${env.DIFF_HEAD}"
                }
            }
        }

        stage('Build Android Builder Image') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.ANDROID_CHANGED == 'true' }
                }
            }
            steps {
                sh 'docker build -t "$ANDROID_BUILDER_IMAGE" -f infra/android-builder/Dockerfile infra/android-builder'
            }
        }

        stage('Build Debug APK') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.ANDROID_CHANGED == 'true' }
                }
            }
            steps {
                sh '''
                    HOST_WORKSPACE="$(printf '%s' "$WORKSPACE" | sed "s#^/var/jenkins_home#$HOST_JENKINS_HOME#")"

                    docker run --rm \
                      -v "$HOST_WORKSPACE:/workspace" \
                      -v android-gradle-cache:/gradle-cache \
                      -e GRADLE_USER_HOME=/gradle-cache \
                      -w /workspace/android \
                      "$ANDROID_BUILDER_IMAGE" \
                      bash -lc 'printf "sdk.dir=/opt/android-sdk\\n" > local.properties && chmod +x ./gradlew && ./gradlew clean assembleDebug'
                '''
            }
        }

        stage('Archive APK') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.ANDROID_CHANGED == 'true' }
                }
            }
            steps {
                archiveArtifacts artifacts: 'android/app/build/outputs/apk/**/*.apk',
                                 fingerprint: true
            }
        }

        stage('Upload APK to GitLab Package Registry') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.ANDROID_CHANGED == 'true' }
                }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.GITLAB_PACKAGE_CREDENTIALS_ID,
                    usernameVariable: 'GITLAB_PACKAGE_USER',
                    passwordVariable: 'GITLAB_PACKAGE_TOKEN'
                )]) {
                    sh '''
                        APK_PATH="$(find android/app/build/outputs/apk/debug -type f -name "*.apk" | head -n 1)"

                        if [ -z "$APK_PATH" ]; then
                          echo "Debug APK not found."
                          exit 1
                        fi

                        COMMIT_SHORT="$(git rev-parse --short HEAD)"
                        BUILD_TIMESTAMP="$(date +%Y%m%d-%H%M)"
                        PACKAGE_VERSION="dev-${BUILD_TIMESTAMP}-${BUILD_NUMBER}-${COMMIT_SHORT}"
                        FILE_NAME="app-debug-${COMMIT_SHORT}.apk"
                        PACKAGE_URL="${GITLAB_API_URL}/projects/${GITLAB_PROJECT_ID}/packages/generic/${GITLAB_PACKAGE_NAME}/${PACKAGE_VERSION}/${FILE_NAME}"

                        curl --fail --location \
                          --user "${GITLAB_PACKAGE_USER}:${GITLAB_PACKAGE_TOKEN}" \
                          --upload-file "$APK_PATH" \
                          "$PACKAGE_URL"

                        echo "Uploaded APK to GitLab Package Registry: ${GITLAB_PACKAGE_NAME}/${PACKAGE_VERSION}/${FILE_NAME}"
                    '''
                }
            }
        }
    }

    post {
        success {
            updateGitlabCommitStatus name: 'android-dev-apk-build', state: 'success'
        }
        failure {
            updateGitlabCommitStatus name: 'android-dev-apk-build', state: 'failed'
        }
        aborted {
            updateGitlabCommitStatus name: 'android-dev-apk-build', state: 'canceled'
        }
    }
}
