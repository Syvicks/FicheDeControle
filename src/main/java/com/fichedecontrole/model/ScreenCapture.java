package com.fichedecontrole.model;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;

/**
 * Représente une capture d'écran associée à une catégorie.
 */
public class ScreenCapture {

    private final CaptureCategory category;
    private final BufferedImage image;
    private final LocalDateTime capturedAt;
    private final int index; // Numéro d'ordre pour les catégories multiples

    public ScreenCapture(CaptureCategory category, BufferedImage image, int index) {
        this.category = category;
        this.image = image;
        this.capturedAt = LocalDateTime.now();
        this.index = index;
    }

    public CaptureCategory getCategory() {
        return category;
    }

    public BufferedImage getImage() {
        return image;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Retourne le libellé affiché dans la liste des captures
     * Ex: "Cotisations Formulaire" ou "Test d'adhésion (2/3)"
     */
    public String getDisplayName() {
        if (category.isMultiple() && index > 0) {
            return category.getDisplayName() + " (" + index + ")";
        }
        return category.getDisplayName();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
