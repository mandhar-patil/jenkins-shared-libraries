#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags and push to GitHub
 */
def call(Map config = [:]) {
    def imageTag      = config.imageTag      ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName   = config.gitUserName   ?: 'Jenkins CI'
    def gitUserEmail  = config.gitUserEmail  ?: 'jenkins@example.com'
    def gitRepo       = config.gitRepo       ?: 'github.com/mandhar-patil/tws-e-commerce-app_hackathon.git'
    def gitBranch     = config.gitBranch     ?: 'master'   // change to 'main' if needed

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            set -e
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"

            echo "Updating manifests in ${manifestsPath}..."

            # Update main app deployment (handle spaces and quotes)
            sed -i "s|image:[[:space:]]*\\\"*.*easyshop-app:.*\\\"*|image: mandhar12/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml

            # Update migration job if present
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image:[[:space:]]*\\\"*.*easyshop-migration:.*\\\"*|image: mandhar12/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Update ingress domain if present
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host:[[:space:]]*.*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            echo "Result after update:"
            grep -r "image:" ${manifestsPath} || true

            # Commit + push if changes exist
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@${gitRepo}
                git push origin HEAD:${gitBranch}
            fi
        """
    }
}
