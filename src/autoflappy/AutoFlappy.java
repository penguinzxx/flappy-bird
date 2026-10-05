package autoflappy;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Scanner;

public class AutoFlappy {

    private static final String SETTINGS_FILE = "autoflappy.properties";

    // Game area on screen, picked by hovering over its corners during setup
    private int gameLeft = -1;
    private int gameTop = -1;
    private int gameRight = -1;
    private int gameBottom = -1;

    // Horizontal band flappy lives in, found automatically during setup
    private int flappyLeft = -1;
    private int flappyRight = -1;

    // Butterfly body
    private final int[] flappyColor = {201, 168, 242};

    // Pipe stem and flower petals; anything else (sky, clouds, sparkles, hills) is ignored
    private final int[][] pipeColors = {
            {177, 228, 198},
            {245, 216, 106},
            {255, 240, 160}
    };

    private double colorTolerancePercent = 10;

    private double targetPercent = 0.5;

    // Positions inside the game area, as a fraction of its height/width
    private static final double PIPE_ROW = 0.03;       // above the score, where every pipe is visible
    private static final double MIN_ROW = 0.80;        // flap if flappy drops below this, whatever the pipes say
    private static final double PIPE_OVERHANG = 0.03;  // flowers stick out past the stem

    // Moving the mouse further than this from where AutoFlappy parked it stops the bot
    private static final int STOP_DISTANCE = 30;

    private static final Scanner sc = new Scanner(System.in);

    public void run() {
        System.out.println("    _         _        _____ _                         ");
        System.out.println("   / \\  _   _| |_ ___ |  ___| | __ _ _ __  _ __  _   _ ");
        System.out.println("  / _ \\| | | | __/ _ \\| |_  | |/ _` | '_ \\| '_ \\| | | |");
        System.out.println(" / ___ \\ |_| | || (_) |  _| | | (_| | |_) | |_) | |_| |");
        System.out.println("/_/   \\_\\__,_|\\__\\___/|_|   |_|\\__,_| .__/| .__/ \\__, |");
        System.out.println("                                    |_|   |_|    |___/");
        System.out.println("--------------------------------------------------");
        System.out.println("      Welcome to AutoFlappy's Control Prompt");
        System.out.println();

        loadSettings();

        try {
            commandPrompt();
        } catch (AWTException e) {
            System.out.println("Error with robot class: " + e.getMessage());
            System.exit(1);
        }
    }

    private void commandPrompt() throws AWTException {
        help();
        if (!isSetUp()) {
            System.out.println();
            System.out.println("First time? Type setup and press Enter.");
        }
        String input;
        while (true) {
            System.out.println();
            System.out.print("Option:");
            if (!sc.hasNextLine()) {
                return;
            }
            input = sc.nextLine().trim();
            switch (input) {
                case "help":
                    help();
                    break;
                case "setup":
                    setupAutoFlappy();
                    break;
                case "target":
                    setTarget();
                    break;
                case "start":
                    startAutoFlappy();
                    break;
                case "quit":
                    System.out.println("ByeBye");
                    System.exit(0);
                    break;
                case "":
                    break;
                default:
                    System.out.println("Unknown option; [help] for list of options.");
            }
        }
    }

    private boolean isSetUp() {
        return gameLeft >= 0 && flappyLeft >= 0;
    }

    private int gameWidth() {
        return gameRight - gameLeft;
    }

    private int gameHeight() {
        return gameBottom - gameTop;
    }

    private Robot rb;

    private void startAutoFlappy() throws AWTException {
        if (!isSetUp()) {
            System.out.println("Run setup first.");
            return;
        }
        rb = new Robot();

        // Park the mouse in the game so clicks land there
        rb.mouseMove(gameLeft + gameWidth() / 2, gameTop + gameHeight() / 3);
        rb.delay(200);
        Point parked = mousePosition();

        System.out.println("Starting! Move your mouse to stop.");
        for (int i = 3; i > 0; i--) {
            System.out.println(i + "...");
            rb.delay(1000);
            if (movedAway(parked)) {
                System.out.println("Stopped.");
                return;
            }
        }

        int pipeRow = (int) (gameHeight() * PIPE_ROW);
        int minRow = (int) (gameHeight() * MIN_ROW);
        int scanLeft = Math.max(gameLeft, flappyLeft - (int) (gameWidth() * PIPE_OVERHANG));
        Rectangle pipeRowArea = new Rectangle(scanLeft, gameTop + pipeRow, gameRight - scanLeft, 1);
        Rectangle flappyArea = new Rectangle(flappyLeft, gameTop, flappyRight - flappyLeft, gameHeight());

        int[] lastGap = null;

        // Flap once so the round starts
        clickFlappy();

        while (true) {
            if (movedAway(parked)) {
                System.out.println("Mouse moved - stopped.");
                return;
            }

            // Aim for the gap in the nearest pipe that hasn't fully passed flappy yet
            double target = gameHeight() * 0.5;
            int pipeX = findPipeColumn(rb.createScreenCapture(pipeRowArea));
            if (pipeX >= 0) {
                int[] gap = findGap(rb.createScreenCapture(new Rectangle(scanLeft + pipeX, gameTop, 1, gameHeight())));
                if (gap != null) {
                    target = gap[1] + (gap[0] - gap[1]) * targetPercent;
                    if (lastGap == null || Math.abs(gap[0] - lastGap[0]) > 5) {
                        System.out.println("Next gap: " + gap[0] + " to " + gap[1] + ", aiming for " + (int) target);
                    }
                    lastGap = gap;
                }
            }

            int flappyY = findFlappy(rb.createScreenCapture(flappyArea));
            if (flappyY < 0) {
                continue;
            }

            if (flappyY > target || flappyY > minRow) {
                clickFlappy();
            }
        }
    }

