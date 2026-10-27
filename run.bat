@echo off
setlocal
cd /d "%~dp0"

rem Prefer the JDK configured in JAVA_HOME so compiler and runtime match.
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
  set "JAVAC=%JAVA_HOME%\bin\javac.exe"
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVAC=javac"
  set "JAVA=java"
)

"%JAVAC%" -version >nul 2>nul
if errorlevel 1 (
  echo ERROR: javac was not found.
  echo Install a JDK and set JAVA_HOME to its folder, or add its bin folder to PATH.
  pause
  exit /b 1
)

if not exist out mkdir out
echo Compiling ElectricBillWebApp.java...
"%JAVAC%" -d out src\ElectricBillWebApp.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Please read the error above.
  pause
  exit /b 1
)

echo.
echo Starting Electricity Bill Web App...
echo Open http://localhost:8080 in your browser.
echo Keep this window open while using the app.
echo Press Ctrl+C to stop the server.
echo.
"%JAVA%" -cp out ElectricBillWebApp
pause
