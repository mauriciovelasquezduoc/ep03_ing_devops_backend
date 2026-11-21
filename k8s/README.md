# Kubernetes - Proyecto backend

Configuración autónoma para desplegar el backend (Spring Boot) en Kubernetes. Este proyecto se instala de forma independiente, sin depender de un repo centralizado.

## Estructura

```
k8s/
├── values.yaml                  # Configuración del proyecto
├── templates/
│   ├── namespace.yaml           # Namespace compartido (idempotente)
│   ├── backend-deployment.yaml  # Spring Boot con health checks + env de BD
│   ├── backend-service.yaml     # ClusterIP ${NAMESPACE}-backend:8080
│   └── backend-hpa.yaml         # Autoscaling por CPU
├── output/                      # YAMLs generados (gitignore)
├── generate.sh                  # Solo genera los YAMLs
└── apply.sh                     # Genera y aplica
```

## Instalación

```bash
cd k8s
./apply.sh
```

Opcionalmente, si el cluster es EKS y quieres actualizar el kubeconfig desde este repo:

```bash
CLUSTER_NAME=laboratorio-ep03-eks ./apply.sh
```

Con tag versionado (por ejemplo, como lo hace el workflow de GitHub Actions):

```bash
BACKEND_IMAGE=<cuenta>.dkr.ecr.us-east-1.amazonaws.com/ep03-backend:v1.2.0 ./apply.sh
```

Las variables definidas en el entorno tienen precedencia sobre `values.yaml`.

## Previsualizar sin aplicar

```bash
./generate.sh
```

## Configuración

Edita `values.yaml`:

```bash
NAMESPACE=ep03                                  # debe coincidir con database y frontend
BACKEND_IMAGE=<AWSAccountId>.dkr.ecr.us-east-1.amazonaws.com/ep03-backend:latest
BACKEND_PORT=8080
BACKEND_REPLICAS=1
BACKEND_HPA_MIN=1
BACKEND_HPA_MAX=10
BACKEND_HPA_CPU=70

# Conexion a la BD (debe coincidir con el values.yaml del proyecto database)
DATABASE_NAME=ep03
DATABASE_USER=ep03_user
DATABASE_PORT=5432
```

La cuenta AWS de la imagen no se hardcodea: `apply.sh` la resuelve en runtime con `aws sts get-caller-identity`.

## Prerrequisito: aplicar database primero

El Deployment del backend lee el Secret `database-secret` (password de PostgreSQL) y se conecta al Service `${NAMESPACE}-database`. `apply.sh` valida que ambos existan y falla con un mensaje claro si no:

```bash
cd ../ep03_ing_devops_database/k8s && ./apply.sh   # 1. database
cd ../ep03_ing_devops_backend/k8s && ./apply.sh    # 2. backend (este)
```

## Acoplamientos con otros proyectos

| Valor | Origen | Uso |
|---|---|---|
| `NAMESPACE` | database / frontend | namespace compartido |
| Service `${NAMESPACE}-database` | proyecto database | `DB_URL` del backend |
| Secret `database-secret` | proyecto database | `DB_PASSWORD` del backend |
| `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PORT` | values.yaml database | deben coincidir |

## Verificación

```bash
kubectl get all -n ep03
kubectl get hpa -n ep03
kubectl logs -f <pod-backend> -n ep03
```

## Integración con GitHub Actions (deploy.yml)

El workflow `.github/workflows/deploy.yml` de este repo ejecuta el mismo flujo de forma automática:

1. Trigger: `workflow_run` cuando "Calidad de código" termina OK.
2. **versioning**: calcula `v1.x.0` y crea el tag git.
3. **build-push-ecr**: construye y publica la imagen en ECR con ese tag.
4. **deploy**: corre `k8s/apply.sh` con `BACKEND_IMAGE=<registry>/ep03-backend:<tag>`, que valida prerequisitos, genera los manifiestos desde `k8s/templates/` y los aplica; luego espera `kubectl rollout status`.

Secrets requeridos: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`, `AWS_REGION`.
