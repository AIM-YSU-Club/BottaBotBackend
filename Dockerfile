# 1. Python 3.11 슬림 버전을 기반 이미지로 사용 (경량화 및 보안)
FROM python:3.12-slim

# 2. 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 3. 컴파일러 및 의존성 설치에 필요한 시스템 패키지 설치
# (psycopg2 등 DB 라이브러리 빌드 시 오류 방지)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 4. 종속성 설치 효율화를 위해 requirements.txt 먼저 복사
# (소스 코드가 바뀌어도 패키지 설치 캐시를 유지하기 위함)
COPY requirements.txt .

# 5. 파이썬 패키지 설치
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r requirements.txt

# 6. 현재 디렉토리의 모든 소스 코드를 컨테이너의 /app으로 복사
# (docker-compose에서 볼륨을 연결하면 개발 시에는 로컬 코드가 덮어씌워집니다)
COPY . .

# 7. 컨테이너가 외부와 통신할 포트 지정
EXPOSE 8000

# 8. 컨테이너 실행 명령 (FastAPI/Uvicorn 기준 예시)
# --reload 옵션을 주어 docker-compose 볼륨 바인딩 시 코드 변경이 실시간 반영되게 합니다.
CMD ["uvicorn", "core.server:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]