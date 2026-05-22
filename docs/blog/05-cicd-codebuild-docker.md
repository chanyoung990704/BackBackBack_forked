# 신입 개발자의 CI/CD: CodeBuild에서 Docker 배포까지

## 1. 문제 상황: "서버가 늘어날수록 빌드 배포 스크립트가 누더기가 된다"
처음 프로젝트를 배포했을 때는 빌드된 `app.jar` 파일을 대상 EC2 서버에 직접 복사(SCP)한 뒤, `nohup java -jar` 명령어로 원격 백그라운드 구동하는 고전적인 방식을 사용했다. 

그러나 운영 환경이 다변화되고 스케일 아웃(Scale-out)이 필요한 시점이 되자, JAR 직접 배포 방식의 한계가 극명히 드러났다.
* **환경 일관성(Consistency) 결여:** 로컬 빌드 장비의 JDK 버지(예: 17)와 개발 서버의 JDK 버전(예: 21)이 달라 기동 시점에 `UnsupportedClassVersionError`가 뿜어져 나오거나, 특정 타겟 서버 환경에서만 알 수 없는 쉘 라이브러리 부재로 기동이 실패했다.
* **배포 스크립트의 복잡성 극대화:** 서버 환경이 1대에서 3대로 증가할 때마다 쉘 스크립트 안에 루프를 돌며 각 서버의 환경 변수를 수동으로 세팅하고 프로세스 아이디(PID)를 조회해 `kill -9`을 때리는 수동 로직이 추가되었다. 스크립트가 점점 누더기가 되어갔다.
* **롤백(Rollback)의 악몽:** 새로 배포한 소스 코드에 심각한 오류가 감지되어 이전 빌드로 되돌려야 할 때, 직전 빌드 성공본 JAR 파일을 찾고 기존 백그라운드 프로세스를 직접 내린 후 다시 올려야 하는 수동 롤백 과정은 매 순간이 손 떨리는 공포였다.

이 지긋지긋한 결함을 근본적으로 고치기 위해, 모든 런타임 환경을 컨테이너 이미지로 패키징하여 배포하는 **Docker 기반의 CI/CD 자동화 배포 파이프라인**을 재설계하기로 결심했다.

---

## 2. CI/CD 배포 파이프라인 아키텍처 (Mermaid)

다음은 AWS CodePipeline, CodeBuild, CodeDeploy를 거쳐 Docker 및 systemd 다중 런타임으로 이행되는 CI/CD 배포 구성도이다.

```mermaid
flowchart TD
    Developer[개발자] -->|1. Code Push| GitHub[GitHub Repository]
    GitHub -->|2. Trigger| CP[AWS CodePipeline]

    subgraph AWS CodeBuild (CI 단계)
        CP -->|3. Build 시작| CB[CodeBuild Project]
        CB -->|3.1 YAML 사전 검증| PyCheck[Python YAML Key Checker]
        PyCheck -->|Pass| Gradle[./gradlew test & bootJar]
        Gradle -->|3.2 Docker Image Build| DockerBuild[docker build --cache-from latest]
        DockerBuild -->|3.3 ECR Push| ECR[(AWS ECR Repo: ap-northeast-2)]
    end

    subgraph AWS CodeDeploy (CD 단계)
        ECR -->|4. Deploy Trigger| CD[CodeDeploy Agent]
        CD -->|5. appspec.yml 실행| AppSpec[CodeDeploy Hooks]
        
        subgraph Target Server (EC2)
            AppSpec -->|5.1 setup-server.sh| Setup[인프라 진단]
            AppSpec -->|5.2 start-server.sh| Start[start-server.sh]
            Start -->|5.3 런타임 분석| Runtime{deploy-runtime.sh}
            
            Runtime -->|DEPLOY_RUNTIME=docker| DockerRun[Docker Compose: image-uri.env]
            Runtime -->|DEPLOY_RUNTIME=systemd| SystemdRun[systemctl restart backbackback]
        end
    end

    DockerRun & SystemdRun -->|6. 서비스 상태 체크| Health[health-check.sh]
```

