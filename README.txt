RyzenWater 1.1.0 (Fabric 1.21.4)
Клавиша K — вкл/выкл. Настройки: Mod Menu -> RyzenWater -> настроить.

Сборка без установки чего-либо:
 1) Создай репозиторий на github.com и загрузи туда ВСЕ файлы из этой папки (включая .github).
 2) Вкладка Actions -> build -> когда станет зелёным, внизу Artifacts -> ryzenwater-jar.
 3) Внутри архива ryzenwater-1.1.0.jar -> в папку mods (нужен Fabric API, Mod Menu по желанию).

Локальная сборка: добавь gradlew/gradle из fabric-example-mod (1.21.4) и запусти ./gradlew build
