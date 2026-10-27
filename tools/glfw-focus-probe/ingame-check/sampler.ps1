param([int]$ClientPid, [string]$Out, [string]$Stop)
Add-Type -Namespace W -Name U -MemberDefinition @'
[StructLayout(LayoutKind.Sequential)] public struct POINT { public int X; public int Y; }
[StructLayout(LayoutKind.Sequential)] public struct RECT { public int L; public int T; public int R; public int B; }
[DllImport("user32.dll")] public static extern bool GetCursorPos(out POINT p);
[DllImport("user32.dll")] public static extern System.IntPtr GetForegroundWindow();
[DllImport("user32.dll")] public static extern bool GetClientRect(System.IntPtr h, out RECT r);
[DllImport("user32.dll")] public static extern bool ClientToScreen(System.IntPtr h, ref POINT p);
[DllImport("user32.dll")] public static extern bool GetClipCursor(out RECT r);
[DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
'@
[W.U]::SetProcessDPIAware() | Out-Null
$lines = New-Object System.Collections.Generic.List[string]
while (-not (Test-Path $Stop)) {
  $h = (Get-Process -Id $ClientPid -ErrorAction SilentlyContinue).MainWindowHandle
  $p = New-Object W.U+POINT; [W.U]::GetCursorPos([ref]$p) | Out-Null
  $fg = [W.U]::GetForegroundWindow()
  $cx = -1; $cy = -1
  if ($h -and $h -ne [IntPtr]::Zero) { $r = New-Object W.U+RECT; [W.U]::GetClientRect($h, [ref]$r) | Out-Null; $c = New-Object W.U+POINT; $c.X = [int](($r.R)/2); $c.Y = [int](($r.B)/2); [W.U]::ClientToScreen($h, [ref]$c) | Out-Null; $cx = $c.X; $cy = $c.Y }
  $clip = New-Object W.U+RECT; [W.U]::GetClipCursor([ref]$clip) | Out-Null
  $lines.Add(("{0} cur={1},{2} mcCentre={3},{4} mcFront={5} clip={6},{7},{8},{9}" -f [DateTimeOffset]::Now.ToUnixTimeMilliseconds(), $p.X, $p.Y, $cx, $cy, ($h -eq $fg), $clip.L, $clip.T, $clip.R, $clip.B))
  Start-Sleep -Milliseconds 50
}
$lines | Set-Content -Path $Out -Encoding utf8
