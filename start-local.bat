@echo off
echo Starting TMMSystem Backend (Local Profile)...
echo.

REM Set Spring profile to local
set SPRING_PROFILES_ACTIVE=local

REM Run Maven
call mvnw.cmd spring-boot:run

pause
