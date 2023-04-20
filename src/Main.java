import java.io.*;
import java.net.URL;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final BufferedReader READER = new BufferedReader(new InputStreamReader(System.in));
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("Do you want to download images? Type y/n.");
        boolean download = parseBoolean(READER.readLine());

        if (download) {
            downloadImages();
        }

        System.out.println("Do you want to merge images? Type y/n.");
        boolean merge = parseBoolean(READER.readLine());

        if (merge) {
            mergeImages();
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

    private static void downloadImages() throws Exception {
        System.out.println("Enter left");
        int left = Integer.parseInt(READER.readLine());

        System.out.println("Enter right");
        int right = Integer.parseInt(READER.readLine());

        System.out.println("Enter top");
        int top = Integer.parseInt(READER.readLine());

        System.out.println("Enter bottom");
        int bottom = Integer.parseInt(READER.readLine());

        System.out.println("Enter folder");
        String folder = READER.readLine();

        int totalImagesCount = (Math.abs(left - right) + 1) * (Math.abs(top - bottom) + 1);
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

        for (int x = left; x <= right; x++) {
            for (int y = top; y <= bottom; y++) {
                downloadImage(urlTemplate, x, y, folder);
                downloadedImagesCount++;

                lastProgressPercentage = printProgressMessage(downloadedImagesCount, totalImagesCount, lastProgressPercentage);

                int randomTimeout = RANDOM.nextInt((downloadTimeoutMillisUpper - downloadTimeoutMillisLower) + downloadTimeoutMillisLower);
                Thread.sleep(randomTimeout);
            }
        }
    }

    private static String convertMillisToHoursAndMinutes(int millis) {
        return String.format("%02d:%02d min",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    private static void downloadImage(String urlTemplate, int x, int y, String folder) {
        String url = urlTemplate.replaceAll("\\{x}", String.valueOf(x)).replaceAll("\\{y}", String.valueOf(y));
        String fileName = String.format("%s\\%s_%s.png", folder, x, y);

        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {

            byte[] dataBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int printProgressMessage(int downloadedImagesCount, int totalImagesCount, int lastProgressPercentage) {
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

            System.out.printf("Downloading [%s][%s%%]\n", graphicalRepresentation, currentProgressPercentage);
            return currentProgressPercentage;
        }

        return lastProgressPercentage;
    }

    private static void mergeImages() {

    }
}
