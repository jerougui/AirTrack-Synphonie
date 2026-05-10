@rem
@rem Copyright (c) 2015, the Gradle project.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

@rem Set local scope for the variables with windows THIS COMMAND
setlocal

@rem Set JAVA_HOME if not already set
if "%JAVA_HOME%"=="" (
  set "JAVA_EXE=java"
) else (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if "%JAVA_EXE%"=="" (
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  goto fail
)

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set "DEFAULT_JVM_OPTS=-Xmx64m -Xms64m"

@rem Find the project directory
set "APP_HOME=%~dp0.."
cd /d "%APP_HOME%" || exit /b 1
set "APP_HOME=%CD%"

@rem Resolve symlinks to get the real path
for /f "delims=" %%I in ("%APP_HOME%") do set "APP_HOME=%%~fI"

@rem Add the Gradle user home directory to the classpath.
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"

@rem Setup the command line
set "CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar"
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:fail
exit /b 1
