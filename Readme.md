## Raven XD 3.1

---

Raven XD 3.1 is a Minecraft Forge 1.8.9 client, maintained at
[Raven XD 3.1](https://github.com/Open-Raven/RavenXD).

## Changes

- Removed all API calls and backend references
- Spoof UID #1

## Building

Use Java 21 to run Gradle and install a Java 8 JDK for compilation. Gradle detects
locally installed JDKs; if needed, specify the Java 8 installation with
`-Porg.gradle.java.installations.paths="C:\path\to\jdk8"`.

```powershell
cd RavenXD
.\gradlew.bat build
```

The installable mod is `RavenXD/build/libs/RavenXD-3.1.jar`. Place it in your
Minecraft Forge 1.8.9 `mods` directory. Open the GUI with Right Shift.

## Decrypting Scripts - u20

Raven's protected scripts carry two recovered layers:

1. **AES-128/ECB** — key `K8ai@rUb^mj7X91P`, applied to the Base64 in a `decrypt - "<base64>"` line.
2. **Hex/XOR chain** — a 16-byte key table and a shift constant of `73`. The first two hex digits
   seed the chain and the rest are the data bytes.

The plaintext keeps a 32-character trailer, and the client stamps the formula it contains with the
user's UID: `size * 0.05506` becomes `size * 0.05` + the UID.

## Credits

- MiauMinus: most velocity modes
- OpenRise: most disablers
- Raven N+: relevant clickgui theme
- Raven B+++: customdisabler
- sunsuke - intave full velocity
- Sande: Finding an undetected way to dump and providing the dump
- Abusez: Recovering
- Mega: Recovering
- Chef: Making Raven B4
