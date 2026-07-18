# 11 - Secrets, Tokens & Credentials Guide

This document lists EVERY credential needed for this project, where to get them, and where to put them.

---

## 🎯 Overview: All Credentials Needed

| # | Secret Name | What it is | Where it goes | Used by |
|---|-------------|-----------|---------------|---------|
| 1 | `DOCKERHUB_USERNAME` | Docker Hub username | GitHub Secrets | CD pipeline (push images) |
| 2 | `DOCKERHUB_TOKEN` | Docker Hub access token | GitHub Secrets | CD pipeline (push images) |
| 3 | `GITOPS_TOKEN` | GitHub Fine-Grained PAT | GitHub Secrets | CD pipeline (update GitOps repo) |
| 4 | `GITHUB_TOKEN` | Auto-provided by GitHub | Automatic (no setup) | CI/CD pipelines (tag creation, scans) |
| 5 | ArgoCD password | Initial admin password | Used for UI login | You (ArgoCD dashboard) |
| 6 | `k3s-cicd-key.pem` | SSH private key | Your local machine | SSH into EC2 instance |

---

## 1️⃣ DOCKERHUB_USERNAME

### What it is
Your Docker Hub login username. This is the same name that appears in your image path.

### How to find it
1. Go to https://hub.docker.com
2. Click your profile icon (top right)
3. Your username is shown there
4. It's also in your image name: `shwetang95/spring-microservice`
   - `shwetang95` ← this is the username

### Our value
```
shwetang95
```

---

## 2️⃣ DOCKERHUB_TOKEN

### What it is
An access token that lets the pipeline push images to Docker Hub. Safer than using your password.

### How to create it (Step by Step)

1. Go to https://hub.docker.com
2. Click **profile icon** → **Account Settings**
3. Click **Security** in the left sidebar
4. Click **Personal access tokens**
5. Click **"Generate New Token"**
6. Configure:
   - **Description:** `github-actions-cicd`
   - **Permissions:** **Read & Write** ✅
