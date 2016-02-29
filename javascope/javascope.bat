@ECHO OFF
ECHO preparing
if defined JDK_HOME GOTO:start
rem This script located the current version of
rem "Java Development Kit" and sets the
rem %JDK_HOME% environment variable
setlocal ENABLEEXTENSIONS
set KEY=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit
FOR /F "usebackq tokens=2,* skip=2" %%L IN (`reg query "%KEY%" /v CurrentVersion`) DO SET JDKVER=%%M
FOR /F "usebackq tokens=2,* skip=2" %%L IN (`reg query "%KEY%\%JDKVER%" /v JavaHome`) DO SET JDK_HOME="%%M"
SET JDK_HOME=%JDK_HOME:"=%
IF EXIST "%JDK_HOME%" GOTO:start
ECHO JDK not found. Please set %%JDK_HOME%% to the root path of your jdk.
SET /A ERROR=1
GOTO:end

:start
SET DOCS=jScope.html jScope.jpg popup.jpg about_jscope.jpg ^
CompositeWaveDisplay.html ^
ConnectionEvent.html ConnectionListener.html ^
DataProvider.html ^
FrameData.html ^
ReadMe.html data_popup.jpg data_setup.jpg frame_popup.jpg image_setup.jpg ^
twu/TWU_image_message.html ^
UpdateEvent.html UpdateEventListener.html ^
WaveData.html WaveDisplay.html

SET TOPDOCS=jScope.html ^
CompositeWaveDisplay.html ^
ConnectionEvent.html ConnectionListener.html ^
DataProvider.html ^
FrameData.html ^
ReadMe.html ^
twu/TWU_image_message.html ^
UpdateEvent.html UpdateEventListener.html ^
WaveData.html WaveDisplay.html

SET SUBDOCS=jScope.html jScope.jpg popup.jpg ^
CompositeWaveDisplay.html ^
ConnectionEvent.html ConnectionListener.html ^
DataProvider.html ^
FrameData.html ^
data_setup.jpg frame_popup.jpg image_setup.jpg ^
UpdateEvent.html UpdateEventListener.html ^
WaveData.html

SET JET_SRC=^
  jet\Ji\JiDataSource.java ^
  jet\Ji\JiDim.java ^
  jet\Ji\JiNcSource.java ^
  jet\Ji\JiNcVarByte.java ^
  jet\Ji\JiNcVarChar.java ^
  jet\Ji\JiNcVarDouble.java ^
  jet\Ji\JiNcVarFloat.java ^
  jet\Ji\JiNcVarImp.java ^
  jet\Ji\JiNcVarInt.java ^
  jet\Ji\JiNcVar.java ^
  jet\Ji\JiNcVarShort.java ^
  jet\Ji\JiSlabIterator.java ^
  jet\Ji\JiSlab.java ^
  jet\Ji\JiVarImpl.java ^
  jet\Ji\JiVar.java ^
  jet\jetDataProvider.java ^
  jet\jetMdsDataProvider.java

SET LOCAL_SRC=^
  local\localDataProvider.java ^
  local\localDataProviderInfo.java

SET MDS_SRC=^
  mds\mdsAccess.java ^
  mds\mdsBrowseSignals.java ^
  mds\mdsConnection.java ^
  mds\mdsConnectionUdt.java ^
  mds\mdsDataClient.java ^
  mds\mdsDataProvider.java ^
  mds\mdsDataProviderUdt.java ^
  mds\mdsIOException.java ^
  mds\mdsMessage.java ^
  mds\mdsParser.java ^
  mds\mdsWaveInterface.java

SET MISC_SRC=^
  misc\asciiDataProvider.java ^
  misc\asdexDataProvider.java ^
  misc\ftuDataProvider.java ^
  misc\tsDataProvider.java ^
  misc\universalDataProvider.java

SET TWU_SRC=^
  twu\textorBrowseSignals.java ^
  twu\FakeTWUProperties.java ^
  twu\TWUFetchOptions.java ^
  twu\TWUProperties.java ^
  twu\TWUSignal.java ^
  twu\twuAccess.java ^
  twu\twuDataProvider.java ^
  twu\twuNameServices.java ^
  twu\twuSimpleFrameData.java ^
  twu\twuSingleSignal.java ^
  twu\twuWaveData.java

SET W7X_SRC=^
  w7x\w7xDataProvider.java ^
  w7x\w7xBrowseSignals.java


