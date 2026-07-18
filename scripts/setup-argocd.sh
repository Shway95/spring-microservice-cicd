#!/bin/bash
# Patch ArgoCD to NodePort
kubectl -n argocd delete svc argocd-server
kubectl -n argocd expose deployment argocd-server --type=NodePort --name=argocd-server --port=80 --target-port=8080
kubectl -n argocd expose deployment argocd-server --type=NodePort --name=argocd-server-https --port=443 --target-port=8080

# Get ArgoCD password
ARGO_PASS=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)
echo "ArgoCD Admin Password: $ARGO_PASS" > /home/ubuntu/argocd-credentials.txt

# Get NodePort
NODE_PORT=$(kubectl -n argocd get svc argocd-server -o jsonpath="{.spec.ports[0].nodePort}")
echo "ArgoCD NodePort: $NODE_PORT" >> /home/ubuntu/argocd-credentials.txt
echo "ArgoCD URL: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):$NODE_PORT" >> /home/ubuntu/argocd-credentials.txt
echo "Username: admin" >> /home/ubuntu/argocd-credentials.txt

# Add GitOps repo to ArgoCD
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
metadata:
  name: spring-microservice-gitops
  namespace: argocd
  labels:
    argocd.argoproj.io/secret-type: repository
type: Opaque
stringData:
  type: git
  url: https://github.com/Shway95/spring-microservice-gitops.git
EOF

# Create ArgoCD App for dev
cat <<EOF | kubectl apply -f -
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: spring-microservice-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/Shway95/spring-microservice-gitops.git
    targetRevision: main
    path: dev
  destination:
    server: https://kubernetes.default.svc
    namespace: dev
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
EOF

echo "=== SETUP COMPLETE ===" >> /home/ubuntu/argocd-credentials.txt
chown ubuntu:ubuntu /home/ubuntu/argocd-credentials.txt
