#!/usr/bin/env bash
set -e

echo "========================================================"
echo "       Starting Trimly Native Java Database Seeder       "
echo "========================================================"

./mvnw spring-boot:run "-Dspring-boot.run.main-class=com.trimly.api.seeder.DatabaseSeederApplication"
