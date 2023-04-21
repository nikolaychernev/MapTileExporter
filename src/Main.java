import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));
    private static final Random RANDOM = new Random();
    private static final Pattern PATTERN = Pattern.compile("(\\d+)_(\\d+)\\.png");
    private static final String MERGED_IMAGE_FILE_NAME = "merged.png";

    public static void main(String[] args) throws Exception {
        System.out.println("Enter folder");
        String folder = READER.readLine();

        System.out.println("Do you want to download images? Type y/n.");
        boolean download = parseBoolean(READER.readLine());

        if (download) {
            downloadImages(folder);
        }

        System.out.println("Do you want to merge images? Type y/n.");
        boolean merge = parseBoolean(READER.readLine());

        if (merge) {
            mergeImages(folder);
        }

        System.out.println("Do you want to delete the temporary images? Type y/n.");
        boolean deleteTemporary = parseBoolean(READER.readLine());

        if (deleteTemporary) {
            deleteTemporaryImages(folder);
        }
    }

    private static boolean parseBoolean(String input) throws Exception {
        switch (input.toLowerCase(Locale.ROOT)) {
            case "y":
                return true;
            case "n": {
                return false;
            }
            default:
                throw new Exception("Unknown command");
        }
    }

    private static void downloadImages(String folder) throws Exception {
        System.out.println("Enter left most X");
        int leftMostX = Integer.parseInt(READER.readLine());

        System.out.println("Enter right most X");
        int rightMostX = Integer.parseInt(READER.readLine());

        System.out.println("Enter top most Y");
        int topMostY = Integer.parseInt(READER.readLine());

        System.out.println("Enter bottom most Y");
        int bottomMostY = Integer.parseInt(READER.readLine());

        int totalImagesCount = (Math.abs(leftMostX - rightMostX) + 1) * (Math.abs(topMostY - bottomMostY) + 1);
        int downloadTimeoutMillisUpper;
        int downloadTimeoutMillisLower;

        while (true) {
            System.out.println("Enter download timeout in millis");
            int downloadTimeoutMillis = Integer.parseInt(READER.readLine());

            System.out.println("Enter download timeout randomness percentage");
            double downloadTimeoutRandomnessPercentage = Integer.parseInt(READER.readLine()) / 100d;

            if (downloadTimeoutRandomnessPercentage < 0 || downloadTimeoutRandomnessPercentage > 1) {
                System.out.println("Invalid percentage");
                continue;
            }

            downloadTimeoutMillisUpper = (int) (downloadTimeoutMillis + (downloadTimeoutMillis * downloadTimeoutRandomnessPercentage));
            downloadTimeoutMillisLower = (int) (downloadTimeoutMillis - (downloadTimeoutMillis * downloadTimeoutRandomnessPercentage));

            System.out.printf("Total added time between: %s and %s. Do you agree to continue execution? Type y/n.\n",
                    convertMillisToHoursAndMinutes(downloadTimeoutMillisLower * totalImagesCount),
                    convertMillisToHoursAndMinutes(downloadTimeoutMillisUpper * totalImagesCount));

            boolean continueExecution = parseBoolean(READER.readLine());

            if (continueExecution) {
                break;
            }
        }

        System.out.println("Enter URL template with {x} and {y} placeholders for the coordinates");
        String urlTemplate = READER.readLine();

        System.out.printf("Downloading %s images\n", totalImagesCount);

        int downloadedImagesCount = 0;
        int lastProgressPercentage = Integer.MIN_VALUE;

        for (int x = leftMostX; x <= rightMostX; x++) {
            for (int y = topMostY; y <= bottomMostY; y++) {
                String url = urlTemplate.replaceAll("\\{x}", String.valueOf(x)).replaceAll("\\{y}", String.valueOf(y));
                String fileName = String.format("%s\\%s_%s.png", folder, x, y);

                //TODO wrap in try catch and if not found error is returned try with 1 zoom level less until you find an image or the zoom level gets to 0
                //TODO dynamically change the X and Y based on the zoom level
                downloadImage(url, fileName);

                downloadedImagesCount++;
                lastProgressPercentage = printProgressMessage(downloadedImagesCount, totalImagesCount, lastProgressPercentage, "Downloading");

                int bound = (downloadTimeoutMillisUpper - downloadTimeoutMillisLower) + downloadTimeoutMillisLower;

                if (bound > 0) {
                    int randomTimeout = RANDOM.nextInt();
                    Thread.sleep(randomTimeout);
                }
            }
        }
    }

    private static String convertMillisToHoursAndMinutes(int millis) {
        return String.format("%02d:%02d min",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    private static void downloadImage(String url, String fileName) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException ignored) {
        }
    }

    private static int printProgressMessage(int downloadedImagesCount, int totalImagesCount, int lastProgressPercentage, String text) {
        int currentProgressPercentage = (downloadedImagesCount * 100) / totalImagesCount;

        if (currentProgressPercentage > lastProgressPercentage) {
            StringBuilder graphicalRepresentation = new StringBuilder();

            for (int i = 1; i <= 20; i++) {
                if (currentProgressPercentage >= i * 5) {
                    graphicalRepresentation.append("=");
                } else {
                    graphicalRepresentation.append(" ");
                }
            }

            System.out.printf("%s [%s][%s%%]\n", text, graphicalRepresentation, currentProgressPercentage);
            return currentProgressPercentage;
        }

        return lastProgressPercentage;
    }

    private static void mergeImages(String folder) throws IOException {
        File[] files = new File(folder).listFiles();

        if (files == null) {
            return;
        }

        Map<Boundary, Integer> boundaries = getBoundaries(files);

        int smallestX = boundaries.get(Boundary.SMALLEST_X);
        int biggestX = boundaries.get(Boundary.BIGGEST_X);

        int smallestY = boundaries.get(Boundary.SMALLEST_Y);
        int biggestY = boundaries.get(Boundary.BIGGEST_Y);

        int width = (biggestX - smallestX + 1) * 256;
        int height = (biggestY - smallestY + 1) * 256;

        BufferedImage mergedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = mergedImage.createGraphics();

        int traversedFilesCount = 0;
        int lastProgressPercentage = Integer.MIN_VALUE;

        for (File file : files) {
            traversedFilesCount++;
            lastProgressPercentage = printProgressMessage(traversedFilesCount, files.length, lastProgressPercentage, "Merging");

            Matcher matcher = PATTERN.matcher(file.getName());

            if (!matcher.matches()) {
                continue;
            }

            int x = Integer.parseInt(matcher.group(1)) - smallestX;
            int y = Integer.parseInt(matcher.group(2)) - smallestY;

            graphics.drawImage(ImageIO.read(file), x * 256, y * 256, null);
        }

        String fileName = String.format("%s\\%s", folder, MERGED_IMAGE_FILE_NAME);
        ImageIO.write(mergedImage, "png", new File(fileName));
    }

    private static Map<Boundary, Integer> getBoundaries(File[] files) {
        int smallestX = Integer.MAX_VALUE;
        int biggestX = Integer.MIN_VALUE;

        int smallestY = Integer.MAX_VALUE;
        int biggestY = Integer.MIN_VALUE;

        System.out.println("Calculating merged image size");

        int traversedFilesCount = 0;
        int lastProgressPercentage = Integer.MIN_VALUE;

        for (File file : files) {
            traversedFilesCount++;
            lastProgressPercentage = printProgressMessage(traversedFilesCount, files.length, lastProgressPercentage, "Calculating");

            Matcher matcher = PATTERN.matcher(file.getName());

            if (!matcher.matches()) {
                continue;
            }

            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));

            if (x < smallestX) {
                smallestX = x;
            }

            if (x > biggestX) {
                biggestX = x;
            }

            if (y < smallestY) {
                smallestY = y;
            }

            if (y > biggestY) {
                biggestY = y;
            }
        }

        Map<Boundary, Integer> boundaries = new HashMap<>();
        boundaries.put(Boundary.SMALLEST_X, smallestX);
        boundaries.put(Boundary.BIGGEST_X, biggestX);
        boundaries.put(Boundary.SMALLEST_Y, smallestY);
        boundaries.put(Boundary.BIGGEST_Y, biggestY);

        return boundaries;
    }

    private static void deleteTemporaryImages(String folder) {
        File[] files = new File(folder).listFiles();

        if (files == null) {
            return;
        }

        System.out.println("Deleting temporary images");

        int deletedImagesCount = 0;
        int lastProgressPercentage = Integer.MIN_VALUE;

        for (File file : files) {
            deletedImagesCount++;
            lastProgressPercentage = printProgressMessage(deletedImagesCount, files.length, lastProgressPercentage, "Deleting");

            if (file.getName().equals(MERGED_IMAGE_FILE_NAME)) {
                continue;
            }

            file.delete();
        }
    }

    private enum Boundary {
        SMALLEST_X, BIGGEST_X, SMALLEST_Y, BIGGEST_Y
    }
}
