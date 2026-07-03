# Kubernetes deployment

```bash
kubectl apply -f namespace.yaml
kubectl create secret generic pcp-secrets -n pcp \
  --from-literal=JWT_SECRET=$(openssl rand -base64 32) \
  --from-literal=POSTGRES_USER=pcp --from-literal=POSTGRES_PASSWORD=$(openssl rand -hex 16) \
  --from-literal=DB_USER=pcp --from-literal=DB_PASSWORD=$(openssl rand -hex 16) \
  --from-literal=STRIPE_SECRET_KEY=sk_live_...
kubectl apply -f config.yaml -f infrastructure/ -f services/ -f ingress.yaml
```

Scaling profile: auction-service 3–10 pods (HPA on 70% CPU, session-affinity
cookies on the ingress keep WebSocket sessions sticky), api-gateway 2–5,
notification-service pinned at 1 (Kafka consumer, not latency-sensitive).
