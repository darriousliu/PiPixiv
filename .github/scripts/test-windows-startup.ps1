[CmdletBinding()]
param(
    [string]$MsiPath = 'desktopApp/build/compose/binaries/main-release/msi/*.msi',
    [string]$OutputDirectory = 'desktopApp/build/reports/windows-startup',
    [ValidateSet('Tao', 'Awt', 'Auto')]
    [string]$ExpectedWindowBackend = 'Tao'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# This installs into the runner's real per-user location. Never run it on a
# developer machine or a persistent self-hosted runner.
if ($env:OS -ne 'Windows_NT' -or $env:GITHUB_ACTIONS -ne 'true' -or
    $env:RUNNER_ENVIRONMENT -ne 'github-hosted') {
    throw 'This startup check only runs on disposable GitHub-hosted Windows runners.'
}

$output = [System.IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Path $output -Force | Out-Null
$installLog = Join-Path $output 'msi-install.log'
$stdoutLog = Join-Path $output 'app-stdout.log'
$stderrLog = Join-Path $output 'app-stderr.log'
foreach ($log in @($installLog, $stdoutLog, $stderrLog)) {
    New-Item -ItemType File -Path $log -Force | Out-Null
}

$result = [ordered]@{
    status = 'failed'
    startedAt = [DateTime]::UtcNow.ToString('o')
    msiPath = $MsiPath
    msiSha256 = $null
    msiProductCode = $null
    msiLauncherComponentCode = $null
    installDirectory = $null
    installerExitCode = $null
    nativeLibrary = $null
    anglePayload = $null
    processId = $null
    launcherPid = $null
    applicationPid = $null
    processExitCode = $null
    windowReadySeconds = $null
    stableSeconds = 30
    observedStableSeconds = $null
    window = $null
    expectedWindowBackend = $ExpectedWindowBackend
    expectedWindowClasses = @()
    screenshot = $null
    screenshotError = $null
    desktopScreenshot = $null
    processTree = $null
    jvmDiagnostics = @()
    diagnosticErrors = @()
    error = $null
    cleanupError = $null
    finishedAt = $null
}
$application = $null
$launcher = $null
$launcherStartedAt = $null
$trackedApplications = [System.Collections.Generic.Dictionary[int, System.Diagnostics.Process]]::new()
$installer = $null
$windowHandle = [IntPtr]::Zero
$checks = [System.Collections.Generic.List[object]]::new()
# Nucleus 2.5.14 uses Tao's default Windows class. Auto is only for the
# workflow's explicit reuse of an older MSI, which can still contain AWT.
$expectedWindowClasses = @(switch ($ExpectedWindowBackend) {
    'Tao' { 'Window Class' }
    'Awt' { 'SunAwtFrame' }
    'Auto' { 'Window Class'; 'SunAwtFrame' }
})
$result.expectedWindowClasses = $expectedWindowClasses

function Get-MsiLauncherMetadata {
    param([string]$PackagePath)

    $comInstaller = $null
    $database = $null
    $view = $null
    $record = $null
    try {
        $comInstaller = New-Object -ComObject WindowsInstaller.Installer
        $database = $comInstaller.OpenDatabase($PackagePath, 0)
        $view = $database.OpenView('SELECT `Value` FROM `Property` WHERE `Property` = ''ProductCode''')
        # COM can emit an empty return value into PowerShell's success stream.
        # Only the launcher metadata object should leave this function.
        [void]$view.Execute()
        $record = $view.Fetch()
        if ($null -eq $record) { throw 'The MSI does not declare a ProductCode.' }
        $productCode = [string]$record.GetType().InvokeMember('StringData', 'GetProperty', $null, $record, @(1))
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($record) | Out-Null
        $record = $null
        [void]$view.Close()
        [System.Runtime.InteropServices.Marshal]::ReleaseComObject($view) | Out-Null
        $view = $null

        # Resolve the launcher's registered component, regardless of whether
        # jpackage or electron-builder authored the MSI directory tree.
        $view = $database.OpenView('SELECT `File`.`File`, `File`.`FileName`, `Component`.`ComponentId`, `Component`.`KeyPath` FROM `File`, `Component` WHERE `File`.`Component_` = `Component`.`Component`')
        [void]$view.Execute()
        $launchers = [System.Collections.Generic.List[object]]::new()
        while ($null -ne ($record = $view.Fetch())) {
            $columns = @(foreach ($column in 1..4) {
                [string]$record.GetType().InvokeMember('StringData', 'GetProperty', $null, $record, @($column))
            })
            $longName = ($columns[1] -split '\|')[-1]
            if ($longName -ieq 'PiPixiv.exe') {
                if (-not $columns[2] -or $columns[3] -cne $columns[0]) {
                    throw 'The MSI launcher must be the key file of a registered component.'
                }
                $launchers.Add([pscustomobject]@{ productCode = $productCode; componentCode = $columns[2] })
            }
            [System.Runtime.InteropServices.Marshal]::ReleaseComObject($record) | Out-Null
            $record = $null
        }
        if ($launchers.Count -ne 1) { throw 'The MSI must contain exactly one PiPixiv.exe launcher.' }
        return $launchers[0]
    }
    finally {
        if ($null -ne $view) { [void]$view.Close() }
        foreach ($comObject in @($record, $view, $database, $comInstaller)) {
            if ($null -ne $comObject) {
                [System.Runtime.InteropServices.Marshal]::ReleaseComObject($comObject) | Out-Null
            }
        }
    }
}

function Get-MsiRegistration {
    param([string]$ProductCode, [string]$ComponentCode)

    $comInstaller = New-Object -ComObject WindowsInstaller.Installer
    try {
        $state = [int]$comInstaller.GetType().InvokeMember('ProductState', 'GetProperty', $null, $comInstaller, @($ProductCode))
        $path = $null
        if ($state -ne -1) {
            $path = [string]$comInstaller.GetType().InvokeMember('ComponentPath', 'GetProperty', $null, $comInstaller, @($ProductCode, $ComponentCode))
        }
        return [pscustomobject]@{ state = $state; path = $path }
    }
    finally { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($comInstaller) | Out-Null }
}

function Get-ApplicationProcessTree {
    param([int]$RootProcessId, [DateTime]$RootStartedAt, [string]$FamilyExecutable, [switch]$IncludeJvmModules)

    $records = [System.Collections.Generic.List[object]]::new()
    $pending = [System.Collections.Generic.Queue[object]]::new()
    $pending.Enqueue([pscustomobject]@{ processId = $RootProcessId; startedAt = $RootStartedAt; isRoot = $true })
    $seen = [System.Collections.Generic.HashSet[int]]::new()
    while ($pending.Count -gt 0) {
        $expected = $pending.Dequeue()
        if (-not $seen.Add($expected.processId)) { continue }
        $row = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $($expected.processId)" -OperationTimeoutSec 5
        if ($null -eq $row) { continue }
        # A recycled PID must not bring an unrelated process into diagnostics.
        if ([Math]::Abs(($row.CreationDate.ToUniversalTime() - $expected.startedAt.ToUniversalTime()).TotalSeconds) -gt 1) {
            continue
        }
        $record = [ordered]@{
            processId = [int]$row.ProcessId
            parentProcessId = [int]$row.ParentProcessId
            isRoot = $expected.isRoot
            name = $row.Name
            executablePath = $row.ExecutablePath
            commandLine = $row.CommandLine
            startedAt = $row.CreationDate.ToUniversalTime().ToString('o')
            hasExited = $null
            mainWindowHandle = $null
            mainWindowTitle = $null
            mainWindowClass = $null
            responding = $null
            visible = $false
            loadedJvm = $false
            inspectionError = $null
        }
        $current = $null
        try {
            $current = Get-Process -Id $record.processId
            $current.Refresh()
            $record.hasExited = $current.HasExited
            $record.mainWindowHandle = $current.MainWindowHandle.ToInt64()
            $record.mainWindowTitle = $current.MainWindowTitle
            $record.responding = $current.Responding
            if ($current.MainWindowHandle -ne [IntPtr]::Zero) {
                $className = [System.Text.StringBuilder]::new(256)
                [PiPixivStartupWindow]::GetClassName($current.MainWindowHandle, $className, $className.Capacity) | Out-Null
                $record.mainWindowClass = $className.ToString()
                $record.visible = [PiPixivStartupWindow]::IsWindowVisible($current.MainWindowHandle)
            }
            if ($IncludeJvmModules) {
                $record.loadedJvm = @($current.Modules | Where-Object { $_.ModuleName -ieq 'jvm.dll' }).Count -gt 0
            }
        }
        catch { $record.inspectionError = $_.Exception.Message }
        finally { if ($null -ne $current) { $current.Dispose() } }
        $records.Add([pscustomobject]$record)
        # Query each known parent's direct children; never dump all runner processes.
        $children = @(Get-CimInstance -ClassName Win32_Process -Filter "ParentProcessId = $($record.processId)" -OperationTimeoutSec 5)
        foreach ($child in $children) {
            if ($child.CreationDate -ge $row.CreationDate -and
                (-not $FamilyExecutable -or $child.ExecutablePath -ieq $FamilyExecutable)) {
                $pending.Enqueue([pscustomobject]@{
                    processId = [int]$child.ProcessId
                    startedAt = $child.CreationDate
                    isRoot = $false
                })
            }
        }
    }
    return $records.ToArray()
}

function Invoke-JvmDiagnostic {
    param([string]$JcmdPath, [int]$TargetProcessId, [string]$Command, [string]$Directory)

    $stem = 'jcmd-{0}-{1}' -f $TargetProcessId, ($Command -replace '[^A-Za-z0-9_.-]', '_')
    $diagnostic = [ordered]@{
        processId = $TargetProcessId
        command = $Command
        timeoutSeconds = 12
        timedOut = $false
        exitCode = $null
        stdout = Join-Path $Directory "$stem.stdout.log"
        stderr = Join-Path $Directory "$stem.stderr.log"
        error = $null
    }
    $probe = $null
    try {
        $probe = Start-Process -FilePath $JcmdPath -ArgumentList "$TargetProcessId $Command" `
            -RedirectStandardOutput $diagnostic.stdout -RedirectStandardError $diagnostic.stderr `
            -WindowStyle Hidden -PassThru
        if ($probe.WaitForExit(12000)) {
            $probe.Refresh()
            $diagnostic.exitCode = $probe.ExitCode
        }
        else { $diagnostic.timedOut = $true }
    }
    catch { $diagnostic.error = $_.Exception.Message }
    finally {
        if ($null -ne $probe) {
            try {
                $probe.Refresh()
                if (-not $probe.HasExited) {
                    $probe.Kill()
                    $probe.WaitForExit(2000) | Out-Null
                }
            }
            catch { $diagnostic.error = $_.Exception.Message }
            finally { $probe.Dispose() }
        }
    }
    return [pscustomobject]$diagnostic
}

try {
    $packages = @(Get-Item -Path $MsiPath)
    if ($packages.Count -ne 1 -or $packages[0].Extension -ne '.msi') {
        throw 'MsiPath must resolve to exactly one MSI package.'
    }
    $package = $packages[0].FullName
    $result.msiPath = $package
    $result.msiSha256 = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash
    foreach ($existingDirectory in @(
        (Join-Path $env:LOCALAPPDATA 'PiPixiv'),
        (Join-Path $env:LOCALAPPDATA 'Programs/PiPixiv')
    )) {
        if (Test-Path -LiteralPath $existingDirectory) {
            throw "Refusing to replace an existing installation at $existingDirectory."
        }
    }
    $msiLauncher = Get-MsiLauncherMetadata -PackagePath $package
    $result.msiProductCode = $msiLauncher.productCode
    $result.msiLauncherComponentCode = $msiLauncher.componentCode
    $registration = Get-MsiRegistration -ProductCode $msiLauncher.productCode -ComponentCode $msiLauncher.componentCode
    if ($registration.state -ne -1) {
        throw "Refusing to replace an already registered MSI product: $($msiLauncher.productCode)."
    }

    Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class PiPixivStartupWindow {
    [StructLayout(LayoutKind.Sequential)]
    public struct Rect { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    public static extern int GetClassName(IntPtr hwnd, StringBuilder name, int maxCount);
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool IsWindow(IntPtr hwnd);
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool IsWindowVisible(IntPtr hwnd);
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool GetWindowRect(IntPtr hwnd, out Rect rect);
}
'@

    # Use the MSI's authored install scope. Nucleus defaults to ALLUSERS=2 and
    # MSIINSTALLPERUSER=1; clearing ALLUSERS disables its per-user folder redirection.
    $installArguments = '/i "{0}" /qn /norestart /L*V "{1}"' -f $package, $installLog
    $installer = Start-Process -FilePath (Join-Path $env:SystemRoot 'System32/msiexec.exe') `
        -ArgumentList $installArguments -PassThru
    if (-not $installer.WaitForExit(300000)) {
        throw 'MSI installation did not complete within five minutes.'
    }
    $installer.Refresh()
    $result.installerExitCode = $installer.ExitCode
    if ($installer.ExitCode -notin @(0, 3010)) {
        throw "MSI installation failed with exit code $($installer.ExitCode)."
    }

    $registration = Get-MsiRegistration -ProductCode $msiLauncher.productCode -ComponentCode $msiLauncher.componentCode
    if ($registration.state -ne 5 -or -not $registration.path) {
        throw 'The MSI did not register an installed launcher component for the current user.'
    }
    $executable = [System.IO.Path]::GetFullPath($registration.path)
    $installDirectory = Split-Path -Parent $executable
    $result.installDirectory = $installDirectory
    $userDirectory = [System.IO.Path]::GetFullPath($env:LOCALAPPDATA).TrimEnd('\') + '\'
    if (-not $executable.StartsWith($userDirectory, [System.StringComparison]::OrdinalIgnoreCase) -or
        [System.IO.Path]::GetFileName($executable) -ine 'PiPixiv.exe') {
        throw "The MSI launcher is not installed in the expected per-user location: $executable"
    }
    $library = Join-Path $installDirectory 'app/resources/composeResources/files/mmkv/mmkvc.dll'
    if (-not (Test-Path -LiteralPath $executable -PathType Leaf)) {
        throw "Installed executable does not exist: $executable"
    }
    if (-not (Test-Path -LiteralPath $library -PathType Leaf)) {
        throw "Installed MMKV native library does not exist: $library"
    }
    $libraryInfo = Get-Item -LiteralPath $library
    if ($libraryInfo.Length -eq 0) {
        throw "Installed MMKV native library is empty: $library"
    }
    $result.nativeLibrary = [ordered]@{
        path = $library
        bytes = $libraryInfo.Length
        sha256 = (Get-FileHash -LiteralPath $library -Algorithm SHA256).Hash
    }

    # Nucleus 2.5.18 extracts ANGLE from the packaged JAR into a per-user
    # cache. Check the installed payload before running the launcher.
    # Auto also accepts legacy AWT packages, which do not ship these entries.
    if ($ExpectedWindowBackend -eq 'Tao') {
        $jarPath = Join-Path $installDirectory 'app/desktopApp.jar'
        $result.anglePayload = [ordered]@{ jar = $jarPath; entries = [ordered]@{} }
        $jar = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
        try {
            foreach ($name in @('libEGL.dll', 'libGLESv2.dll', 'nucleus_tao_gl.dll')) {
                $entryPath = "nucleus/native/win32-x64/$name"
                $entry = $jar.GetEntry($entryPath)
                if ($null -eq $entry -or $entry.Length -eq 0) {
                    throw "Installed ANGLE payload is missing or empty: $entryPath in $jarPath"
                }
                $stream = $entry.Open()
                try {
                    $result.anglePayload.entries[$entryPath] = [ordered]@{
                        bytes = $entry.Length
                        sha256 = (Get-FileHash -InputStream $stream -Algorithm SHA256).Hash
                    }
                }
                finally { $stream.Dispose() }
            }
        }
        finally { $jar.Dispose() }
    }

    $startup = [System.Diagnostics.Stopwatch]::StartNew()
    $launcher = Start-Process -FilePath $executable -WorkingDirectory $installDirectory `
        -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog -PassThru
    $result.launcherPid = $launcher.Id
    $launcherStartedAt = $launcher.StartTime
    $trackedApplications.Add($launcher.Id, $launcher)
    while ($startup.Elapsed.TotalSeconds -lt 60) {
        $launcher.Refresh()
        if ($launcher.HasExited) {
            $result.processExitCode = $launcher.ExitCode
            throw "PiPixiv launcher exited before its main window appeared (exit code $($launcher.ExitCode))."
        }
        # JDK 25's Windows launcher can restart the same EXE as a child and
        # wait in a parent without a main window. Follow only this EXE's chain.
        $family = @(Get-ApplicationProcessTree -RootProcessId $launcher.Id `
            -RootStartedAt $launcherStartedAt -FamilyExecutable $executable)
        foreach ($member in $family) {
            if (-not $trackedApplications.ContainsKey($member.processId)) {
                $candidate = Get-Process -Id $member.processId
                if ([Math]::Abs(($candidate.StartTime.ToUniversalTime() - [DateTime]::Parse($member.startedAt).ToUniversalTime()).TotalSeconds) -gt 1) {
                    $candidate.Dispose()
                    continue
                }
                $trackedApplications.Add($member.processId, $candidate)
            }
            $check = [ordered]@{
                elapsedSeconds = [Math]::Round($startup.Elapsed.TotalSeconds, 2)
                processId = $member.processId
                handle = $member.mainWindowHandle
                title = $member.mainWindowTitle
                class = $member.mainWindowClass
                responding = $member.responding
                visible = $member.visible
            }
            $checks.Add($check)
            if ($null -ne $check.handle -and $check.handle -ne 0) { $windowHandle = [IntPtr]$check.handle }
            if ($check.title -ceq 'PiPixiv' -and $expectedWindowClasses -ccontains $check.class -and
                $check.responding -and $check.visible) {
                $application = $trackedApplications[$member.processId]
                $result.processId = $application.Id
                $result.applicationPid = $application.Id
                $windowHandle = [IntPtr]$check.handle
                $result.windowReadySeconds = $check.elapsedSeconds
                $result.window = $check
                break
            }
        }
        if ($null -ne $result.window) { break }
        Start-Sleep -Seconds 1
    }
    if ($null -eq $result.window) {
        throw "No responding PiPixiv $ExpectedWindowBackend main window appeared within 60 seconds."
    }

    # MMKV initializes synchronously before the main window in main.kt.
    # A launcher error dialog cannot satisfy the title and backend class checks.
    $stability = [System.Diagnostics.Stopwatch]::StartNew()
    do {
        $application.Refresh()
        if ($application.HasExited) {
            $result.processExitCode = $application.ExitCode
            throw "PiPixiv exited during the stability check (exit code $($application.ExitCode))."
        }
        $stableClass = [System.Text.StringBuilder]::new(256)
        [PiPixivStartupWindow]::GetClassName($windowHandle, $stableClass, $stableClass.Capacity) | Out-Null
        if ($application.MainWindowHandle -ne $windowHandle -or
            -not [PiPixivStartupWindow]::IsWindow($windowHandle) -or
            -not [PiPixivStartupWindow]::IsWindowVisible($windowHandle) -or
            $application.MainWindowTitle -cne 'PiPixiv' -or
            $stableClass.ToString() -cne $result.window.class -or -not $application.Responding) {
            throw 'The PiPixiv main window disappeared, changed, or stopped responding.'
        }
        if ($stability.Elapsed.TotalSeconds -ge $result.stableSeconds) { break }
        Start-Sleep -Seconds 1
    } while ($true)
    $result.observedStableSeconds = [Math]::Round($stability.Elapsed.TotalSeconds, 2)
    $result.status = 'passed'
}
catch {
    $result.error = $_.Exception.Message
    Write-Warning $result.error
}
finally {
    # Capture only the app window, before stopping the process. Screenshot
    # failures are diagnostic and must not hide the actual startup result.
    if ($windowHandle -ne [IntPtr]::Zero) {
        $bitmap = $null
        $graphics = $null
        try {
            $rectangle = [PiPixivStartupWindow+Rect]::new()
            if (-not [PiPixivStartupWindow]::IsWindow($windowHandle) -or
                -not [PiPixivStartupWindow]::GetWindowRect($windowHandle, [ref]$rectangle)) {
                throw 'The application window is no longer available for a screenshot.'
            }
            Add-Type -AssemblyName System.Drawing
            $width = $rectangle.Right - $rectangle.Left
            $height = $rectangle.Bottom - $rectangle.Top
            if ($width -le 0 -or $height -le 0) { throw 'The application window has no visible area.' }
            $bitmap = [System.Drawing.Bitmap]::new($width, $height)
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            $graphics.CopyFromScreen($rectangle.Left, $rectangle.Top, 0, 0, $bitmap.Size)
            $screenshot = Join-Path $output 'main-window.png'
            $bitmap.Save($screenshot, [System.Drawing.Imaging.ImageFormat]::Png)
            $result.screenshot = $screenshot
        }
        catch {
            $result.screenshotError = $_.Exception.Message
            Write-Warning "Window screenshot failed: $($result.screenshotError)"
        }
        finally {
            if ($null -ne $graphics) { $graphics.Dispose() }
            if ($null -ne $bitmap) { $bitmap.Dispose() }
        }
    }

    if ($result.status -ne 'passed' -and $null -eq $result.screenshot) {
        $desktopBitmap = $null
        $desktopGraphics = $null
        try {
            # The entry guard limits this fallback to disposable hosted CI.
            Add-Type -AssemblyName System.Drawing
            Add-Type -AssemblyName System.Windows.Forms
            $bounds = [System.Windows.Forms.SystemInformation]::VirtualScreen
            $desktopBitmap = [System.Drawing.Bitmap]::new($bounds.Width, $bounds.Height)
            $desktopGraphics = [System.Drawing.Graphics]::FromImage($desktopBitmap)
            $desktopGraphics.CopyFromScreen($bounds.Left, $bounds.Top, 0, 0, $desktopBitmap.Size)
            $desktopScreenshot = Join-Path $output 'desktop-at-failure.png'
            $desktopBitmap.Save($desktopScreenshot, [System.Drawing.Imaging.ImageFormat]::Png)
            $result.desktopScreenshot = $desktopScreenshot
        }
        catch {
            $result.diagnosticErrors += "Desktop screenshot: $($_.Exception.Message)"
            Write-Warning $result.diagnosticErrors[-1]
        }
        finally {
            if ($null -ne $desktopGraphics) { $desktopGraphics.Dispose() }
            if ($null -ne $desktopBitmap) { $desktopBitmap.Dispose() }
        }
    }

    if ($null -ne $launcher -and $null -ne $launcherStartedAt) {
        try {
            $processTree = @(Get-ApplicationProcessTree -RootProcessId $launcher.Id `
                -RootStartedAt $launcherStartedAt -IncludeJvmModules)
            $treePath = Join-Path $output 'application-process-tree.json'
            ConvertTo-Json -InputObject $processTree -Depth 5 | Set-Content -LiteralPath $treePath -Encoding utf8
            $result.processTree = $treePath
            if ($result.status -ne 'passed') {
                $jcmd = Join-Path $env:JAVA_HOME 'bin/jcmd.exe'
                if (-not (Test-Path -LiteralPath $jcmd -PathType Leaf)) {
                    throw "The CI JDK does not provide jcmd: $jcmd"
                }
                foreach ($target in ($processTree | Sort-Object loadedJvm -Descending)) {
                    if ($target.isRoot -or $target.loadedJvm -or $target.name -imatch '^javaw?\.exe$') {
                        foreach ($command in @('VM.command_line', 'Thread.print -l')) {
                            $result.jvmDiagnostics += Invoke-JvmDiagnostic -JcmdPath $jcmd `
                                -TargetProcessId $target.processId -Command $command -Directory $output
                        }
                    }
                }
            }
        }
        catch {
            $result.diagnosticErrors += "Application process/JVM diagnostics: $($_.Exception.Message)"
            Write-Warning $result.diagnosticErrors[-1]
        }
    }

    # Stop only the recorded same-EXE chain, newest descendants before parents.
    # Preserve Process objects so cleanup never targets a recycled PID by name.
    $cleanupProcesses = @($trackedApplications.Values | Sort-Object StartTime -Descending) + @($installer)
    foreach ($ownedProcess in $cleanupProcesses) {
        if ($null -eq $ownedProcess) { continue }
        try {
            $ownedProcess.Refresh()
            if (-not $ownedProcess.HasExited) {
                $ownedProcess.Kill()
                $ownedProcess.WaitForExit(10000) | Out-Null
            }
            $ownedProcess.Dispose()
        }
        catch {
            $result.cleanupError = $_.Exception.Message
            Write-Warning "Owned-process cleanup failed: $($result.cleanupError)"
        }
    }
    $result.finishedAt = [DateTime]::UtcNow.ToString('o')
    ConvertTo-Json -InputObject ($checks.ToArray()) -Depth 5 | Set-Content -LiteralPath (Join-Path $output 'window-checks.json') -Encoding utf8
    $result | ConvertTo-Json -Depth 5 | Tee-Object -FilePath (Join-Path $output 'result.json')
}

if ($result.status -ne 'passed') { exit 1 }