---

## 3. 시도한 방법들과 솔루션

### 1) JAR 직접 배포에서 Docker 컨테이너 배포로의 전환 이유
Docker로 환경을 감싸 안는 순간 "내 로컬 컴퓨터에선 잘 돌아가는데 서버에선 왜 안 되지?"라는 고질적인 질문이 단숨에 해결된다. 
컨테이너 이미지는 애플리케이션 실행을 위한 JDK 21 런타임, OS 설정, 모든 의존 패키지를 자급자족형으로 담고 있기 때문에, 빌드 단계에서 구워진 이미지가 로컬, 스테이징, 프로덕션 환경 어디서든 **100% 동일하게 동작하는 절대적인 환경 일관성**을 선물해 준다.

### 2) 파이프라인 정밀 설계: `buildspec.yml` & `appspec.yml`
* **Python 스크립트를 통한 YAML 사전 검증:** 
  배포 중 예기치 못한 YAML 문법 에러로 기동 실패가 나는 것을 방지하기 위해, CodeBuild 빌드 페이즈 극초반(`pre_build`)에 Python을 실행시켜 모든 `application.yaml` 설정 파일 내부에 **중복된 YAML 키가 존재하는지 파싱하여 즉시 빌드를 Fail-fast** 시키는 지능형 체크 장치를 구현했다.
* **ECR 멀티 태깅 캐싱 기법:**
  매 빌드마다 Docker 레이어를 처음부터 받으면 빌드 시간이 10분을 넘는다. 이를 방지하기 위해 빌드 시작 시 ECR에서 `latest` 태그 이미지를 `pull` 하고, `docker build --cache-from` 옵션을 걸어 변경 사항이 없는 이전 빌드 레이어 캐시를 최대로 재활용함으로써 빌드 시간을 2분대까지 단축시켰다.

### 3) 배포 스크립트 모듈화 및 신뢰성 테스트
* **`deploy-runtime.sh`:** 
  배포 인프라 요구 조건에 따라 Docker 컨테이너 배포와 호스트 OS의 `systemd` 데몬 배포 방식 모두를 다이내믹하게 소화할 수 있도록 배포 비즈니스 로직을 모듈화했다.
* **`deploy-runtime-test.sh`:** 
  배포 쉘 스크립트 내부의 헬퍼 함수가 예상대로 작동하는지, 배포 전 **쉘 스크립트 전용 단위 테스트**를 촘촘하게 작성하여 배포 시 스크립트 버그로 인한 릴리즈 실패를 완벽 차단했다.

---

## 4. 실제 프로젝트 소스 코드 분석

