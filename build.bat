@echo off
echo [Poly Pixelator] Checking for Java...
where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Java not found. Opening web version in browser...
    start "" "%~dp0index.html"
    exit /b 0
)
echo [Poly Pixelator] Compiling...
if not exist out mkdir out
javac -d out src\PolyPixelator.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed. Opening web version instead...
    start "" "%~dp0index.html"
    pause
    exit /b 1
)
echo [Poly Pixelator] Running Java app...
java -cp out PolyPixelator
