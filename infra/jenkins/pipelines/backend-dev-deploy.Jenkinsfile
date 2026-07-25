pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        gitLabConnection('GitLab')
    }

    parameters {
        string(name: 'DEPLOY_BRANCH', defaultValue: 'dev', description: 'Git branch to deploy')
        booleanParam(name: 'FORCE_BUILD', defaultValue: false, description: 'Deploy backend regardless of changed paths')
    }

    environment {
        REPOSITORY_URL = 'https://lab.ssafy.com/s14-final/S14P31S207.git'
        GIT_CREDENTIALS_ID = 'gitlab-read-token'
        COMPOSE_ENV_CREDENTIALS_ID = 's207-compose-env'
        BACKEND_COMPOSE_FILE = 'infra/compose/docker-compose.backend.yml'
    }

    stages {
        stage('Checkout Deploy Branch') {
            steps {
                script {
                    env.EFFECTIVE_DEPLOY_BRANCH = params.DEPLOY_BRANCH?.trim()
                    if (!env.EFFECTIVE_DEPLOY_BRANCH) {
                        env.EFFECTIVE_DEPLOY_BRANCH = 'dev'
                    }

                    echo "deploy branch: ${env.EFFECTIVE_DEPLOY_BRANCH}"
                }

                git branch: env.EFFECTIVE_DEPLOY_BRANCH,
                    credentialsId: env.GIT_CREDENTIALS_ID,
                    url: env.REPOSITORY_URL
            }
        }

        stage('Detect Backend Changes') {
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

                    env.BACKEND_CHANGED = sh(
                        script: '''
                            if git rev-parse "$DIFF_BASE" >/dev/null 2>&1 && git rev-parse "$DIFF_HEAD" >/dev/null 2>&1; then
                              git diff --name-only "$DIFF_BASE" "$DIFF_HEAD" > changed-files.txt
                            else
                              git diff-tree --no-commit-id --name-only -r HEAD > changed-files.txt
                            fi

                            cat changed-files.txt
                            grep -E '^(backend/|infra/backend/|infra/compose/docker-compose\\.backend\\.yml|infra/compose/\\.env\\.example|infra/jenkins/pipelines/backend-dev-deploy\\.Jenkinsfile|infra/nginx/backend|\\.dockerignore$)' changed-files.txt >/dev/null && echo true || echo false
                        ''',
                        returnStdout: true
                    ).trim().split('\\n')[-1]

                    env.COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_BRANCH = env.EFFECTIVE_DEPLOY_BRANCH.replaceAll('[^A-Za-z0-9_.-]', '-')
                    env.BACKEND_IMAGE_TAG = "${env.IMAGE_BRANCH}-${env.BUILD_NUMBER}-${env.COMMIT_SHORT}"

                    echo "backend changed: ${env.BACKEND_CHANGED}"
                    echo "diff base: ${env.DIFF_BASE}"
                    echo "diff head: ${env.DIFF_HEAD}"
                    echo "backend image tag: ${env.BACKEND_IMAGE_TAG}"
                }
            }
        }

        stage('Build Backend Image') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.BACKEND_CHANGED == 'true' }
                }
            }
            steps {
                updateGitlabCommitStatus name: 'backend-dev-deploy', state: 'running'
                withCredentials([file(credentialsId: env.COMPOSE_ENV_CREDENTIALS_ID, variable: 'COMPOSE_ENV_FILE')]) {
                    sh '''
                        mkdir -p infra/compose
                        cp "$COMPOSE_ENV_FILE" infra/compose/.env
                        chmod 600 infra/compose/.env

                        BACKEND_IMAGE_TAG="$BACKEND_IMAGE_TAG" docker compose \
                          -p s14p31s207-backend \
                          --env-file infra/compose/.env \
                          -f "$BACKEND_COMPOSE_FILE" \
                          build backend
                    '''
                }
            }
        }

        stage('Deploy Backend Container') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.BACKEND_CHANGED == 'true' }
                }
            }
            steps {
                withCredentials([file(credentialsId: env.COMPOSE_ENV_CREDENTIALS_ID, variable: 'COMPOSE_ENV_FILE')]) {
                    sh '''
                        mkdir -p infra/compose
                        cp "$COMPOSE_ENV_FILE" infra/compose/.env
                        chmod 600 infra/compose/.env

                        BACKEND_IMAGE_TAG="$BACKEND_IMAGE_TAG" docker compose \
                          -p s14p31s207-backend \
                          --env-file infra/compose/.env \
                          -f "$BACKEND_COMPOSE_FILE" \
                          up -d --no-build backend
                    '''
                }
            }
        }

        stage('Health Check') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.BACKEND_CHANGED == 'true' }
                }
            }
            steps {
                sh '''
                    for i in $(seq 1 30); do
                      HEALTH_STATUS="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' s207-backend 2>/dev/null || true)"
                      if [ "$HEALTH_STATUS" = "healthy" ]; then
                        docker exec s207-backend wget -qO- "http://127.0.0.1:8080/api/actuator/health" | grep '"status":"UP"'
                        exit 0
                      fi

                      echo "backend health status: ${HEALTH_STATUS:-unknown}"
                      sleep 1
                    done

                    docker ps -a --filter name=s207-backend || true
                    docker inspect s207-backend --format '{{json .State}}' || true
                    docker logs s207-backend --tail=100 || true
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            updateGitlabCommitStatus name: 'backend-dev-deploy', state: 'success'
        }
        failure {
            updateGitlabCommitStatus name: 'backend-dev-deploy', state: 'failed'
        }
        aborted {
            updateGitlabCommitStatus name: 'backend-dev-deploy', state: 'canceled'
        }
    }
}
