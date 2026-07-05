# Zeka Kubernetes Deployment Script
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   ZEKA KUBERNETES DEPLOYMENT STARTING   " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Check if kubectl is installed
if (-not (Get-Command "kubectl" -ErrorAction SilentlyContinue)) {
    Write-Error "Hata: 'kubectl' kurulu değil veya PATH çevre değişkenlerinde bulunamadı."
    Write-Host "Lütfen Docker Desktop (Kubernetes enabled) veya Minikube kurulumunu yapın." -ForegroundColor Yellow
    Exit 1
}

# 2. Deploy Secrets
Write-Host "`n[1/3] Gizli Anahtarlar (Secrets) uygulanıyor..." -ForegroundColor Green
kubectl apply -f k8s-secrets.yaml

# 3. Deploy Databases (Postgres & Redis)
Write-Host "`n[2/3] Veritabanları (PostgreSQL & Redis) ayağa kaldırılıyor..." -ForegroundColor Green
kubectl apply -f k8s-db.yaml

# 4. Deploy Backend Application
Write-Host "`n[3/3] Zeka Backend Uygulaması deploy ediliyor..." -ForegroundColor Green
kubectl apply -f k8s-backend.yaml

Write-Host "`n=========================================" -ForegroundColor Cyan
Write-Host "  KUBERNETES DAĞITIMI BAŞARIYLA BAŞLATILDI " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

Write-Host "`nUygulama durumunu takip etmek için:" -ForegroundColor Gray
Write-Host "  kubectl get pods -w" -ForegroundColor Yellow

Write-Host "`nMinikube üzerinde NodePort URL'sini almak için:" -ForegroundColor Gray
Write-Host "  minikube service zeka-backend-service --url" -ForegroundColor Yellow
