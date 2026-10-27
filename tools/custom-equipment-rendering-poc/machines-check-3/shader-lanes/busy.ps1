# Prints how many Gradle test executors or Gradle launches run outside this worktree (compile worker daemons do not count).
$me = 'custom-equipment-rendering-805e7d'
@(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { ($_.CommandLine -like '*Gradle Test Executor*' -or $_.CommandLine -like '*GradleWrapperMain*') -and $_.CommandLine -notlike "*$me*" }).Count
