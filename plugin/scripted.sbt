ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { ScriptedPlugin.scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

ScriptedPlugin.scriptedBufferLog := false
