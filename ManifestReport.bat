@echo off
rem set code="C:\Users\Andrew\Documents\Work\VERS-2015\VPA"
rem set bin="C:\Program Files\Java\jdk1.8.0_162\bin"
set code="J:\PROV\TECHNOLOGY MANAGEMENT\Application Development\VERS\VERS-1999\V2ManifestGenerator"
set bin="C:\Program Files\Java\jdk1.8.0_144\bin"
set versclasspath=%code%/dist/*
rem set versclasspath="G:\PROV\TECHNOLOGY MANAGEMENT\Application Development\VERS"
rem set xalanDir="J:\PROV\TECHNOLOGY MANAGEMENT\Application Development\VERS\xalan-j_2_7_1"
rem java -Xmx200m -classpath %xalanDir%\xalan.jar org.apache.xalan.xslt.Process -in %1 -xsl %versclasspath%\VEOToolkitV2\ManifestGenerator\manifestrpt.xsl -out %2
set xalanDir=%code%\src\libs\xalan-j_2_7_1
java -Xmx200m -classpath %xalanDir%\xalan.jar org.apache.xalan.xslt.Process -in %1 -xsl %code%\src\v2manifestgenerator\manifestrpt.xsl -out %2