#!/usr/bin/env sh
set -eu

image_name=${1:-user-profile-service:verify}
network_name="profile-verify-$$"
database_name="profile-verify-db-$$"
service_name="profile-verify-service-$$"
database_password="container-test-profile-database-password"

cleanup() {
    docker rm --force "$service_name" >/dev/null 2>&1 || true
    docker rm --force "$database_name" >/dev/null 2>&1 || true
    docker network rm "$network_name" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

mvn -B clean verify
test -f target/user-profile-service-1.0.0.jar
docker build --tag "$image_name" .

test "$(docker image inspect --format '{{.Config.User}}' "$image_name")" = "10001:10001"
test "$(docker image inspect --format '{{json .Config.Healthcheck.Test}}' "$image_name")" != "null"

docker network create "$network_name" >/dev/null
docker run --detach --name "$database_name" --network "$network_name" \
    --env POSTGRES_DB=user_profile \
    --env POSTGRES_USER=user_profile \
    --env POSTGRES_PASSWORD="$database_password" \
    postgres:17-alpine >/dev/null

attempt=0
while [ "$attempt" -lt 45 ]; do
    if docker exec "$database_name" pg_isready --username user_profile --dbname user_profile >/dev/null 2>&1; then
        break
    fi
    attempt=$((attempt + 1))
    sleep 1
done
docker exec "$database_name" pg_isready --username user_profile --dbname user_profile >/dev/null

docker run --detach --name "$service_name" --network "$network_name" \
    --read-only --tmpfs /tmp:rw,noexec,nosuid,size=16m \
    --env SPRING_PROFILES_ACTIVE=production \
    --env PROFILE_DB_URL="jdbc:postgresql://${database_name}:5432/user_profile" \
    --env PROFILE_DB_USERNAME=user_profile \
    --env PROFILE_DB_PASSWORD="$database_password" \
    "$image_name" >/dev/null

attempt=0
while [ "$attempt" -lt 60 ]; do
    state=$(docker inspect --format '{{.State.Status}} {{if .State.Health}}{{.State.Health.Status}}{{else}}missing{{end}}' "$service_name")
    if [ "$state" = "running healthy" ]; then
        break
    fi
    if [ "${state%% *}" != "running" ]; then
        docker logs "$service_name"
        exit 1
    fi
    attempt=$((attempt + 1))
    sleep 1
done

test "$(docker inspect --format '{{.State.Health.Status}}' "$service_name")" = "healthy"
docker exec "$service_name" sh -c 'test "$(id -u)" = 10001 && test "$(id -g)" = 10001'

docker stop --time 10 "$database_name" >/dev/null
database_unready=false
attempt=0
while [ "$attempt" -lt 30 ]; do
    if ! docker exec "$service_name" /usr/local/bin/container-healthcheck >/dev/null 2>&1; then
        database_unready=true
        break
    fi
    attempt=$((attempt + 1))
    sleep 1
done
test "$database_unready" = "true"

docker stop --time 25 "$service_name" >/dev/null
test "$(docker inspect --format '{{.State.ExitCode}}' "$service_name")" = "143"
