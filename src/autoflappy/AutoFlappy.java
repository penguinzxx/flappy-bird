package autoflappy;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.util.InputMismatchException;
import java.util.Scanner;

public class AutoFlappy {

    private static final String version = "3.4.7";

    private int flappyX = 487;
    private int flappyWidth = 40;

    private int pipeX = 743;
    private int checkPipeX = 400;

    private int topY = 460;
    private int bottomY = 1157;

    private int minY = 1057;

    // Butterfly body
    private int[] flappyColor = {201, 168, 242};

    // Pipe stem and flower petals; anything else (sky, clouds, sparkles, hills) is ignored
    private int[][] pipeColors = {
            {177, 228, 198},
            {245, 216, 106},
            {255, 240, 160}
    };

    private double colorTolerancePercent = 10;

    private int range = 400;
    private double targetPercent = 0.55;

    // Row (relative to topY) used to spot pipes; keep it above the score
    private static final int PIPE_ROW = 5;

    private static final Scanner sc = new Scanner(System.in);

    public void run() {
        System.out.println("    _         _        _____ _                         ");
        System.out.println("   / \\  _   _| |_ ___ |  ___| | __ _ _ __  _ __  _   _ ");
        System.out.println("  / _ \\| | | | __/ _ \\| |_  | |/ _` | '_ \\| '_ \\| | | |");
        System.out.println(" / ___ \\ |_| | || (_) |  _| | | (_| | |_) | |_) | |_| |");
        System.out.println("/_/   \\_\\__,_|\\__\\___/|_|   |_|\\__,_| .__/| .__/ \\__, |");
        System.out.println("                                    |_|   |_|    |___/");
        System.out.println("--------------------------------------------------");
        System.out.println("   ============ PROGRAM SOURCE CODE ==========");
        System.out.println("   = https://github.com/itsmarsss/AutoFlappy =");
        System.out.println("   ===========================================");
        System.out.println("      Welcome to AutoFlappy's Control Prompt");
        System.out.println();
        System.out.println("Purpose: This program was made to automatically play Flappy bird for you since you're bad at it.");
        System.out.println();
        System.out.println("Note: This program does not have a kill switch so good luck.");
        System.out.println();
        System.out.println("Warning[1]: Use this program at your own risk, I (the creator of this program) will not be liable for any issues that this program causes to your computer (or sanity?)");
        System.out.println();
        System.out.println("Version:" + versionCheck());
        System.out.println();

        try {
            commandPrompt();
        } catch (AWTException e) {
            System.out.println("Error with robot class: " + e.getMessage());
            System.exit(1);
        }


    }

    private void commandPrompt() throws AWTException {
        help();
        String input;
        while (true) {
            System.out.println();
            System.out.print("Option:");
            input = sc.next();
            sc.nextLine();
            switch (input) {
                case "help":
                    help();
                    break;
                case "setup":
                    setupAutoFlappy();
                    break;
                case "start":
                    startAutoFlappy();
                    break;
                case "quit":
                    System.out.println("ByeBye");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Unknown option; [help] for list of options.");
            }
        }
    }

    private int top = -1;
    private int bottom = -1;

    private Rectangle pipe;

    private Robot rb;

    private double target;

    private void startAutoFlappy() throws AWTException {
        rb = new Robot();
        Rectangle flappy = new Rectangle(flappyX - (flappyWidth / 2), topY, flappyWidth, bottomY - topY);
        pipe = new Rectangle(pipeX - (range / 2), topY, range, bottomY - topY);
        top = -1;
        bottom = -1;

        while (true) {
            // A pipe at checkPipeX has passed flappy, so measure the next one
            BufferedImage checkPipe = rb.createScreenCapture(new Rectangle(checkPipeX, topY + PIPE_ROW, 1, 1));
            if (top < 0 || isPipeColor(checkPipe.getRGB(0, 0))) {
                updateTopBottom(rb.createScreenCapture(pipe));
            }

            int flappyY = findFlappy(rb.createScreenCapture(flappy));
            if (flappyY < 0) {
                continue;
            }

            if (((flappyY > target) && (top > 0 && bottom > 0)) ||
                    (flappyY + topY > minY)) {
                System.out.println("Flappy at y = " + flappyY + "\t Target at y = " + target);
                clickFlappy();
            }
        }
    }

    private void clickFlappy() {
        try {
            rb.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            Thread.sleep(100);
            rb.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            System.out.println("Clicked");
        } catch (Exception e) {
            System.out.println("Error with clicking: " + e.getMessage());
        }
    }

