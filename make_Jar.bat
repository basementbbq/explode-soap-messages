@SET JAVA_HOME=c:\Program Files (x86)\Java\jdk1.6.0_29
@echo off
set RESULT_JAR_FILENAME=%CD%\ac_explodesoapmessages_app.jar
set JAVA_START_CLASS=ch.abacus.abaconnecttools.ExplodeTcpMonMessages
set SOURCE_FILES_DIR=src

if NOT EXIST "%JAVA_HOME%\bin\javac.exe" goto javapath_error

if EXIST %RESULT_JAR_FILENAME% del /Q %RESULT_JAR_FILENAME%

set CLASSPATH=.\

echo. 
echo Using JAVA Path : %JAVA_HOME%
echo. 
echo Compiling Java files...
setlocal EnableDelayedExpansion 
set OUTPUT_PATH=classes
if EXIST %OUTPUT_PATH% rmdir /S /Q %OUTPUT_PATH%
if NOT EXIST %OUTPUT_PATH% md %OUTPUT_PATH% 

REM for testing purposes 
set /a STRING_LENGTH=0
if "%CD%"=="" goto end_string_length
  set STRING_VARIABLE=%CD%
  set TEMP_FILE=temp_string.txt
  rem write string to temp text file 
  rem put redirection symbol right after 
  rem variable to avoid a trailing space 
  echo %STRING_VARIABLE%> %TEMP_FILE%

  rem get the file size in bytes 
  for %%a in (%TEMP_FILE%) do set /a STRING_LENGTH=%%~za 

  rem do some batch arithmetic 
  rem subtract 2 bytes, 1 for CR 1 for LF 
  set /a STRING_LENGTH -=2 

  rem clean up temp file 
  if EXIST %TEMP_FILE% del /Q %TEMP_FILE%
:end_string_length
rem call do_string_length.bat %CD%
rem @echo string "%CD%" has %STRING_LENGTH% characters 
set /a START_PATH_LENGTH=%STRING_LENGTH% + 1

@echo.
@echo Compiling all Java Packages containing AbaConnect Generated Client Source
set ALL_JAVA_PATHS=
For /R "%SOURCE_FILES_DIR%" %%i in (.) do (
    set DIRECTORY_PATH=%%i
    rem Remove the last 2 characters from directory path
    set JAVA_SRC_PATH=!DIRECTORY_PATH:~0,-2!
    set FULL_JAVA_SRC_PATH=!DIRECTORY_PATH:~0,-2!
    set SHORT_JAVA_SRC_PATH=!FULL_JAVA_SRC_PATH:~%START_PATH_LENGTH%!
    rem echo !SHORT_JAVA_SRC_PATH!
    if EXIST !SHORT_JAVA_SRC_PATH!\*.java set ALL_JAVA_PATHS=!ALL_JAVA_PATHS! !SHORT_JAVA_SRC_PATH!\*.java
)
@"%JAVA_HOME%\bin\javac" -J-Xmx1024m -d %OUTPUT_PATH% %ALL_JAVA_PATHS%

rem Copy any XSL files to output path
if EXIST %SOURCE_FILES_DIR%\ch\abacus\junit\abaconnect\tools\*.xsl xcopy /Y %SOURCE_FILES_DIR%\ch\abacus\junit\abaconnect\tools\*.xsl %OUTPUT_PATH%\ch\abacus\junit\abaconnect\tools\*.*

rem Create manifest.txt file with the Main class name
echo Main-Class: %JAVA_START_CLASS%>manifest.txt

cd %OUTPUT_PATH%
@"%JAVA_HOME%\bin\jar" cmf ..\manifest.txt %RESULT_JAR_FILENAME% *
cd ..

if EXIST %OUTPUT_PATH% rmdir /S /Q %OUTPUT_PATH%

rem Remove the manifest.txt file which was temporarily created with the Main class name
if EXIST manifest.txt del /Q manifest.txt

echo.
if EXIST "%RESULT_JAR_FILENAME%" echo Jar file "%RESULT_JAR_FILENAME%" was built successfully.
if NOT EXIST "%RESULT_JAR_FILENAME%" echo Jar file was NOT created.

goto end 

:javapath_error
echo.
echo INCORRECT JAVA PATH ERROR : The batch file JAVA_PATH is incorrect.
echo Please edit the batch file and set the correct JAVA_PATH variable.
echo.
echo    Looking for file : "%JAVA_HOME%\bin\javac.exe"
echo.

:end
echo.
@if "%DO_PAUSE%"=="" pause