SET COMMON_SRC=^
  jScope\AboutWindow.java ^
  jScope\Base64.java ^
  jScope\ColorDialog.java ^
  jScope\ColorMapDialog.java ^
  jScope\ColorMap.java ^
  jScope\ConnectionEvent.java ^
  jScope\ConnectionListener.java ^
  jScope\ContourSignal.java ^
  jScope\DataAccess.java ^
  jScope\DataAccessURL.java ^
  jScope\DataCached.java ^
  jScope\DataCacheObject.java ^
  jScope\DataProvider.java ^
  jScope\DataServerItem.java ^
  jScope\Descriptor.java ^
  jScope\FontSelection.java ^
  jScope\FrameData.java ^
  jScope\Frames.java ^
  jScope\Grid.java ^
  jScope\ImageTransferable.java ^
  jScope\jScopeBrowseSignals.java ^
  jScope\jScopeBrowseUrl.java ^
  jScope\jScopeDefaultValues.java ^
  jScope\jScopeFacade.java ^
  jScope\jScopeMultiWave.java ^
  jScope\jScopeProperties.java ^
  jScope\jScopeWaveContainer.java ^
  jScope\jScopeWavePopup.java ^
  jScope\MdsIpProtocolWrapper.java ^
  jScope\MultiWaveform.java ^
  jScope\MultiWavePopup.java ^
  jScope\NotConnectedDataProvider.java ^
  jScope\ProfileDialog.java ^
  jScope\PropertiesEditor.java ^
  jScope\RandomAccessData.java ^
  jScope\RowColumnContainer.java ^
  jScope\SignalListener.java ^
  jScope\RowColumnLayout.java ^
  jScope\SetupDataDialog.java ^
  jScope\SetupDefaults.java ^
  jScope\SetupWaveformParams.java ^
  jScope\SignalBox.java ^
  jScope\AsynchDataSource.java ^
  jScope\Signal.java ^
  jScope\SignalsBoxDialog.java ^
  jScope\SshTunneling.java ^
  jScope\UpdateEvent.java ^
  jScope\UpdateEventListener.java ^
  jScope\WaveContainerEvent.java ^
  jScope\WaveContainerListener.java ^
  jScope\WaveData.java ^
  jScope\WaveDisplay.java ^
  jScope\WaveformContainer.java ^
  jScope\WaveformEditor.java ^
  jScope\WaveformEditorListener.java ^
  jScope\WaveformEvent.java ^
  jScope\Waveform.java ^
  jScope\WaveformListener.java ^
  jScope\WaveformManager.java ^
  jScope\WaveformMetrics.java ^
  jScope\WaveInterface.java ^
  jScope\WavePopup.java ^
  jScope\XYData.java ^
  jScope\XYWaveData.java ^
  jScope\WaveDataListener.java

SET WAVEDISPLAY_SRC=

SET JSCOPE_SRC=^
  jScope.java ^
  jScope\CompositeWaveDisplay.java ^
  %JET_SRC% ^
  %LOCAL_SRC% ^
  %MDS_SRC% ^
  %MISC_SRC% ^
  %TWU_SRC% ^
  %W7X_SRC%

SET EXTRA_CLASS=^
jScope\FakeTWUProperties.class ^
jScope\FontPanel.class ^
jScope\ServerDialog*.class ^
jScope\WindowDialog.class

SET CLASSPATH=-classpath ".;MindTerm.jar;w7x\swingx.jar;w7x\w7xDataProvider.jar"
SET JAVAC="%JDK_HOME%\bin\javac.exe"
SET JCFLAGS=-O -g:none||rem-Xlint -deprecation
SET SRCDIR=%CD%
SET JAR="%JDK_HOME%\bin\jar.exe"
SET JARDIR=..\java\classes

ECHO compiling *.java to *.class . . .
%JAVAC% %JCFLAGS% -d %JARDIR% %CLASSPATH% %COMMON_SRC% %JSCOPE_SRC% %WAVEDISPLAY_SRC%
SET /A ERROR=%ERRORLEVEL%
IF %ERROR% NEQ 0 GOTO:cleanup

:gather
ECHO gathering data
COPY /Y jScope.properties %JARDIR%\>NUL
COPY /Y colors1.tbl %JARDIR%\>NUL
MKDIR  %JARDIR%\docs 2>NUL
MKDIR  %JARDIR%\jars 2>NUL
FOR %%F IN (%DOCS%) DO COPY /Y %%F /D %JARDIR%\docs>NUL
COPY %CD%\MindTerm.jar %JARDIR%>NUL
COPY %CD%\w7x\swingx.jar %JARDIR%\jars>NUL
COPY %CD%\w7x\w7xDataProvider.jar %JARDIR%>NUL

:packjar
ECHO creating jar packages
PUSHD %JARDIR%
%JAR% -cmf %SRCDIR%\jScope\MANIFEST.mf "jScope.jar" jScope.class colors1.tbl jScope.properties jScope docs
%JAR% -cmf %SRCDIR%\jet\MANIFEST.mf "jetDataProvider.jar" jet
%JAR% -cmf %SRCDIR%\local\MANIFEST.mf "localDataProvider.jar" local
%JAR% -cmf %SRCDIR%\mds\MANIFEST.mf "mdsDataProvider.jar" mds
%JAR% -cmf %SRCDIR%\misc\MANIFEST.mf "miscDataProvider.jar" misc
%JAR% -cmf %SRCDIR%\twu\MANIFEST.mf "twuDataProvider.jar" twu
%JAR% -umf %SRCDIR%\w7x\MANIFEST.mf "w7xDataProvider.jar" w7x jars\swingx.jar
%JAR% -cf "WaveDisplay.jar" %COMMON_SRC:.java=.class%
POPD

:cleanup
ECHO cleaning up
PUSHD %JARDIR%
RMDIR /S /Q docs 2>NUL
DEL colors1.tbl jScope.properties jScope.class 2>NUL
RMDIR /S /Q jars 2>nul
RMDIR /S /Q jet 2>nul
RMDIR /S /Q jScope 2>nul
RMDIR /S /Q local 2>nul
RMDIR /S /Q mds 2>nul
RMDIR /S /Q misc 2>nul
RMDIR /S /Q twu 2>nul
RMDIR /S /Q w7x 2>nul

POPD

:jscope
IF %ERROR% NEQ 0 GOTO:end
ECHO start jScope?
PAUSE
CLS
java -jar -Xmx1G "%JARDIR%\jScope.jar"

:end
PAUSE
EXIT /B ERROR