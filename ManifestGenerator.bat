@echo off
rem set code="C:\Users\Andrew\Documents\Work\VERS-2015\VPA"
rem set bin="C:\Program Files\Java\jdk1.8.0_162\bin"
set code="J:\PROV\TECHNOLOGY MANAGEMENT\Application Development\VERS\VERS-1999\V2ManifestGenerator"
set bin="C:\Program Files\Java\jdk1.8.0_144\bin"
set versclasspath=%code%/dist/*
rem java -classpath %versclasspath% v2ManifestGenerator.V2ManifestGenerator -proxy cse14.cse:8080 %*
java -classpath %versclasspath% v2manifestgenerator.V2ManifestGenerator %*