7. Click **"Generate"**
8. **COPY THE TOKEN IMMEDIATELY** (you won't see it again!)

### What permissions it needs

| Permission | Purpose | Required? |
|-----------|---------|-----------|
| Read | Pull images from Docker Hub | ✅ Yes |
| Write | Push new images to Docker Hub | ✅ Yes |
| Delete | Delete images from Docker Hub | ❌ No |

> **Choose "Read & Write"** — this is the minimum the pipeline needs.

### Token format
```
dckr_pat_XXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

---

## 3️⃣ GITOPS_TOKEN

### What it is
A GitHub **Fine-Grained Personal Access Token** that lets the CD pipeline push changes to the GitOps repository.

### Why we need it
The default `GITHUB_TOKEN` can only access the CURRENT repository. Our CD pipeline runs in `spring-microservice-cicd` but needs to push changes to `spring-microservice-gitops` (a DIFFERENT repo). So we need a PAT that has access to the GitOps repo.

### How to create it (Step by Step)

1. Go to https://github.com/settings/tokens?type=beta (Fine-grained tokens page)
2. Or navigate: GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
3. Click **"Generate new token"**
4. Configure:

| Field | Value |
|-------|-------|
| **Token name** | `gitops-cd-token` |
| **Expiration** | 90 days (or custom) |
| **Resource owner** | Your account (Shway95) |
| **Repository access** | "Only select repositories" |
| **Selected repositories** | `spring-microservice-gitops` |

5. Under **Permissions** → **Repository permissions:**

| Permission | Access Level | Why |
|-----------|-------------|-----|
| **Contents** | **Read and Write** ✅ | Push commits (update deployment.yml) |
| **Metadata** | Read (auto-selected) | Required for all tokens |

6. Click **"Generate token"**
7. **COPY THE TOKEN IMMEDIATELY**

### Token format
```
github_pat_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

### Critical details
- Only grant access to the GitOps repo, NOT all repos
- Only grant Contents: Read & Write (minimum permissions)
- Set an expiration (you'll need to rotate it periodically)
- If you get a `403` error pushing to GitOps, this token's permissions are wrong

---

## 4️⃣ GITHUB_TOKEN (Automatic)

### What it is
A special token that GitHub **automatically provides** to every workflow run. You don't create or store this — it just exists.

### How to use it
```yaml
${{ secrets.GITHUB_TOKEN }}    # Just reference it in your workflow
```

### What it can do (by default)
- Read repository contents
- Read/write packages
- Read/write issues and PRs
- Trigger other workflows

### What it CANNOT do
- Access OTHER repositories (only the repo the workflow runs in)
- Push to protected branches (without `permissions` block)
- Create tags (without `permissions: contents: write`)

### When to use the `permissions` block

```yaml
jobs:
  create-tag:
    permissions:
      contents: write    # ← Grants write access to THIS repo's contents
```

**Why?** By default, GITHUB_TOKEN has read-only access to contents. To push tags or create releases, you need to explicitly grant write permission.

### When GITHUB_TOKEN is NOT enough
| Need | Use GITHUB_TOKEN? | Alternative |
|------|-------------------|-------------|
| Push to SAME repo | ✅ Yes (with `permissions: contents: write`) | — |
| Push to DIFFERENT repo | ❌ No | Use a PAT (GITOPS_TOKEN) |
| Docker Hub login | ❌ No | Use DOCKERHUB_TOKEN |
| Read own repo contents | ✅ Yes (default) | — |

---

## 5️⃣ ArgoCD Admin Password

### What it is
The initial admin password for the ArgoCD web dashboard.

### How to retrieve it

```bash
# SSH into your EC2 instance first
ssh -i k3s-cicd-key.pem ubuntu@35.175.240.246

# Then run:
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d
```

### Or find it in the saved file

```bash
cat /home/ubuntu/argocd-credentials.txt
```

### Our credentials
| Field | Value |
|-------|-------|
| URL | http://35.175.240.246:30080 |
| Username | `admin` |
| Password | `XvKzqiOZtKDZCbLg` |

> ⚠️ **Best practice:** Change this password in production using `argocd account update-password`.

---

## 6️⃣ SSH Key (k3s-cicd-key.pem)

### What it is
An AWS EC2 key pair private key file. It's how you SSH into the EC2 instance that runs k3s and ArgoCD.

### How permissions work

**On Linux/Mac:**
```bash
chmod 400 k3s-cicd-key.pem    # Only owner can read
ssh -i k3s-cicd-key.pem ubuntu@35.175.240.246
```

**On Windows (PowerShell):**
```powershell
# Remove inherited permissions
icacls .\k3s-cicd-key.pem /inheritance:r

# Grant only your user read access
icacls .\k3s-cicd-key.pem /grant:r "$($env:USERNAME):(R)"

# SSH in
ssh -i .\k3s-cicd-key.pem ubuntu@35.175.240.246
```

**On Windows (if permissions error):**
```powershell
# Alternative: Right-click file → Properties → Security tab
# Remove all users except your own
# Set your user to "Read" only
```

> **Why permissions matter:** SSH refuses to use a private key file if other users on the system can read it. This is a security feature.

### Where this file lives
```
d:\Devops\AWS workflow\k3s-cicd-key.pem    ← Your local machine
```

---

## 🔧 Where Each Secret Goes

### GitHub Repository Secrets

These secrets go in your **application code repository** (`spring-microservice-cicd`):

**Navigation:** GitHub repo → Settings → Secrets and variables → Actions

| Secret Name | Value |
|-------------|-------|
| `DOCKERHUB_USERNAME` | `shwetang95` |
| `DOCKERHUB_TOKEN` | `dckr_pat_XXXXX...` |
| `GITOPS_TOKEN` | `github_pat_XXXXX...` |

> Note: `GITHUB_TOKEN` is automatic — don't add it manually!

---

## 📋 How to Add Secrets to GitHub (Step by Step)

### Step 1: Navigate to Secrets

1. Go to your repo: https://github.com/Shway95/spring-microservice-cicd
2. Click **Settings** tab (top navigation)
3. In the left sidebar, click **Secrets and variables**
4. Click **Actions**

### Step 2: Add a New Secret

1. Click **"New repository secret"**
2. Fill in:
   - **Name:** `DOCKERHUB_USERNAME` (exact name, case-sensitive)
   - **Secret:** `shwetang95` (the actual value)
3. Click **"Add secret"**

### Step 3: Repeat for Each Secret

Add all three secrets:

```
DOCKERHUB_USERNAME → shwetang95
DOCKERHUB_TOKEN    → dckr_pat_XXXXX... (from Docker Hub)
GITOPS_TOKEN       → github_pat_XXXXX... (from GitHub PAT page)
```

### Step 4: Verify

After adding, you should see all three listed:

```
Repository secrets:
  DOCKERHUB_TOKEN    Updated 2 days ago
  DOCKERHUB_USERNAME Updated 2 days ago
  GITOPS_TOKEN       Updated 2 days ago
```

> **You can never VIEW a secret after adding it.** You can only update or delete it. If you need to check the value, you'll need to check where you originally generated it.

---

## 🔒 Security Best Practices

1. **Never commit secrets to Git** — use GitHub Secrets or environment variables
2. **Use minimum permissions** — only grant what's needed (Read & Write, not Delete)
3. **Set expiration dates** — rotate tokens before they expire
4. **Use fine-grained PATs** — scope to specific repos, not "all repos"
5. **Use GITHUB_TOKEN when possible** — it's automatically rotated and scoped
6. **Base64 ≠ Encryption** — Kubernetes secrets are encoded, not encrypted
7. **Don't log secrets** — never `echo $SECRET` in pipeline scripts

---

## 🔄 Token Rotation Checklist

When a token expires, here's what to update:

| Token | Where to regenerate | Where to update |
|-------|--------------------|--------------------|
| DOCKERHUB_TOKEN | Docker Hub → Security → Tokens | GitHub Secrets |
| GITOPS_TOKEN | GitHub → Settings → Developer settings → PATs | GitHub Secrets |
| ArgoCD password | `argocd account update-password` | Your notes/team wiki |
| SSH key | AWS Console → EC2 → Key Pairs | Download new .pem file |

---

## 📝 Key Takeaways

1. **4 secrets needed** in GitHub: DOCKERHUB_USERNAME, DOCKERHUB_TOKEN, GITOPS_TOKEN (+ auto GITHUB_TOKEN)
2. **GITHUB_TOKEN** is automatic but needs `permissions: contents: write` for tags
3. **GITOPS_TOKEN** must be a fine-grained PAT with Contents: Read & Write on the GitOps repo
4. **Docker Hub token** needs Read & Write permissions (not Read-only)
5. **SSH key** permissions must be restricted (chmod 400 or Windows equivalent)
6. **Never commit secrets** — always use GitHub Secrets or sealed secrets
