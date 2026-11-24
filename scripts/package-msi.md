# Сборка MSI (jpackage) на Windows

Требования: JDK 21 с `jpackage`, WiX Toolset 3.x в PATH.

```bat
mvn -pl client -am -DskipTests package
jpackage ^
  --type msi ^
  --name "OFD Marking Client" ^
  --app-version 1.0.0 ^
  --vendor "OFD Marking" ^
  --input client\target ^
  --main-jar ofd-marking-client-1.0.0-SNAPSHOT.jar ^
  --main-class ru.ofd.marking.client.MarkingClientApp ^
  --module-path "%JAVAFX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  --win-menu ^
  --win-shortcut ^
  --dest dist
```

Либо используйте `javafx-maven-plugin` + `jlink`/`jpackage` из JDK на машине сборки Windows.
На Linux MSI не собирается — используйте `scripts/run-client.sh`.
