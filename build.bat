@echo off

rem 构建WebManager插件

echo 正在构建WebManager插件...

rem 检查是否安装了Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到Maven，请确保Maven已安装并添加到系统路径中
    pause
    exit /b 1
)

rem 执行构建命令
mvn clean package

if %errorlevel% neq 0 (
    echo 构建失败，请检查错误信息
    pause
    exit /b 1
)

echo 构建成功！
echo 插件JAR文件已生成在 target/ 目录中

pause