    // Returns the highest row containing flappy's color, or -1 if flappy is not visible
    int findFlappy(BufferedImage findFlappy) {
        for (int i = 0; i < findFlappy.getHeight(); i++) {
            for (int j = 0; j < findFlappy.getWidth(); j++) {
                if (isColorWithinTolerance(findFlappy.getRGB(j, i), flappyColor)) {
                    return i;
                }
            }
        }
        return -1;
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

    void updateTopBottom(BufferedImage findPipe) {
        // Find the leftmost pipe in range and use the middle of it
        int start = -1;
        int end = -1;
        for (int i = 0; i < findPipe.getWidth(); i++) {
            if (isPipeColor(findPipe.getRGB(i, PIPE_ROW))) {
                if (start < 0) {
                    start = i;
                }
                end = i;
            } else if (start >= 0) {
                break;
            }
        }
        if (start < 0) {
            return;
        }
        int x = (start + end) / 2;

        // The gap is the longest run of non-pipe pixels with pipe both above and below it;
        // this skips small runs such as the flower centers and the score
        int gapTop = -1;
        int gapBottom = -1;
        int runStart = -1;
        boolean seenPipe = false;
        for (int i = 0; i < findPipe.getHeight(); i++) {
            if (isPipeColor(findPipe.getRGB(x, i))) {
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
        if (gapTop < 0) {
            return;
        }

        top = gapTop;
        bottom = gapBottom;
        target = (bottom + (top - bottom) * targetPercent);
        System.out.println("Top:Low bounds - " + top + ":" + bottom);
        System.out.println("Target - " + target);
    }

    private void setupAutoFlappy() {
        flappyX = readInt("Where to find flappy? (x coordinates):", 0, Integer.MAX_VALUE);
        flappyWidth = readInt("Width to search for flappy? (pixels):", 1, Integer.MAX_VALUE);
        pipeX = readInt("Where to find pipes? (x coordinates):", 0, Integer.MAX_VALUE);
        range = readInt("Range for finding pipes? (range):", 1, Integer.MAX_VALUE);
        checkPipeX = readInt("Where to find passed pipes? (x coordinates):", 0, Integer.MAX_VALUE);
        topY = readInt("Game window highest? (y coordinates):", 0, Integer.MAX_VALUE);
        bottomY = readInt("Game window lowest? (y coordinates):", topY + PIPE_ROW + 1, Integer.MAX_VALUE);
        minY = readInt("Lowest allowed flappy? (y coordinates):", 0, Integer.MAX_VALUE);
        targetPercent = readDouble("Target value, percentage from bottom to top? (percentage):", 0, 100) / 100;

        System.out.println("--------------------------------------------------");
        System.out.println("\t~ Look for Flappy at x = " + flappyX + " (width " + flappyWidth + ") from y = " + topY + " to " + bottomY);
        System.out.println("\t~ Look for Pipes at x = " + pipeX + " from y = " + topY + " to " + bottomY + " with range " + range);
        System.out.println("\t~ Look for passed Pipes at x = " + checkPipeX + " at y = " + (topY + PIPE_ROW));
        System.out.println("\t~ Keep Flappy above " + (targetPercent * 100) + "% of the distance from bottom to top of pipes.");
        System.out.println("--------------------------------------------------");

        flappyColor = readColor("Flappy color? (r g b):");

        int pipeColorCount = readInt("How many pipe colors? (stem, flower petals, etc.):", 1, Integer.MAX_VALUE);
        pipeColors = new int[pipeColorCount][];
        for (int i = 0; i < pipeColorCount; i++) {
            pipeColors[i] = readColor("Pipe color #" + (i + 1) + "? (r g b):");
        }

        colorTolerancePercent = readDouble("Color check tolerance (percentage):", 0, 100);

        System.out.println("--------------------------------------------------");
        System.out.println("\t~ Look for Flappy with color " + colorString(flappyColor));
        for (int[] color : pipeColors) {
            System.out.println("\t~ Look for Pipes with color " + colorString(color));
        }
        System.out.println("\t~ Use color check tolerance of " + colorTolerancePercent + "%");
        System.out.println("--------------------------------------------------");
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.println(prompt);
            try {
                int value = sc.nextInt();
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (InputMismatchException e) {
                sc.nextLine();
            }
            System.out.println("Please enter a whole number from " + min + " to " + max + ".");
        }
    }

    private static double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.println(prompt);
            try {
                double value = sc.nextDouble();
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (InputMismatchException e) {
                sc.nextLine();
            }
            System.out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    private static int[] readColor(String prompt) {
        System.out.println(prompt);
        return new int[]{
                readInt("  r:", 0, 255),
                readInt("  g:", 0, 255),
                readInt("  b:", 0, 255)
        };
    }

    private static String colorString(int[] color) {
        return "(" + color[0] + ", " + color[1] + ", " + color[2] + ")";
    }

    private void help() {
        String help = "help\t- this menu" +
                "\n" +
                "setup\t- setup coordinates" +
                "\n" +
                "start\t- start flappy (read warning)" +
                "\n" +
                "quit\t- quit playing AutoFlappy :(";

        System.out.println(help);
    }

    static String versionCheck() {
        String newest;
        StringBuilder note = new StringBuilder("Author's Note: ");
        try {
            URLConnection uc = URI.create("https://raw.githubusercontent.com/itsmarsss/AutoFlappy/main/newestversion").toURL().openConnection();
            uc.setConnectTimeout(3000);
            uc.setReadTimeout(3000);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(uc.getInputStream()))) {
                newest = reader.readLine();
                String line;
                while ((line = reader.readLine()) != null)
                    note.append(line).append("\n");
            }

            if (note.toString().equals("Author's Note: "))
                note = new StringBuilder();

        } catch (Exception e) {
            return "Unable to check for version and creator's note";
        }
        if (newest == null) {
            return "Unable to check for version and creator's note";
        }
        if (!newest.trim().equals(version)) {
            return "   [There is a newer version of AutoFlappy]" +
                    "\n\t##############################################" +
                    "\n\t   " + version + "(current) >> " + newest + "(newer)" +
                    "\nNew version: https://github.com/itsmarsss/AutoFlappy/releases" +
                    "\n\t##############################################" +
                    "\n" + note;
        }
        return " This program is up to date!" +
                "\n" + note;
    }

}
