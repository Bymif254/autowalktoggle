# AutoWalk Toggle

Mod de Fabric **solo cliente** para Minecraft Java **26.2**. Funciona en cualquier servidor, también vanilla.

Con el mod activado, pulsas una vez una tecla de movimiento (las que tengas en Controles) y se queda "enganchada" como si la mantuvieras pulsada.

- Si pulsas la misma tecla, se suelta.
- Si pulsas otra dirección, se suelta y la nueva tecla funciona normal.
- También se suelta al abrir cualquier menú, morir, cambiar de mundo o servidor, o desconectarte.

## Comandos

| Comando | Efecto |
|---|---|
| `/autowalk on` | Activa el mod |
| `/autowalk off` | Desactiva el mod (y suelta la tecla enganchada) |
| `/autowalk toggle` | Alterna el estado |
| `/autowalk status` | Muestra el estado |
| `/autowalk w` | Empieza a andar hacia delante solo (activa el mod si estaba desactivado) |
| `/autowalk s` | Empieza a andar hacia atrás solo (activa el mod si estaba desactivado) |

Por defecto empieza **desactivado**.

## Versiones

| | |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.2 |
| Fabric Loom | 1.18.2 |
| Gradle | 9.7.1 |
| Java | 25 |

## Compilar

Necesitas el JDK 25. Este repositorio no incluye el Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`). Cópialo de la plantilla oficial (https://fabricmc.net/develop/template/) y después ejecuta:

```
./gradlew build
```

El mod queda en `build/libs/autowalktoggle-1.0.0.jar`. Cópialo, junto con Fabric API, a la carpeta `.minecraft/mods`.
