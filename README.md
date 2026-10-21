# D.raviti

Mod de Minecraft para Forge.

- **Minecraft:** 1.20.1
- **Forge:** 47.4.10 o superior
- **Java:** 17

## Mods necesarios para jugar

Además de D.raviti, instala en la carpeta `mods`:

- [Lodestone](https://www.curseforge.com/minecraft/mc-mods/lodestone) para Forge 1.20.1 (efectos visuales)
- [Player Animator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) para Forge 1.20.1 (animaciones)

En desarrollo (`runClient`) se descargan solos.

## Compilar

```bash
./gradlew build
```

El `.jar` queda en `build/libs/`. Cada push a GitHub también se compila solo en la pestaña **Actions**.

## Probar en desarrollo

```bash
./gradlew genIntellijRuns
./gradlew runClient
```

En Windows usa `.\gradlew` en lugar de `./gradlew`.

## Estructura

```
src/main/java/com/markadrii/draviti/
├── Draviti.java          Clase principal (mod id: draviti)
├── registry/             Objetos, bloques, entidades y pestaña creativa
├── network/              Paquetes cliente ↔ servidor
└── client/               Teclas, renderers y eventos del cliente
src/main/resources/
├── META-INF/mods.toml
├── assets/draviti/       lang, texturas, modelos, blockstates
└── data/draviti/         recetas y tags
```
