@echo off
chcp 65001 > nul
echo ------------------------------------------
echo 🚀 开始编译并将 VPPassword 安装到已连接设备...
echo ------------------------------------------
call gradlew.bat installDebug
if %ERRORLEVEL% equ 0 (
    echo.
    echo ✅ 编译并安装成功！你可以去手机上查看了。
) else (
    echo.
    echo ❌ 安装失败，请检查报错或设备是否已连接。
)
pause