    private static Point mousePosition() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info == null ? null : info.getLocation();
    }

    private static boolean movedAway(Point parked) {
        Point now = mousePosition();
        return parked != null && now != null && now.distance(parked) > STOP_DISTANCE;
    }

    private void clickFlappy() {
        rb.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        rb.delay(100);
        rb.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    // Returns the highest row containing flappy's color, or -1 if flappy is not visible
    int findFlappy(BufferedImage findFlappy) {
        for (int i = 0; i < findFlappy.getHeight() - 1; i++) {
            for (int j = 0; j < findFlappy.getWidth() - 1; j++) {
                if (isFlappyAt(findFlappy, j, i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    // Returns the middle of the leftmost pipe in a one-pixel-tall strip, or -1 if there is none
    int findPipeColumn(BufferedImage row) {
        int start = -1;
        int end = -1;
        for (int i = 0; i < row.getWidth(); i++) {
            if (isPipeColor(row.getRGB(i, 0))) {
                if (start < 0) {
                    start = i;
                }
                end = i;
            } else if (start >= 0) {
                break;
            }
        }
        return start < 0 ? -1 : (start + end) / 2;
    }

    // Returns {top, bottom} of the gap in a one-pixel-wide column through a pipe, or null.
    // The gap is the longest run of non-pipe pixels with pipe both above and below it;
    // this skips small runs such as the flower centers and the score
    int[] findGap(BufferedImage column) {
        int gapTop = -1;
        int gapBottom = -1;
        int runStart = -1;
        boolean seenPipe = false;
        for (int i = 0; i < column.getHeight(); i++) {
            if (isPipeColor(column.getRGB(0, i))) {
                if (runStart >= 0 && seenPipe && i - runStart > gapBottom - gapTop) {
                    gapTop = runStart;
                    gapBottom = i;
                }
                seenPipe = true;
                runStart = -1;
            } else if (runStart < 0) {
                runStart = i;
            }
        }
        return gapTop < 0 ? null : new int[]{gapTop, gapBottom};
    }

    // Returns {left, right} of flappy's color in the image, or null if flappy is not visible
    int[] findFlappyBand(BufferedImage game) {
        int left = -1;
        int right = -1;
        for (int x = 0; x < game.getWidth() - 1; x++) {
            for (int y = 0; y < game.getHeight() - 1; y++) {
                if (isFlappyAt(game, x, y)) {
                    if (left < 0) {
                        left = x;
                    }
                    right = x;
                    break;
                }
            }
        }
        return left < 0 ? null : new int[]{left, right};
    }

    // Flappy's body is a solid patch, so require a 2x2 block; this ignores the thin
    // lavender-ish fringe where pipe outlines blend into the sky
    private boolean isFlappyAt(BufferedImage image, int x, int y) {
        return isColorWithinTolerance(image.getRGB(x, y), flappyColor) &&
                isColorWithinTolerance(image.getRGB(x + 1, y), flappyColor) &&
                isColorWithinTolerance(image.getRGB(x, y + 1), flappyColor) &&
                isColorWithinTolerance(image.getRGB(x + 1, y + 1), flappyColor);
    }

    private boolean isPipeColor(int rgb) {
        for (int[] color : pipeColors) {
            if (isColorWithinTolerance(rgb, color)) {
                return true;
            }
        }
        return false;
    }

    private boolean isColorWithinTolerance(int rgb, int[] color) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = (rgb) & 0xFF;
        double tolerance = 255 * (colorTolerancePercent / 100);

        return Math.abs(r - color[0]) <= tolerance &&
                Math.abs(g - color[1]) <= tolerance &&
                Math.abs(b - color[2]) <= tolerance;
    }

    private void setupAutoFlappy() throws AWTException {
        System.out.println("Open the game so the butterfly is visible, and don't move the game window afterwards.");
        System.out.println();
        System.out.println("1) Put your mouse on the TOP-LEFT corner of the game (just inside the frame), then press Enter here.");
        sc.nextLine();
        Point topLeft = mousePosition();
        System.out.println("2) Put your mouse on the BOTTOM-RIGHT corner of the game (just inside the frame), then press Enter here.");
        sc.nextLine();
        Point bottomRight = mousePosition();

        if (topLeft == null || bottomRight == null) {
            System.out.println("Couldn't read the mouse position. Try again.");
            return;
        }
        if (bottomRight.x - topLeft.x < 50 || bottomRight.y - topLeft.y < 50) {
            System.out.println("Those corners don't look right (the second one should be below and to the right of the first). Try again.");
            return;
        }

        BufferedImage game = new Robot().createScreenCapture(
                new Rectangle(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y));
        int[] band = findFlappyBand(game);
        if (band == null) {
            System.out.println("Couldn't find the butterfly inside those corners. Make sure the game is visible and not covered, then try again.");
            return;
        }

        gameLeft = topLeft.x;
        gameTop = topLeft.y;
        gameRight = bottomRight.x;
        gameBottom = bottomRight.y;
        // A little extra room so a tilting butterfly stays in view
        flappyLeft = gameLeft + Math.max(0, band[0] - 5);
        flappyRight = gameLeft + Math.min(game.getWidth(), band[1] + 6);
        saveSettings();

        System.out.println("--------------------------------------------------");
        System.out.println("\t~ Game area: (" + gameLeft + ", " + gameTop + ") to (" + gameRight + ", " + gameBottom + ")");
        System.out.println("\t~ Butterfly found between x = " + flappyLeft + " and " + flappyRight);
        System.out.println("--------------------------------------------------");
        System.out.println("All set! Type start to play.");
    }

    private void setTarget() {
        System.out.println("Where in the gap should the butterfly fly? 0 = bottom edge, 100 = top edge (now " + Math.round(targetPercent * 100) + "):");
        System.out.println("Tip: go higher if it hits the bottom flowers, lower if it hits the top ones.");
        String line = sc.nextLine().trim();
        try {
            double value = Double.parseDouble(line);
            if (value < 0 || value > 100) {
                throw new NumberFormatException();
            }
            targetPercent = value / 100;
            saveSettings();
            System.out.println("Target set to " + Math.round(value) + ".");
        } catch (NumberFormatException e) {
            System.out.println("Please enter a number from 0 to 100.");
        }
    }

    private void loadSettings() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(SETTINGS_FILE)) {
            props.load(in);
            gameLeft = Integer.parseInt(props.getProperty("gameLeft"));
            gameTop = Integer.parseInt(props.getProperty("gameTop"));
            gameRight = Integer.parseInt(props.getProperty("gameRight"));
            gameBottom = Integer.parseInt(props.getProperty("gameBottom"));
            flappyLeft = Integer.parseInt(props.getProperty("flappyLeft"));
            flappyRight = Integer.parseInt(props.getProperty("flappyRight"));
            targetPercent = Double.parseDouble(props.getProperty("targetPercent"));
            System.out.println("Loaded your setup from last time (type setup to redo it).");
        } catch (Exception e) {
            gameLeft = -1;
            flappyLeft = -1;
        }
    }

    private void saveSettings() {
        Properties props = new Properties();
        props.setProperty("gameLeft", String.valueOf(gameLeft));
        props.setProperty("gameTop", String.valueOf(gameTop));
        props.setProperty("gameRight", String.valueOf(gameRight));
        props.setProperty("gameBottom", String.valueOf(gameBottom));
        props.setProperty("flappyLeft", String.valueOf(flappyLeft));
        props.setProperty("flappyRight", String.valueOf(flappyRight));
        props.setProperty("targetPercent", String.valueOf(targetPercent));
        try (OutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "AutoFlappy setup");
        } catch (Exception e) {
            System.out.println("Couldn't save your setup (" + e.getMessage() + "); you'll need to redo it next time.");
        }
    }

    private void help() {
        String help = "setup\t- point at the game so AutoFlappy knows where it is" +
                "\n" +
                "start\t- start playing (move your mouse to stop)" +
                "\n" +
                "target\t- change how high in the gap the butterfly flies" +
                "\n" +
                "help\t- this menu" +
                "\n" +
                "quit\t- quit playing AutoFlappy :(";

        System.out.println(help);
    }

}
