<img src="./icon.png" alt="Icon" width="128">

# PatchEditor

[![Mindustry v159](https://img.shields.io/badge/Mindustry-v159-blue?logo=mindustry)](https://github.com/Anuken/Mindustry)
[![Version](https://img.shields.io/github/v/release/Dustdustry/PatchEditor?color=green&label=release)](https://github.com/Dustdustry/PatchEditor/releases)
[![Downloads](https://img.shields.io/github/downloads/Dustdustry/PatchEditor/total?color=brightgreen&label=downloads)](https://github.com/Dustdustry/PatchEditor/releases)
[![Stars](https://img.shields.io/github/stars/Dustdustry/PatchEditor?style=flat&color=yellow)](https://github.com/Dustdustry/PatchEditor/stargazers)

A Mindustry mod providing an in-game **GUI** for DataPatcher — create and edit patches visually, without leaving the game.

This mod helps you create patches faster and more intuitively:

- **No more wiki diving** — all available fields are displayed in a tree view, with inline notes.
- **Stay in the game** — edit patches directly in Mindustry, preview results instantly.
- **Visual selectors** — pick textures, sounds, colors, effects and more via dedicated dialogs.
- **Export & share** — generate clean HJSON/JSON patches ready for publishing.

![PatchEditor](https://github.com/user-attachments/assets/40737915-3ee4-4e39-b1e0-69706837f3ef)

---

## Features

### Core Editing

- Edit **numbers**, **strings**, **booleans** in-place
- Edit **Color** via game palette picker
- Edit **Interp** (interpolation curves)
- Edit **Consumer** (block I/O configuration)
- Edit **Attribute**
- Edit **BuildVisibility**
- Edit **EnumSet**

### Collection Operations

- View and edit elements in **Seq**, **ObjectSet**, **ObjectMap**
- **Add** new objects to any collection
- **Remove** objects from ObjectMap
- **Override** entire Seq / ObjectSet

### Specialized Selectors

| Selector | Description |
|---|---|
| Texture Region | Pick sprite regions visually |
| Weapon Region | Select weapon overlay textures |
| Sound | Browse and preview sound effects |
| Effect | Choose visual effects |
| Block Flag | Select block behavior flags |
| Content | Search any game content type |
| Block Class | Pick block Java classes |
| Unit Controller / AI / Type | Configure unit behaviors |
| Color Palette | Pick from Mindustry's palette |

### Content Editor

- **Embedded in vanilla UI** — patch entry appears directly in game content dialogs.
- **Edit content assets** — modify resource type, name, and fields of any game content.
- **Vanilla data preview** — view original content data side-by-side for reference.

### Export & Import

- Export patches to **HJSON / JSON**
- **JSON formatting** with configurable style
- **Path simplification** — condense nested paths via dot notation
- **Stack simplification** — ItemStack / LiquidStack / PayloadStack to shorthand
- **Magic Export** — convert vanilla content into editable patches

### Notes & Favorites

- **Field notes** — built-in wiki notes plus user-defined annotations
- **Remote notes** — fetch annotations from GitHub repositories
- **Star/favorite fields** — bookmark commonly used fields for quick access

### Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+Z` | Undo |
| `Ctrl+Y` / `Ctrl+Shift+Z` | Redo |
| `↑` / `↓` | Collapse / Expand node |
| `Mouse Forward` / `Mouse Back` | Next / Previous page |

---

## TODO

- [ ] Effect editing and previewer
- [ ] Patch to JSON conversion utility
