# Jison Language Support

![Build](https://github.com/SShnoodles/jison-idea-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

Provides syntax highlighting and editor features for jison and jison Schema languages.

## Features

- Syntax highlighting for keys, values, braces, directive, and comments.
- Editor assistance for line comments and brace pairing.
- Completion for `JISON_COMMENT_FLAG` directive.
- Format diagnostics for brace placement, unmatched braces, invalid `JISON_COMMENT_FLAG` values, and multi-item single-line entries.
- Support for `.jison`, `.jisonschema`, and `.jison.schema` files.

## Language Reference

The format guide used by this implementation is available in:
[JisonFormatGuide.md](https://github.com/SShnoodles/jison-idea-plugin/blob/master/JisonFormatGuide.md)

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Jison Language Support"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/SShnoodles/jison-idea-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


[template]: https://github.com/JetBrains/intellij-platform-plugin-template
