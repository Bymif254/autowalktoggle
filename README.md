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
| `/autowalk menu` | Abre el menú de la ruta |
| `/autowalk route start` | Inicia la ruta guardada |
| `/autowalk route stop` | Detiene la ruta |
| `/autowalk route status` | Muestra por qué punto va la ruta |

Por defecto empieza **desactivado**.

## Ruta (cambios de dirección automáticos)

En `/autowalk menu` añades puntos con coordenadas **con decimales** (X, Y, Z) y lo que hay que hacer al llegar: Adelante, Atrás, Izquierda, Derecha o Parar.

- **Espera** (segundos, por ejemplo `0.5`): al llegar al punto se para del todo, espera ese tiempo y después pulsa la tecla nueva. Sirve para no chocar con vallas o esquinas al girar. Con `0` gira al momento.
- **Punto en mi posición** y **Aquí** rellenan las coordenadas exactas de donde estás.
- **Bucle**: al terminar el último punto, vuelve a empezar por el primero.
- **Empezar**: tecla con la que arranca la ruta al pulsar **Iniciar ruta** (o "tú pulsas").

**Precisión:** el mod no espera a pasar el punto, porque el jugador avanza unos 0,2 bloques por tick y sigue deslizándose después de soltar la tecla. Cada tick calcula, con la física de Minecraft (velocidad y rozamiento del suelo), dónde se pararía si cambiara de tecla en ese momento, y cambia en el tick que deja al jugador más cerca del punto exacto. El error máximo es de medio tick de movimiento: unos 0,1 bloques, 1-2 píxeles.

La ruta se guarda en `config/autowalktoggle-route.json`.

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
