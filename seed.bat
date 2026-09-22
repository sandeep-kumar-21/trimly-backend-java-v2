@echo off
echo ========================================================
echo       Starting Trimly Native Java Database Seeder       
echo ========================================================
call .\mvnw.cmd spring-boot:run "-Dspring-boot.run.main-class=com.trimly.api.seeder.DatabaseSeederApplication"
