# Open2Online

Makes your local (LAN) Minecraft world reachable over the internet, without setting up port
forwarding by hand.

The mod sits on top of Minecraft's own "Open to LAN" and adds the missing half: it asks your router
to open the port for you, over **UPnP**, **NAT-PMP** or **PCP**, and tells you the address to hand
out. When you leave the world, it closes the port again.

**[Modrinth](https://modrinth.com/project/open2online)** ·
**[Issues](https://github.com/v14d4n/open2online/issues)**

---

## Requirements

|            | Fabric                            | NeoForge      |
| ---------- | --------------------------------- | ------------- |
| Minecraft  | 1.21.11                           | 1.21.11       |
| Java       | 21                                | 21            |
| Needs      | Architectury, Fabric API          | Architectury  |


## Using it

Open a world, press Escape, and use the Open2Online button next to "Open to LAN".

- **Start Online World** - opens the port and puts the world on the internet. The address appears in
  chat; click it to copy.
- **Start LAN World** - the plain vanilla thing, without touching the router.
- Publishing takes a few seconds while the router is found. Opening the pause menu calls it off.

## What you can set

Everything lives under **Advanced Settings**.

- **Port** and **Max Players.**
- **Library** - which protocol implementation opens the port. **Auto** tries them in turn and
  remembers what worked; **PortMapper** is the slowest and the recommended one, being the only one
  that also speaks NAT-PMP and PCP.
- **Whitelist** - let only certain players connect. Names are checked against Minecraft's own rule,
  so one nobody could join under cannot be added by mistake.
- **Require License** - verify that joining players own the game. With it off, anyone can join under
  any name.
- **Hide IP** - show "Click to copy" in chat instead of the address itself, so it cannot be read off
  a screenshot or a stream. It changes nothing about who can connect.
- **Auto Start** - open the world on its own shortly after you enter it, to the internet or to the
  local network, after a delay you choose. The pause menu cancels a pending start.
- **Notifications** - updates, licence and whitelist warnings, each switched separately.
- **Allow PVP.**
- **Recreate Firewall Rules** - Windows only, and only for the case where the server publishes with
  no errors but nobody can reach it. It replaces Minecraft's firewall rules with correct ones and
  asks for administrator rights to do so.

## When it does not work

The mod tries to tell you why, in chat, rather than leaving you to guess.

- **Your provider uses CGNAT** - your external address is shared with other subscribers, and no
  port mapping can make you reachable. Ask your provider for a public address; nothing on your side
  fixes this.
- **Your router is itself behind another router** - the same problem one hop closer to home. The
  port was opened on the wrong device.
- **Port is already in use** - something else on your machine has it. Change the port.
- **Failed to open the port with every library** - the router refused, or UPnP is switched off in its
  settings. Try a different port, or turn UPnP on.
- **Everything succeeds but nobody can connect** - this is what the firewall button is for.

## Problems, ideas, requests

Everything goes to [issues](https://github.com/v14d4n/open2online/issues): a bug, a feature you want,
a rough edge worth smoothing. Requests are as welcome as reports, and nothing is too small to write
down.

## Built with

Bundled and relocated into the mod's own namespace, so they cannot clash with anything else:

- [WeUPnP](https://github.com/bitletorg/weupnp) - UPnP
- [WaifUPnP](https://github.com/adolfintel/WaifUPnP) - UPnP
- [PortMapper](https://github.com/offbynull/portmapper) - UPnP, NAT-PMP, PCP

## Building

```
./gradlew build
```

Jars land in `fabric/build/libs` and `neoforge/build/libs`.

## Licence

MIT.
