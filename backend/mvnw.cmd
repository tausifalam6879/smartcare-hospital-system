@ECHO OFF
SETLOCAL
SET "WRAPPER_DIR=%~dp0"
java -classpath "%WRAPPER_DIR%.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%WRAPPER_DIR%." org.apache.maven.wrapper.MavenWrapperMain %*
IF ERRORLEVEL 1 EXIT /B %ERRORLEVEL%
ENDLOCAL
