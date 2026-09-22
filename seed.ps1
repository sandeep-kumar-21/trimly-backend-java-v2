Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       Starting Trimly Native Java Database Seeder       " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.main-class=com.trimly.api.seeder.DatabaseSeederApplication"
