pipeline {
    agent any

    options {
        timestamps()
        gitLabConnection('GitLab')
    }

    parameters {
        string(name: 'SOURCE_BRANCH', defaultValue: '', description: 'MR source branch')
        string(name: 'TARGET_BRANCH', defaultValue: '', description: 'MR target branch')
        booleanParam(name: 'FORCE_BUILD', defaultValue: false, description: 'Build APK regardless of Android changes')
    }

    environment {
        REPOSITORY_URL = 'https://lab.ssafy.com/s14-final/S14P31S207.git'
        GIT_CREDENTIALS_ID = 'gitlab-read-token'
        ANDROID_BUILDER_IMAGE = 's14p31s207/android-builder:api35'
        HOST_JENKINS_HOME = '/home/ubuntu/ops/jenkins/jenkins_home'
    }

    stages {
        stage('Resolve Branches') {
            steps {
                script {
                    env.EFFECTIVE_SOURCE_BRANCH = params.SOURCE_BRANCH?.trim()
                    if (!env.EFFECTIVE_SOURCE_BRANCH) {
                        env.EFFECTIVE_SOURCE_BRANCH = env.gitlabSourceBranch?.trim()
                    }
                    if (!env.EFFECTIVE_SOURCE_BRANCH) {
                        env.EFFECTIVE_SOURCE_BRANCH = env.CHANGE_BRANCH?.trim()
                    }

                    env.EFFECTIVE_TARGET_BRANCH = env.gitlabTargetBranch?.trim()
                    if (!env.EFFECTIVE_TARGET_BRANCH) {
                        env.EFFECTIVE_TARGET_BRANCH = env.CHANGE_TARGET?.trim()
                    }
                    if (!env.EFFECTIVE_TARGET_BRANCH) {
                        env.EFFECTIVE_TARGET_BRANCH = params.TARGET_BRANCH?.trim()
                    }
                    if (!env.EFFECTIVE_TARGET_BRANCH) {
                        env.EFFECTIVE_TARGET_BRANCH = 'dev'
                    }

                    if (!env.EFFECTIVE_SOURCE_BRANCH) {
                        error 'SOURCE_BRANCH is required. Set SOURCE_BRANCH manually or run this job from a GitLab MR webhook.'
                    }

                    echo "source branch: ${env.EFFECTIVE_SOURCE_BRANCH}"
                    echo "target branch: ${env.EFFECTIVE_TARGET_BRANCH}"
                }
            }
        }

        stage('Skip Non-Dev Target') {
            when {
                expression { env.EFFECTIVE_TARGET_BRANCH != 'dev' }
            }
            steps {
                echo "skip android MR check for target branch: ${env.EFFECTIVE_TARGET_BRANCH}"
            }
        }

        stage('Checkout Source Branch') {
            when {
                expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
            }
            steps {
                git branch: env.EFFECTIVE_SOURCE_BRANCH,
                    credentialsId: env.GIT_CREDENTIALS_ID,
                    url: env.REPOSITORY_URL
            }
        }

        stage('Compare Target Branch') {
            when {
                expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
            }
            steps {
                sh '''
                    TARGET_REF="origin/$EFFECTIVE_TARGET_BRANCH"
                    git rev-parse "$TARGET_REF"
                    git diff --name-only "$TARGET_REF"...HEAD > changed-files.txt
                    cat changed-files.txt
                '''
            }
        }

        stage('Detect Android Changes') {
            when {
                expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
            }
            steps {
                script {
                    env.ANDROID_CHANGED = sh(
                        script: 'grep "^android/" changed-files.txt >/dev/null && echo true || echo false',
                        returnStdout: true
                    ).trim()
                }
            }
        }

        stage('Merge Target Branch') {
            when {
                expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
            }
            steps {
                sh '''
                    git config user.name "S14P31S207 Jenkins"
                    git config user.email "s14p31s207-jenkins@example.local"
                    git merge --no-ff --no-commit "origin/$EFFECTIVE_TARGET_BRANCH"
                '''
            }
        }

        stage('Build Android Builder Image') {
            when {
                anyOf {
                    allOf {
                        expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
                        expression { params.FORCE_BUILD }
                    }
                    allOf {
                        expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
                        expression { env.ANDROID_CHANGED == 'true' }
                    }
                }
            }
            steps {
                sh 'docker build -t "$ANDROID_BUILDER_IMAGE" -f infra/android-builder/Dockerfile infra/android-builder'
            }
        }

        stage('Build Merged Debug APK') {
            when {
                anyOf {
                    allOf {
                        expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
                        expression { params.FORCE_BUILD }
                    }
                    allOf {
                        expression { env.EFFECTIVE_TARGET_BRANCH == 'dev' }
                        expression { env.ANDROID_CHANGED == 'true' }
                    }
                }
            }
            steps {
                updateGitlabCommitStatus name: 'android-mr-check', state: 'running'
                gitlabCommitStatus(name: 'android-mr-check') {
                    sh '''
                        HOST_WORKSPACE="$(printf '%s' "$WORKSPACE" | sed "s#^/var/jenkins_home#$HOST_JENKINS_HOME#")"

                        docker run --rm \
                          -v "$HOST_WORKSPACE:/workspace" \
                          -v android-gradle-cache:/gradle-cache \
                          -e GRADLE_USER_HOME=/gradle-cache \
                          -w /workspace/android \
                          "$ANDROID_BUILDER_IMAGE" \
                          bash -lc 'printf "sdk.dir=/opt/android-sdk\\n" > local.properties && chmod +x ./gradlew && ./gradlew assembleDebug'
                    '''
                }
            }
        }
    }

    post {
        success {
            updateGitlabCommitStatus name: 'android-mr-check', state: 'success'
        }
        failure {
            updateGitlabCommitStatus name: 'android-mr-check', state: 'failed'
        }
        aborted {
            updateGitlabCommitStatus name: 'android-mr-check', state: 'canceled'
        }
        always {
            sh 'git merge --abort >/dev/null 2>&1 || true'
        }
    }
}
