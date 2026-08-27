package prove;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Verifies that repository screenshots and the demo animation are valid release media. */
public final class ProvaMedia {
    private static final int MIN_WIDTH = 640;
    private static final int MIN_HEIGHT = 360;
    private static final long MAX_SCREENSHOT_BYTES = 500_000L;
    private static final long MAX_DEMO_BYTES = 1_500_000L;

    private ProvaMedia() { }

    public static void esegui() throws Exception {
        Prove.suite("Repository media");

        String[] screenshots = {
            "docs/screenshot-vetrina.png",
            "docs/screenshot-editor.png",
            "docs/screenshot-operatore.png"
        };
        for (String path : screenshots) verifyPng(path);
        verifyGif("docs/demo.gif");
        verifyReadmeReferences(screenshots);
    }

    private static void verifyPng(String path) throws Exception {
        File file = new File(path);
        Prove.vero(path + " exists", file.isFile());
        Prove.vero(path + " is not empty", file.length() >= 2_048L);
        Prove.vero(path + " stays repository-sized", file.length() <= MAX_SCREENSHOT_BYTES);

        BufferedImage image = ImageIO.read(file);
        Prove.vero(path + " decodes as PNG", image != null);
        if (image != null) {
            Prove.vero(path + " has useful width", image.getWidth() >= MIN_WIDTH);
            Prove.vero(path + " has useful height", image.getHeight() >= MIN_HEIGHT);
        }
    }

    private static void verifyGif(String path) throws Exception {
        File file = new File(path);
        Prove.vero(path + " exists", file.isFile());
        Prove.vero(path + " is not empty", file.length() >= 4_096L);
        Prove.vero(path + " stays repository-sized", file.length() <= MAX_DEMO_BYTES);

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        Prove.vero("JDK GIF reader is available", readers.hasNext());
        if (!readers.hasNext()) return;

        ImageReader reader = readers.next();
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            reader.setInput(input, false, false);
            int frames = reader.getNumImages(true);
            Prove.vero(path + " has useful width", reader.getWidth(0) >= MIN_WIDTH);
            Prove.vero(path + " has useful height", reader.getHeight(0) >= MIN_HEIGHT);
            Prove.vero(path + " is animated", frames >= 3);
        } finally {
            reader.dispose();
        }
    }

    private static void verifyReadmeReferences(String[] screenshots) throws Exception {
        String readme = new String(Files.readAllBytes(Paths.get("README.md")), StandardCharsets.UTF_8);
        Prove.vero("README references docs/demo.gif", readme.contains("docs/demo.gif"));
        for (String screenshot : screenshots) {
            Prove.vero("README references " + screenshot, readme.contains(screenshot));
        }
    }
}
