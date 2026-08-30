$ErrorActionPreference = 'Stop'
$wrapperDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$wrapperJar = Join-Path $wrapperDir '.mvn\wrapper\maven-wrapper.jar'
& java -classpath $wrapperJar "-Dmaven.multiModuleProjectDirectory=$wrapperDir" org.apache.maven.wrapper.MavenWrapperMain @args
exit $LASTEXITCODE
