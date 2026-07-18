#!/bin/bash
set -e

# Update system
apt-get update -y
apt-get upgrade -y

# Install k3s
curl -sfL https://get.k3s.io | sh -

# Wait for k3s to be ready
sleep 30
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

# Install ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
sleep 60

# Expose ArgoCD via NodePort on port 30080
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort", "ports": [{"port": 443, "targetPort": 8080, "nodePort": 30080}]}}'

# Create namespaces for environments
kubectl create namespace dev
kubectl create namespace test
kubectl create namespace prod

# Save ArgoCD initial admin password
sleep 30
ARGO_PASS=$(kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d)
echo "ArgoCD Admin Password: $ARGO_PASS" > /home/ubuntu/argocd-credentials.txt
chown ubuntu:ubuntu /home/ubuntu/argocd-credentials.txt

# Connect ArgoCD to GitOps repo
kubectl apply -f - <<EOF
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

# Create ArgoCD Application for dev environment
kubectl apply -f - <<EOF
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
echo "ArgoCD URL: https://<PUBLIC_IP>:30080" >> /home/ubuntu/argocd-credentials.txt
echo "Username: admin" >> /home/ubuntu/argocd-credentials.txt
