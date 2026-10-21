# D.raviti

Mod de Minecraft con el poder del **Dios de la Gravedad**: el Satan Gravity de Shiki Granbell (Edens Zero), con todas
sus técnicas, el Overdrive y sus dos formas.

- **Minecraft:** 1.20.1
- **Forge:** 47.4.10 o superior
- **Java:** 17

## Mods necesarios para jugar

Además de D.raviti, instala en la carpeta `mods`:

- [Lodestone](https://www.curseforge.com/minecraft/mc-mods/lodestone) para Forge 1.20.1 (efectos visuales)
- [Player Animator](https://www.curseforge.com/minecraft/mc-mods/playeranimator) para Forge 1.20.1 (animaciones)

En desarrollo (`runClient`) se descargan solos.

**Opcional:** [Photon](https://www.curseforge.com/minecraft/mc-mods/photon) para Forge 1.20.1. Con Photon instalado los
efectos son los de Photon (`assets/draviti/fx/`, 68 efectos); sin él, D.raviti usa sus efectos de Lodestone. Así se
puede jugar con quien no lo tenga.

## Cómo se consigue el poder

```
/draviti dar @s            da el Dios de la Gravedad (con el éter lleno)
/draviti quitar @s         lo quita
/draviti eter @s 1000      llena la barra de éter
/draviti cinetica @s 1000  llena la energía cinética (Impacto Gravitatorio)
/draviti lista             poderes que tienes
```

## Barra de habilidades y teclas

Es la misma barra y el mismo sistema que **Cursed Crown: Powers**: abajo a la derecha, habilidades con
**V, B, N, M y coma**, **X** cambia de lista (Shift + X hacia atrás). Con el chat abierto (T) se ven los nombres.
Al lado salen dos barras: **éter** (morado), que gastan las técnicas y se recarga solo, y **energía cinética**
(dorada), que se llena al recibir golpes y caídas.

Posición de la barra: `config/draviti-client.toml` → `bar_position`. Ahí también se puede quitar el temblor de pantalla.

### Junto a Cursed Crown: Powers

D.raviti no necesita Cursed Crown, pero si está instalado:

- Las teclas son las suyas: en **Controles** solo sale "Cursed Crown: Poderes" y las de D.raviti desaparecen.
  Si cambias una tecla allí, cambia para los dos mods.
- Las dos barras son una sola: **X** pasa por las listas de Cursed Crown y luego por las del Dios de la Gravedad.
  Se dibuja en la posición que tenga la barra de Cursed Crown.

## Técnicas

| Lista | Tecla | Técnica | Qué hace |
|---|---|---|---|
| 1 | V | Ingravidez | Flotas: salto sube, Shift baja. Contra una pared puedes andar por ella. Sin daño de caída |
| 1 | B | Puño Gravitatorio (Jūken) | El brazo brilla y el puñetazo lanza a todos los de delante y revienta el suelo |
| 1 | N | Cañón Gravitatorio (Jūdan) | Forma una esfera de gravedad en el puño y la lanza; su peso empuja al objetivo |
| 1 | M | Onda Gravitatoria (Jūryokuha) | Palma en el suelo: una espiral atrae hacia ti a todos (y los objetos) |
| 1 | , | Centro de Gravedad (Jūshin) | Al que miras le sube la gravedad y se estrella contra el suelo |
| 2 | V | Ráfaga de Puños (Jūken Ranbu) | Mantén: lluvia de puñetazos y un golpe final que los manda lejos |
| 2 | B | Patada Molinete (Jūsen Kyaku) | Patada giratoria en el aire que barre todo alrededor |
| 2 | N | Cometa Gravitatorio (Jūsuisei) | Salto enorme y caída con los pies cargados de gravedad |
| 2 | M | Placaje Gravitatorio | Vuelas hacia el objetivo y lo clavas en el suelo de una patada |
| 2 | , | Impacto Gravitatorio | Convierte la energía cinética absorbida en un puñetazo hacia abajo (más energía, más grande) |
| 3 | V | Campo Gravitatorio (Jūryokuba) | 10 s: los que entran quedan aplastados y los ataques caen al suelo |
| 3 | B | Antigravedad (Hanjūryoku) | Desvía flechas y proyectiles; lo que tocas o te toca sale despedido |
| 3 | N | Peso Infernal | Aumenta el peso de todos los de alrededor sin tocarlos (no pueden saltar ni volar) |
| 3 | M | Ingravidez Ajena | El objetivo pierde su peso y flota sin control |
| 3 | , | Meteor Breaker | Mantén para cargar el puño; al soltar sale un meteoro de gravedad |
| 4 | V | Cielo Negro (Kokuten) | Técnica definitiva (éter lleno): una bola de gravedad que crece y lo aplasta todo |
| 4 | B | Overdrive | Pelo y piel más claros, marcas de éter y cuernos de energía. Más fuerza, velocidad y aguante; técnicas x1,5 |
| 4 | N | Overdrive Completo | (con Overdrive) Pelo blanco, brazos oscuros con marcas moradas, cuernos negros. Agujero negro que lo atrae todo; técnicas x2 |

## Formas y colores

| Forma | Color de los efectos | Daño | Tamaño (alcance, radios, cráteres) |
|---|---|---|---|
| Normal | Turquesa pálido (como el Ether Gear) | x1 | x1 |
| Overdrive | Turquesa pálido | x1,6 | x1,3 |
| Overdrive Completo | Morado | x2,4 | x1,7 |

Al transformarte te encoges concentrando el éter (no te puedes mover un momento), la tierra tiembla y al final estallas
con el nuevo aspecto. En el Overdrive Completo se oscurecen brazos y piernas y el pelo se vuelve blanco.

Con el poder siempre llevas la **marca del Ether Gear** en el antebrazo derecho (líneas turquesa como las de Shiki).

## Controles extra

- **Ingravidez + W dos veces en el aire:** vuelo sónico (vuelas muy rápido hacia donde miras mientras mantienes W;
  arrollas lo que pillas). Gasta éter.
- **Ingravidez + A, S o D dos veces:** dash hacia la izquierda, atrás o la derecha.
- **Shift tres veces seguidas:** las técnicas rompen o dejan de romper bloques (se guarda para cada jugador).

**Pasivas:** fuerza inmensa (los golpes normales empujan), aguante (menos daño), salta más alto ajustando su peso y
aguanta caídas, absorbe la energía cinética de los golpes y caídas.

Las técnicas rompen bloques (cráteres) pero nunca cofres ni bloques con inventario, ni en zonas protegidas
(FTB Chunks...). Todo se ajusta en `config/draviti-common.toml`: daño, alcance, éter, enfriamientos, romper bloques
y si dañan a otros jugadores.

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
├── Draviti.java      Clase principal (mod id: draviti)
├── ability/          Sistema de habilidades (el de Cursed Crown: Powers)
├── power/            Poderes y sus habilidades (Powers.java)
├── data/             Datos del jugador, teclas en el servidor y sincronización
├── gravity/          El Dios de la Gravedad: técnicas, pasivas y utilidades
├── entity/           Esfera de gravedad (cañón, meteoro, Cielo Negro)
├── effect/           Efectos Pesado e Ingrávido
├── network/          Paquetes cliente ↔ servidor
├── config/           draviti-common.toml y draviti-client.toml
└── client/           Barra, teclas, compatibilidad con Cursed Crown, efectos, animaciones y capas del jugador
```

## Añadir un poder nuevo

1. Escribe sus técnicas como `AbilityBehavior` (mira `gravity/GravityAbilities.java`).
2. Regístralo en `power/Powers.java` con `create("id")...add(...)...register()`.
3. Añade sus textos en `lang/es_es.json` y `lang/en_us.json` y sus iconos en `textures/gui/icons/<poder>/`.
