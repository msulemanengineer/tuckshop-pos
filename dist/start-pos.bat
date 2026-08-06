@echo off
cd /d "%~dp0"
echo Starting Tuck Shop POS...
echo Once it says "Tuck Shop POS is running", open this address in a browser:
echo   http://localhost:8080
echo.
java -jar tuckshop-pos.jar
echo.
echo The program stopped. If this was not on purpose, read the message above.
pause
