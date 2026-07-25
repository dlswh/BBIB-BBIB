pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        gitLabConnection('GitLab')
    }

    parameters {
        string(name: 'DEPLOY_BRANCH', defaultValue: 'dev', description: 'Git branch to deploy')
        booleanParam(name: 'FORCE_BUILD', defaultValue: false, description: 'Deploy frontend regardless of changed paths')
    }

    environment {
        REPOSITORY_URL = 'https://lab.ssafy.com/s14-final/S14P31S207.git'
        GIT_CREDENTIALS_ID = 'gitlab-read-token'
        FRONTEND_COMPOSE_FILE = 'infra/compose/docker-compose.frontend.yml'
        FRONTEND_IMAGE_NAME = 's14p31s207-frontend'
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

        stage('Detect Frontend Changes') {
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

                    env.FRONTEND_CHANGED = sh(
                        script: '''
                            if git rev-parse "$DIFF_BASE" >/dev/null 2>&1 && git rev-parse "$DIFF_HEAD" >/dev/null 2>&1; then
                              git diff --name-only "$DIFF_BASE" "$DIFF_HEAD" > changed-files.txt
                            else
                              git diff-tree --no-commit-id --name-only -r HEAD > changed-files.txt
                            fi

                            cat changed-files.txt
                            grep -E '^(web/|infra/frontend/|infra/compose/docker-compose\\.frontend\\.yml|infra/compose/\\.env\\.example|infra/jenkins/pipelines/frontend-dev-deploy\\.Jenkinsfile|\\.dockerignore$)' changed-files.txt >/dev/null && echo true || echo false
                        ''',
                        returnStdout: true
                    ).trim().split('\\n')[-1]

                    env.COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_BRANCH = env.EFFECTIVE_DEPLOY_BRANCH.replaceAll('[^A-Za-z0-9_.-]', '-')
                    env.FRONTEND_IMAGE_TAG = "${env.IMAGE_BRANCH}-${env.BUILD_NUMBER}-${env.COMMIT_SHORT}"

                    echo "frontend changed: ${env.FRONTEND_CHANGED}"
                    echo "diff base: ${env.DIFF_BASE}"
                    echo "diff head: ${env.DIFF_HEAD}"
                    echo "frontend image tag: ${env.FRONTEND_IMAGE_TAG}"
                }
            }
        }

        stage('Build Frontend Image') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.FRONTEND_CHANGED == 'true' }
                }
            }
            steps {
                updateGitlabCommitStatus name: 'frontend-dev-deploy', state: 'running'
                sh '''
                    COMPOSE_ENV_ARGS=""
                    if [ -f infra/compose/.env ]; then
                      COMPOSE_ENV_ARGS="--env-file infra/compose/.env"
                    fi

                    FRONTEND_IMAGE_TAG="$FRONTEND_IMAGE_TAG" docker compose \
                      -p s14p31s207-frontend \
                      $COMPOSE_ENV_ARGS \
                      -f "$FRONTEND_COMPOSE_FILE" \
                      build frontend
                '''
            }
        }

        stage('Deploy Frontend Container') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.FRONTEND_CHANGED == 'true' }
                }
            }
            steps {
                sh '''
                    COMPOSE_ENV_ARGS=""
                    if [ -f infra/compose/.env ]; then
                      COMPOSE_ENV_ARGS="--env-file infra/compose/.env"
                    fi

                    FRONTEND_IMAGE_TAG="$FRONTEND_IMAGE_TAG" docker compose \
                      -p s14p31s207-frontend \
                      $COMPOSE_ENV_ARGS \
                      -f "$FRONTEND_COMPOSE_FILE" \
                      up -d --no-build frontend
                '''
            }
        }

        stage('Health Check') {
            when {
                anyOf {
                    expression { params.FORCE_BUILD }
                    expression { env.FRONTEND_CHANGED == 'true' }
                }
            }
            steps {
                sh '''
                    for i in $(seq 1 20); do
                      HEALTH_STATUS="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' s207-frontend 2>/dev/null || true)"
                      if [ "$HEALTH_STATUS" = "healthy" ]; then
                        docker exec s207-frontend wget -qO- http://127.0.0.1/health >/dev/null
                        docker exec s207-frontend wget -qO- http://127.0.0.1/ >/dev/null
                        exit 0
                      fi

                      echo "frontend health status: ${HEALTH_STATUS:-unknown}"
                      sleep 1
                    done

                    docker ps -a --filter name=s207-frontend || true
                    docker inspect s207-frontend --format '{{json .State}}' || true
                    docker logs s207-frontend --tail=100 || true
                    exit 1
                '''
            }
        }
    }

    post {
        success {
            updateGitlabCommitStatus name: 'frontend-dev-deploy', state: 'success'
        }
        failure {
            updateGitlabCommitStatus name: 'frontend-dev-deploy', state: 'failed'
        }
        aborted {
            updateGitlabCommitStatus name: 'frontend-dev-deploy', state: 'canceled'
        }
    }
}
