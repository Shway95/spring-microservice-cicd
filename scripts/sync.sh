#!/bin/bash
# Force sync ArgoCD app
kubectl -n argocd patch app spring-microservice-dev -p '{"operation":{"initiatedBy":{"username":"admin"},"sync":{"syncStrategy":{"apply":{"force":true}}}}}' --type merge
