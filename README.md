# Hexacraft
[![Build Status](https://travis-ci.org/Martomate/Hexacraft.svg?branch=master)](https://travis-ci.org/Martomate/Hexacraft)
[![Coverage Status](https://coveralls.io/repos/github/Martomate/Hexacraft/badge.svg?branch=master)](https://coveralls.io/github/Martomate/Hexacraft?branch=master)

A game with hexagonal blocks on a cylindrical world inspired by Minecraft

Official website: http://www.martomate.com/games/Hexacraft

![image](http://www.martomate.com/games/Hexacraft/bigPicture_0.8.png)

## Features:

- Hexagonal blocks
- Cylindrical world
- Infinite world (except around the cylinder)
- And much more!

## Installation

The game requires Java 8 (or later) to be installed. If it's not installed you can get it [here](https://java.com).

### Version 0.7 and earlier
Up to version 0.7 the game was shipped as a couple of folders and a jar file.
To get started, just run `Hexacraft.jar` (or Hexagon.jar in early versions) either by **double-clicking** it or using the terminal.
In the terminal you would run `java -jar Hexacraft.jar`

### Version 0.8
In this version you can run the game in any of the following ways.
- Run `bin/hexacraft.bat` (Windows)
- Run `bin/hexacraft` (Mac/Linux)
- Double click `lib/com.martomate.hexacraft-0.8-launcher.jar` (like in the previous version)

## Usage

### Controls
The controls are almost the same as in Minecraft.

#### Movement:
Action          | Keys
--------------- | -------------------------------
Movement        | WASD
Camera rotation | Mouse or arrow keys, PgUp, PgDn
Jump            | Space
Fly             | F
Fly up          | Space
Fly down        | Left Shift
Walk slow       | Left Ctrl
Walk fast       | Left Alt
Walk superfast  | Right Ctrl

#### Other useful controls
Action             | Keys
------------------ | ---------------------
Select hotbar slot | 1 ... 9, scroll wheel
Place block        | Right click
Remove block       | Left click
Pause              | Escape
Fullscreen         | F11

#### Extra
Action                      | Keys
--------------------------- | ---------------------
Free the mouse              | M
Debug info                  | F7
Place block under your feet | B

### Worlds are saved
The program works very similarly to Minecraft (at least when it comes to saving worlds).

You can create as many worlds as you like and they will be saved on your computer. To find the files go to the folder named ".hexacraft" in the %appdata% directory on Windows and in the home directory on Mac and Linux (just like for Minecraft).

## Contributing

### The GitHub repository

To contribute you need to fork the develop-branch of the project. To make changes I would recommend that you make many small commits rather than one big at the end, since it will make it easy to see which commit introduced what (assuming that you provide good commit-messages). When you are done with a feature you can create a pull-request. Please note that you can still add commits to an active pull-request, which might be needed to fix merge conflicts.

### Building the project

This project uses [sbt](https://www.scala-sbt.org/1.x/docs/index.html) as the build tool. The project can be directly imported into [IntelliJ](https://www.jetbrains.com/idea/) since it supports sbt, but the project itself does not in any way depend on any tools other than sbt.

### Tests

At the moment this project hardly contains any tests (unit tests), but that will hopefully improve as time goes on. That being said, there is support for unit tests so you are welcome to write tests for your code. That will make sure that the code you write do what it was designed to do not only now, but also in the future.

## Credits

The game was created in spring of 2015 by **Martin Jakobsson** ([Martomate](https://github.com/Martomate)).

Almost three years later the game moved to GitHub and since then anyone has had the chance to contribute. Every contributor will be listed in this section.

## Licence

This project goes under the [MIT Licence](LICENSE) which you can read more about if you click right above the clone-button at the top of the page. In short it says that anyone may use this project for anything they want as long as the licence is not removed or changed.

Basically this should be an open source project, but if anyone uses it for some purpose it would be nice if they mentioned the original project or the authors of this project. There has, after all, been a lot of work put into this.
