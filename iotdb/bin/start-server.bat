@echo off
echo ````````````````````````
echo Starting IoTDB
echo ````````````````````````


if "%OS%" == "Windows_NT" setlocal

pushd %~dp0..
if NOT DEFINED IOTDB_HOME set IOTDB_HOME=%~sdp0..
popd

set IOTDB_CONF=%IOTDB_HOME%\conf
set IOTDB_LOGS=%IOTDB_HOME%\logs

IF EXIST %IOTDB_CONF%\iotdb-env.bat (
    CALL %IOTDB_CONF%\iotdb-env.bat
    ) ELSE (
    echo "can't find %IOTDB_CONF%\iotdb-env.bat"
    )

if NOT DEFINED MAIN_CLASS set MAIN_CLASS=cn.edu.tsinghua.iotdb.service.IoTDB
if NOT DEFINED JAVA_HOME goto :err

@REM -----------------------------------------------------------------------------
@REM JVM Opts we'll use in legacy run or installation
set JAVA_OPTS=-ea^
 -Dlogback.configurationFile="%IOTDB_CONF%\logback.xml"^
 -DIOTDB_HOME=%IOTDB_HOME%^
 -DTSFILE_HOME=%IOTDB_HOME%^
 -DIOTDB_CONF=%IOTDB_CONF%

@REM ***** CLASSPATH library setting *****
@REM Ensure that any user defined CLASSPATH variables are not used on startup
set CLASSPATH="%IOTDB_HOME%\lib"

REM For each jar in the IOTDB_HOME lib directory call append to build the CLASSPATH variable.
for %%i in ("%IOTDB_HOME%\lib\*.jar") do call :append "%%i"
set CLASSPATH=%CLASSPATH%;iotdb.IoTDB
goto okClasspath

:append
set CLASSPATH=%CLASSPATH%;%1
goto :eof

REM -----------------------------------------------------------------------------
:okClasspath

rem echo CLASSPATH: %CLASSPATH%

"%JAVA_HOME%\bin\java" %JAVA_OPTS% %IOTDB_HEAP_OPTS% -cp %CLASSPATH% %IOTDB_DERBY_OPTS% %IOTDB_JMX_OPTS% %MAIN_CLASS%
goto finally

:err
echo JAVA_HOME environment variable must be set!
pause


@REM -----------------------------------------------------------------------------
:finally

pause

ENDLOCAL
