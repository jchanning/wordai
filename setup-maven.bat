@echo off
REM Add Maven to PATH for current session
set MAVEN_HOME=%USERPROFILE%\.tools\maven\apache-maven-3.9.11
set PATH=%MAVEN_HOME%\bin;%PATH%

echo Maven added to PATH for current session
echo Maven Home: %MAVEN_HOME%
echo.
echo Testing Maven installation:
mvn -version
echo.
echo To add Maven permanently to your system PATH:
echo 1. Open System Properties (Win + X, then Y)
echo 2. Click "Environment Variables"
echo 3. Under "User variables", click "New"
echo 4. Variable name: MAVEN_HOME
echo 5. Variable value: %MAVEN_HOME%
echo 6. Edit the PATH variable and add: %%MAVEN_HOME%%\bin
echo.
echo For now, run this batch file whenever you need Maven in a new terminal session.