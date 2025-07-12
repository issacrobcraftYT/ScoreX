# ScoreX

![ScoreX Banner](https://img.shields.io/badge/ScoreX-Ultimate%20Scoreboard-blueviolet?style=for-the-badge)

The **Ultimate Animated, Multi-Language, Customizable Scoreboard Plugin** for Spigot 1.16.5+ servers.

---

## 🚀 Features
- ✨ Animated titles and per-line effects (customizable in `animations/`)
- 🌍 Multi-language support (edit `lang/en.yaml` or add your own)
- 🔌 Integrates with PlaceholderAPI, Vault, LuckPerms, WorldGuard, BungeeCord, and CMI
- 🌐 Per-world, per-permission, and per-player scoreboards
- 🖼️ GUI scoreboard selector (`/scorex gui`)
- ♻️ Fully reloadable config and language files (`/scorex reload`)
- 🎨 Customizable lines, RGB/MiniMessage colors, and animations
- 🔊 Sound feedback, clickable lines, AFK/combat auto-hide, and more
- 🛡️ Auto-hide in regions, AFK, or combat
- 🧩 API for other plugins

---

## 📦 Installation
1. Place `ScoreX.jar` in your server's `plugins` folder.
2. Start the server to generate default config and language files.
3. *(Optional)* Install PlaceholderAPI, Vault, LuckPerms, WorldGuard, BungeeCord, and CMI for full integration.

---

## ⚙️ Configuration
- **`config.yml`**: Global settings (language, animation interval, etc.)
- **`lang/`**: Translations and all plugin messages (copy `en.yaml` to add new languages)
- **`scoreboards/`**: Custom scoreboard layouts (per-world, per-permission, or custom)
- **`animations/`**: Custom animated titles and frames

---

## 🕹️ Commands
| Command | Description |
|---------|-------------|
| `/scorex reload` | Reload config and language files |
| `/scorex toggle` | Toggle your scoreboard on/off |
| `/scorex set <name>` | Set your scoreboard |
| `/scorex list` | List available scoreboards |
| `/scorex gui` | Open scoreboard selector GUI |
| `/scorex customlines <lines>` | Set your own scoreboard lines |
| `/scorex reset` | Reset your scoreboard to default |

---

## 🏷️ Placeholders
- **Supports all PlaceholderAPI placeholders**
- **Built-in:**
  - `%player_name%`, `%vault_eco_balance%`, `%server_online%`, `%player_world%`, `%player_ping%`, `%player_health%`, `%player_max_health%`, `%player_gamemode%`, `%player_level%`, `%player_uuid%`, `%server_tps%`
  - `%scorex_currentboard%`, `%scorex_toggle%`, `%luckperms_prefix%`, `%luckperms_suffix%`, `%luckperms_primary_group%`, `%bungee_server%`, `%bungee_total%`
- *See the PlaceholderAPI wiki for more!*

---

## 🎨 Customization
- Use `{animation}` in the scoreboard title for animated titles
- Add new YAML files in `scoreboards/` for per-world, per-permission, or custom boards
- Add new YAML files in `animations/` for custom animation frames
- Edit `lang/en.yaml` to change all plugin messages and prefix
- Use RGB colors (`&#ff0000`) and MiniMessage formatting in lines/titles
- All lines and titles support placeholders and color codes

---

## 🧠 Advanced
- Scoreboards can be auto-switched by world, permission, or schedule
- Scoreboard can auto-hide when AFK, in combat, or in specific regions
- All toggle states and custom lines are per-player and persistent
- All features are reloadable without server restart
- API available for other plugins to set scoreboards

---

## 🌐 Translating
- Copy `lang/en.yaml` to `lang/<yourlang>.yaml` and translate
- Set `language: <yourlang>` in `config.yml`
- All messages use `%prefix%` for easy branding

---

## 💡 Tips & Best Practices
- Use the GUI (`/scorex gui`) for easy scoreboard selection
- Use per-permission scoreboards for VIPs or staff (`scorex.board.vip`)
- Use per-world scoreboards for minigames or special worlds
- Add your own animations in `animations/` and reference them in scoreboard YAMLs
- Use the help command or check `lang/en.yaml` for all available messages

---

## 🛠️ Support & Contributing
- For help, feature requests, or bug reports, open an issue or contact the developer.
- Contributions, translations, and suggestions are welcome!

---

Enjoy ScoreX! 🎉
