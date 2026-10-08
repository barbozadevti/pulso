@echo off
chcp 65001 >nul
title Liberar Pulso no celular

rem Cria uma regra no Firewall do Windows para o celular (mesmo Wi-Fi) alcancar a porta 5310.
rem Precisa de administrador: o Windows vai pedir sua permissao.
net session >nul 2>&1
if errorlevel 1 (
  powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

netsh advfirewall firewall delete rule name="Pulso (celular)" >nul 2>&1
netsh advfirewall firewall add rule name="Pulso (celular)" dir=in action=allow protocol=TCP localport=5310 profile=private
if errorlevel 1 (
  echo.
  echo   Nao consegui criar a regra.
) else (
  echo.
  echo   Pronto! Agora o celular, no mesmo Wi-Fi, consegue abrir o Pulso.
  echo   A regra vale so para redes "Particulares" ^(a rede de casa^).
)
echo.
pause