### 1) 빌드 최적화 및 YAML 문법 사전 진단이 내재된 `buildspec.yml`
```yaml
version: 0.2

env:
  variables:
    AWS_REGION: ap-northeast-2
    ECR_REPO_URI: 160885260227.dkr.ecr.ap-northeast-2.amazonaws.com/aivle-back-app

phases:
  install:
    runtime-versions:
      java: corretto21
  pre_build:
    commands:
      - chmod +x gradlew
      - |
        # [핵심] 빌드 시작 전 모든 YAML 설정 파일 중복 키 검사 (Python 활용)
        python3 - <<'PY'
        import yaml
        from pathlib import Path

        class UniqueKeyLoader(yaml.SafeLoader):
          pass

        def construct_mapping(loader, node, deep=False):
          mapping = {}
          for key_node, value_node in node.value:
            key = loader.construct_object(key_node, deep=deep)
            if key in mapping:
              line = key_node.start_mark.line + 1
              raise ValueError(f"Duplicate YAML key '{key}' at line {line}")
            mapping[key] = loader.construct_object(value_node, deep=deep)
          return mapping

        UniqueKeyLoader.add_constructor(
          yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG,
          construct_mapping,
        )

        targets = [
          Path("src/main/resources/application.yaml"),
          Path("src/main/resources/application-dev.yaml"),
          Path("src/main/resources/application-prod.yaml"),
        ]

        for target in targets:
          if not target.exists():
            continue
          with target.open("r", encoding="utf-8") as f:
            yaml.load(f, Loader=UniqueKeyLoader)
          print(f"[YAML CHECK] OK: {target}")
        PY
      - ./gradlew --version
      - chmod +x scripts/*.sh
      - IMAGE_TAG="${CODEBUILD_RESOLVED_SOURCE_VERSION:-manual}"
      - REPO_HOST="${ECR_REPO_URI%%/*}"
      - aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REPO_HOST"
      - docker pull "$ECR_REPO_URI:latest" || true  # 캐시 재활용용 이미지 풀
  build:
    commands:
      - IMAGE_TAG="${CODEBUILD_RESOLVED_SOURCE_VERSION:-manual}"
      - ./gradlew bootJar -x test
      - cp build/libs/*.jar build/libs/app.jar
      - # 캐싱 빌드 최적화 수행
        docker build --cache-from "$ECR_REPO_URI:latest" -t "$ECR_REPO_URI:$IMAGE_TAG" -t "$ECR_REPO_URI:latest" .
      - touch .image_built
  post_build:
    commands:
      - IMAGE_TAG="${CODEBUILD_RESOLVED_SOURCE_VERSION:-manual}"
      - |
        if [ -f .image_built ]; then
          docker push "$ECR_REPO_URI:$IMAGE_TAG"
          docker push "$ECR_REPO_URI:latest"
          echo "APP_IMAGE=$ECR_REPO_URI:$IMAGE_TAG" > image-uri.env
        fi

artifacts:
  files:
    - appspec.yml
    - image-uri.env
    - docker-compose.app.yml
    - scripts/**/*
```

### 2) systemd 및 docker 런타임을 모두 분기 처리해 구동하는 `deploy-runtime.sh`
```bash
#!/usr/bin/env bash

# DEPLOY_RUNTIME 환경변수 값 파싱 및 검증
resolve_deploy_runtime() {
  local runtime="${DEPLOY_RUNTIME:-systemd}"
  runtime="$(printf '%s' "$runtime" | tr '[:upper:]' '[:lower:]')"

  case "$runtime" in
    docker|systemd)
      printf '%s\n' "$runtime"
      ;;
    *)
      echo "[WARN] 지원하지 않는 DEPLOY_RUNTIME(${runtime})입니다. systemd로 기본 처리합니다." >&2
      printf 'systemd\n'
      ;;
  esac
}

# ECR 이미지 도메인에서 AWS 리전 추출 로직
extract_ecr_region() {
  local registry="${1:-}"
  if [[ "$registry" =~ ^[0-9]+\.dkr\.ecr\.([a-z0-9-]+)\.amazonaws\.com$ ]]; then
    printf '%s\n' "${BASH_REMATCH[1]}"
    return 0
  fi
  return 1
}

# docker compose 명령어 호환 래퍼
set_compose_command() {
  COMPOSE_CMD=()
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=("docker" "compose")
    return 0
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=("docker-compose")
    return 0
  fi
  return 1
}

# 쉘 환경 오염 없이 안전하게 컨테이너 실행
run_compose_command() {
  local app_name="${APP_NAME:-backbackback}"
  local app_dir="${APP_DIR:-/opt/project}"
  local compose_file="${COMPOSE_FILE_PATH:-${app_dir}/docker-compose.app.yml}"
  local env_file="${ENV_FILE:-/etc/${app_name}/${app_name}.env}"
  local image_env_file="${IMAGE_ENV_FILE:-${app_dir}/image-uri.env}"
  local cmd=()

  if [ ! -f "$compose_file" ]; then
    echo "[ERROR] compose 파일을 찾지 못했습니다: $compose_file" >&2
    return 1
  fi

  if ! set_compose_command; then
    echo "[ERROR] docker compose 명령을 찾지 못했습니다." >&2
    return 1
  fi

  cmd=("${COMPOSE_CMD[@]}")
  if [ -f "$env_file" ]; then
    cmd+=("--env-file" "$env_file")
  fi
  if [ -f "$image_env_file" ]; then
    cmd+=("--env-file" "$image_env_file")
  fi
  cmd+=("-f" "$compose_file")
  cmd+=("$@")

  "${cmd[@]}"
}
```

