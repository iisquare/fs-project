!macro customInstall
  ; 覆盖安装 / 自动升级时保留用户已有的开机启动意愿，只把命令更新到新的安装目录。
  ; 没有 Run 项就保持关闭：首次安装不默认替用户开机启动。
  ReadRegStr $0 HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "fs-bot"
  ${If} $0 != ""
    WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "fs-bot" '"$INSTDIR\fs-bot.exe" --autostart'
  ${EndIf}
!macroend

!macro customUnInstall
  ; 开机启动项由 app.setLoginItemSettings 按当前用户写入。
  ; 这两处都要清：Run 是启动命令，StartupApproved 是 Windows“启动应用”里的开关状态。
  ; 覆盖安装也会调用 uninstaller，此时保留启动项，只有真正卸载才删除。
  ${IfNot} ${isUpdated}
    DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "fs-bot"
    DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run" "fs-bot"
    DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Explorer\StartupApproved\Run32" "fs-bot"
  ${EndIf}
!macroend
