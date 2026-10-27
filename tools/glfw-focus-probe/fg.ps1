Add-Type -Namespace W -Name U -MemberDefinition @'
[DllImport("user32.dll")] public static extern System.IntPtr GetForegroundWindow();
[DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern int GetWindowText(System.IntPtr h, System.Text.StringBuilder s, int n);
'@
foreach ($i in 1..3) { Start-Sleep -Milliseconds 900; $h=[W.U]::GetForegroundWindow(); $sb=New-Object System.Text.StringBuilder 256; [void][W.U]::GetWindowText($h,$sb,256); "FOREGROUND hwnd=$([int64]$h) title=$($sb.ToString())" }
