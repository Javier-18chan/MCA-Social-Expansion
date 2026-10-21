# MCA Social Expansion

MCA Social Expansion amplía MCA Reborn con relaciones más dinámicas, comunicación a distancia, descubrimiento progresivo de NPC, regalos contextuales e interacciones sociales con memoria.

## Versión 0.9.2

- Los NPC revividos pueden volver a registrarse al interactuar con ellos usando un objeto social.
- Los diálogos distinguen entre hijos, nietos y bisnietos y respetan el género elegido por el jugador.
- Nuevos mensajes espontáneos para nietos y bisnietos según su etapa de vida.
- Retratos persistentes con lectura y guardado en segundo plano y sin búsquedas continuas de contactos lejanos.
- Protección frente a interacciones con NPC fallecidos desde interfaces que seguían abiertas.

Consulta [CHANGELOG.md](CHANGELOG.md) para ver los cambios de esta versión.

## Funciones

- Descubrimiento progresivo de personalidad y preferencias.
- Sistema híbrido de regalos con categorías, calidad, favoritos, gustos y rechazos.
- Compatibilidad controlada con objetos de otros mods y filtrado de regalos inválidos.
- Celular, carta, cristal y espejo mágico con retratos de los contactos.
- Bandeja de mensajes y tiempos de entrega configurables.
- Mensajes espontáneos de NPC según su relación, con respuestas y contextos del Nether y el End.
- Sonidos propios para cada interfaz de comunicación.
- Conversaciones por temas, insultos, disculpas y reconciliación, con memoria de interacciones recientes.
- Mejoras visuales y correcciones de distribución en las interfaces.

## Compatibilidad

- Minecraft 1.20.1.
- Forge 47 o posterior.
- Java 17.

Este repositorio contiene exclusivamente el código fuente de Forge 1.20.1. El JAR oficial de esta versión se encuentra en la sección Releases.

## Dependencias

MCA Reborn es la única dependencia obligatoria. Este proyecto utiliza MCA Reborn 7.7.1-alpha.1 o posterior en Minecraft 1.20.1.

Integraciones opcionales:

- MCA Quests 1.1.0 o posterior.
- MCA Conversations 1.0.0 o posterior.
- Townstead 0.7.6 o posterior.

## Compilación

Coloca estas dependencias de compilación en `libs/`:

- `minecraft-comes-alive-7.7.1-alpha.1+1.20.1-universal.jar`
- `architectury-9.2.14-forge.jar`

Después ejecuta:

```powershell
.\gradlew.bat build
```

El JAR compilado se genera en `build/libs/`. La compilación también ejecuta las pruebas de parentesco, incluidas las generaciones posteriores y los árboles familiares con ciclos.

## Idiomas

El mod incluye traducciones para inglés de Estados Unidos, español de España, español de México, español de Estados Unidos y portugués de Brasil.

## Autor

_XavierCodes_

## Licencia

GNU General Public License v3.0 only (`GPL-3.0-only`). Consulta el archivo `LICENSE` para leer los términos completos.
