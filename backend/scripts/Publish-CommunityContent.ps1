# 本地开发导入；从脚本位置定位仓库，不读取 IDE 或提交任何凭据。
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$taskId = [guid]::NewGuid().ToString('N')
$classPathFile = Join-Path ([IO.Path]::GetTempPath()) "arieshub-content-classpath-$taskId.txt"
$javaArgsFile = Join-Path ([IO.Path]::GetTempPath()) "arieshub-content-java-$taskId.args"
Push-Location $repoRoot
try {
    if ($env:S3_ENABLED -ne 'true' -or !$env:S3_ACCESS_KEY_ID -or !$env:S3_ACCESS_KEY_SECRET) {
        throw '请先在当前终端配置项目 OSS 环境变量；此脚本只用于本地开发数据库。'
    }
    # 复用 Maven Wrapper 和现有依赖，构建通用运行时 classpath。
    & ./backend/mvnw.cmd -q -f backend/pom.xml compile dependency:build-classpath "-Dmdep.outputFile=$classPathFile" '-Dmdep.includeScope=runtime'
    if ($LASTEXITCODE -ne 0) { throw 'Maven 编译失败，尚未发布内容' }
    $dependencyCp = (Get-Content -LiteralPath $classPathFile -Raw).Trim().Replace('\','/')
    $compiledPath = (Join-Path $repoRoot 'backend/target/classes').Replace('\','/')
    $sourcePath = (Join-Path $PSScriptRoot 'PublishCommunityContent.java').Replace('\','/')
    @('--class-path', ('"' + $compiledPath + ';' + $dependencyCp + '"'), ('"' + $sourcePath + '"'), ('"' + $repoRoot.Replace('\','/') + '"')) |
        Set-Content -LiteralPath $javaArgsFile -Encoding utf8
    & java "@$javaArgsFile"
    if ($LASTEXITCODE -ne 0) { throw '导入失败，请检查已保存的发布结果，避免重复操作' }
} finally {
    Pop-Location
    # 仅清理本次生成的两个确切临时文件。
    foreach ($temporaryFile in @($classPathFile, $javaArgsFile)) {
        if (Test-Path -LiteralPath $temporaryFile) { Remove-Item -LiteralPath $temporaryFile }
    }
}
