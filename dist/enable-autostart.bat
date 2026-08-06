@echo off
REM Run this once. It makes Tuck Shop POS start automatically whenever this
REM computer is turned on or restarts after a power cut - no one needs to
REM remember to double-click start-pos.bat again.
set SCRIPT_DIR=%~dp0
set STARTUP_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup
powershell -NoProfile -Command "$s=(New-Object -COM WScript.Shell).CreateShortcut('%STARTUP_DIR%\TuckShopPOS.lnk'); $s.TargetPath='%SCRIPT_DIR%start-pos.bat'; $s.WorkingDirectory='%SCRIPT_DIR%'; $s.WindowStyle=7; $s.Save()"
echo Done. Tuck Shop POS will now start automatically on every reboot.
pause