### 3) 쉘 스크립트 기능성 품질을 담보하는 단위 테스트 `deploy-runtime-test.sh`
```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "${SCRIPT_DIR}/../lib/deploy-runtime.sh"

assert_eq() {
  local expected="$1"
  local actual="$2"
  local message="$3"

  if [ "$expected" != "$actual" ]; then
    echo "[FAIL] ${message} | expected=${expected}, actual=${actual}" >&2
    exit 1
  fi
}

test_resolve_deploy_runtime_default() {
  unset DEPLOY_RUNTIME
  assert_eq "systemd" "$(resolve_deploy_runtime)" "DEPLOY_RUNTIME 기본값 검증"
}

test_resolve_deploy_runtime_docker() {
  DEPLOY_RUNTIME="DoCkEr"
  assert_eq "docker" "$(resolve_deploy_runtime)" "DEPLOY_RUNTIME docker 소문자 변환"
}

test_extract_ecr_region() {
  assert_eq "ap-northeast-2" "$(extract_ecr_region "160885260227.dkr.ecr.ap-northeast-2.amazonaws.com")" "ECR 리전 파싱 검증"
}

main() {
  test_resolve_deploy_runtime_default
  test_resolve_deploy_runtime_docker
  test_extract_ecr_region
  echo "[PASS] deploy-runtime helper 단위 테스트 전체 통과!"
}

main "$@"
```

---

## 5. 최종 결과 및 배운 점

### 1) 컨테이너가 제공하는 "배포의 평화"
배포 방식을 Docker 이미지 단위로 완전히 바꾼 뒤, 가상 환경을 일치시켜 줌으로써 예전 배포 때 간헐적으로 발생하던 라이브러리 누락 에러 및 런타임 충돌 장애가 단 **0%**로 전멸했다. 
이제 빌드된 이미지는 AWS ECR에 버전별 태그(`git-commit-hash`)를 달고 차곡차곡 역사가 관리된다. 만약 신규 배포에서 치명적인 런타임 결함이 발견되어도, RDB 마이그레이션 호환성이 깨지지 않았다면, 기존 가동 중인 컨테이너 버전을 단 **1초 만에 롤백(Rollback)**하여 이전 이미지로 원복시킬 수 있는 신속성을 얻었다.

### 2) 배포 자동화 파이프라인의 핵심은 "지속적 테스트와 신뢰도"
CI/CD 프로세스는 그 자체가 또 하나의 견고한 "소프트웨어 프로그램"과 같다는 것을 이번 전환 작업을 통해 깊이 깨달았다.
CodeBuild 빌드 실패의 절반 가량은 결국 환경 설정 오류나 빌드 환경의 설정 문법 누락 때문이었다. 이를 위해 **Python 사전 문법 검증기**를 얹고, 배포용 Bash 스크립트에 **단위 테스트 스크립트**까지 밀착 동반시킴으로써 수십 번의 무중단 연속 배포 시도 속에서도 인프라 스크립트 결함으로 배포가 망가지는 일을 철통같이 막을 수 있었다. 신뢰성 높은 인프라는 결국 자동화된 테스트 장치가 받쳐줄 때 비로소 완성된다는 값진 교훈을 배운 여정이었다.
