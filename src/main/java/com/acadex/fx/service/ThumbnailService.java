package com.acadex.fx.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class ThumbnailService {
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Map<String, Image> memoryCache = new ConcurrentHashMap<>();
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();
    private final Path thumbnailDir = Paths.get("uploads", ".thumbnails");

    public void load(Path pdfPath, java.util.function.Consumer<Image> onImage) {
        try {
            Files.createDirectories(thumbnailDir);
            String hash = FileHash.sha256(pdfPath);
            if (memoryCache.containsKey(hash)) {
                onImage.accept(memoryCache.get(hash));
                return;
            }
            Path cached = thumbnailDir.resolve(hash + ".png");
            if (Files.exists(cached)) {
                Image image = new Image(cached.toUri().toString(), true);
                memoryCache.put(hash, image);
                onImage.accept(image);
                return;
            }
            if (!inFlight.add(hash)) {
                return;
            }
            executor.submit(() -> render(pdfPath, cached, hash, onImage));
        } catch (Exception ignored) {
            // Keep placeholder visible if thumbnail generation fails.
        }
    }

    public void deleteFor(Path pdfPath) {
        try {
            String hash = FileHash.sha256(pdfPath);
            memoryCache.remove(hash);
            inFlight.remove(hash);
            Files.deleteIfExists(thumbnailDir.resolve(hash + ".png"));
        } catch (Exception ignored) {
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void render(Path pdfPath, Path cached, String hash, java.util.function.Consumer<Image> onImage) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage rendered = renderer.renderImageWithDPI(0, 120);
            javax.imageio.ImageIO.write(rendered, "png", cached.toFile());
            Image image = SwingFXUtils.toFXImage(rendered, null);
            memoryCache.put(hash, image);
            Platform.runLater(() -> onImage.accept(image));
        } catch (IOException ignored) {
        } finally {
            inFlight.remove(hash);
        }
    }
}
