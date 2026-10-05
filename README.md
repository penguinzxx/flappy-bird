<h1 align="center">
<img src="/assets/image.png" alt="Icon" width="100" height="100">
<br>
AutoFlappy
<br>
</h1>

## NOTICE
> :warning: :warning: :warning: **WARNING:** This project is no longer maintained; there may be bugs. Feel free to fork this repository, pull requests *may* be accepted. :warning: :warning: :warning:

## What is AutoFlappy
AutoFlappy is a program that automatically plays Flappy Bird for you, with a bit of setting up, you can easily beat the World Record to most points achieved in Flappy Bird.

## How to use
1. **Install Java** (one time only): https://adoptium.net/ (click the big download button and install it).
2. **Download** this repository (green *Code* button → *Download ZIP*) and unzip it.
3. **Double-click** `Start AutoFlappy (Windows).bat` or `Start AutoFlappy (Mac).command`.
   - On Mac, if it says it can't be opened, right-click it → *Open*. Also allow your Terminal app under System Settings → Privacy & Security → *Screen Recording* and *Accessibility*.
4. Open the game so the butterfly is visible, type `setup` and press Enter, then follow the two steps (point your mouse at the top-left and bottom-right corners of the game, pressing Enter each time). It remembers this for next time.
5. Type `start` and press Enter. AutoFlappy moves the mouse onto the game and starts playing after a 3 second countdown.
6. **To stop, just move your mouse.**

### Options
- `setup` - point at the game so AutoFlappy knows where it is (redo this if you move the game window)
- `start` - start playing; move your mouse to stop
- `target` - change how high in the gap the butterfly flies (raise it if it hits the bottom flowers, lower it if it hits the top ones)
- `help` - list of options
- `quit` - quit the program

### Building from source
`javac --release 8 -d out src/autoflappy/*.java && jar cfm AutoFlappy.jar META-INF/MANIFEST.MF -C out .`

## Video
<p align="center">Making of the program: https://www.youtube.com/watch?v=-sUVFuqVBdU</p>

[![Image Link](https://img.youtube.com/vi/-sUVFuqVBdU/maxresdefault.jpg)](https://www.youtube.com/watch?v=-sUVFuqVBdU)