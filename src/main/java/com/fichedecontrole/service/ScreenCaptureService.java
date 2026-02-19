package com.fichedecontrole.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;

/**
 * Service de capture d'écran avec sélection rectangulaire.
 * Affiche un overlay transparent plein écran et permet à l'utilisateur
 * de tracer un rectangle pour capturer une zone précise.
 */
public class ScreenCaptureService {

    private static final Logger logger = LoggerFactory.getLogger(ScreenCaptureService.class);

    /**
     * Lance une capture d'écran interactive.
     * 1. Prend un screenshot complet de l'écran
     * 2. Affiche un overlay avec le screenshot en fond (légèrement assombri)
     * 3. L'utilisateur trace un rectangle
     * 4. Retourne l'image de la zone sélectionnée
     *
     * @return CompletableFuture contenant l'image capturée, ou null si annulé (Echap)
     */
    public CompletableFuture<BufferedImage> captureRegion() {
        CompletableFuture<BufferedImage> future = new CompletableFuture<>();

        try {
            // 1. Capturer l'écran complet AVANT d'afficher l'overlay
            Robot robot = new Robot();
            Rectangle screenBounds = getFullScreenBounds();
            BufferedImage fullScreenshot = robot.createScreenCapture(screenBounds);

            // 2. Afficher l'overlay de sélection (sur l'EDT)
            SwingUtilities.invokeLater(() -> {
                SelectionOverlay overlay = new SelectionOverlay(fullScreenshot, screenBounds, future);
                overlay.setVisible(true);
            });

        } catch (AWTException e) {
            logger.error("Impossible de créer Robot pour la capture d'écran", e);
            future.complete(null);
        }

        return future;
    }

    /**
     * Calcule les dimensions totales de tous les écrans combinés
     */
    private Rectangle getFullScreenBounds() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle totalBounds = new Rectangle();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            totalBounds = totalBounds.union(gd.getDefaultConfiguration().getBounds());
        }
        return totalBounds;
    }

    /**
     * Overlay transparent plein écran pour la sélection rectangulaire
     */
    private static class SelectionOverlay extends JWindow {

        private final BufferedImage screenshot;
        private final CompletableFuture<BufferedImage> future;

        // Points de sélection
        private Point startPoint;
        private Point currentPoint;
        private boolean selecting = false;

        SelectionOverlay(BufferedImage screenshot, Rectangle bounds,
                         CompletableFuture<BufferedImage> future) {
            this.screenshot = screenshot;
            this.future = future;

            setBounds(bounds);
            setAlwaysOnTop(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            // Panel de dessin
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    drawOverlay(g);
                }
            };
            panel.setOpaque(false);
            setContentPane(panel);

            // Gestion souris
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        startPoint = e.getPoint();
                        currentPoint = e.getPoint();
                        selecting = true;
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (selecting) {
                        currentPoint = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (selecting && SwingUtilities.isLeftMouseButton(e)) {
                        currentPoint = e.getPoint();
                        selecting = false;
                        finishCapture();
                    }
                }
            };
            panel.addMouseListener(mouseHandler);
            panel.addMouseMotionListener(mouseHandler);

            // Touche Echap pour annuler
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                 .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
            panel.getActionMap().put("cancel", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    cancelCapture();
                }
            });

            // Rendre le panel focusable pour les raccourcis clavier
            panel.setFocusable(true);
            panel.requestFocusInWindow();
        }

        /**
         * Dessine l'overlay avec le screenshot assombri et le rectangle de sélection
         */
        private void drawOverlay(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;

            // Dessiner le screenshot complet
            g2d.drawImage(screenshot, 0, 0, null);

            // Assombrir tout l'écran
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Si sélection en cours, dessiner la zone claire + bordure
            if (startPoint != null && currentPoint != null) {
                Rectangle selection = getSelectionRectangle();

                // Zone sélectionnée : afficher le screenshot original (sans assombrissement)
                g2d.setClip(selection);
                g2d.drawImage(screenshot, 0, 0, null);
                g2d.setClip(null);

                // Bordure bleue
                g2d.setColor(new Color(0, 120, 215));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(selection.x, selection.y, selection.width, selection.height);

                // Afficher les dimensions
                String dimensions = selection.width + " x " + selection.height;
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.setColor(new Color(0, 120, 215));
                int textX = selection.x + 5;
                int textY = selection.y > 25 ? selection.y - 8 : selection.y + selection.height + 18;
                // Fond pour le texte
                FontMetrics fm = g2d.getFontMetrics();
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRect(textX - 2, textY - fm.getAscent() - 2,
                             fm.stringWidth(dimensions) + 4, fm.getHeight() + 4);
                g2d.setColor(new Color(0, 120, 215));
                g2d.drawString(dimensions, textX, textY);
            }

            // Instructions en haut
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String instruction = "Tracez un rectangle pour capturer - Echap pour annuler";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(instruction);
            int x = (getWidth() - textWidth) / 2;
            int y = 30;
            // Fond semi-transparent pour le texte
            g2d.setColor(new Color(0, 0, 0, 180));
            g2d.fillRoundRect(x - 15, y - fm.getAscent() - 5, textWidth + 30, fm.getHeight() + 10, 10, 10);
            g2d.setColor(Color.WHITE);
            g2d.drawString(instruction, x, y);
        }

        /**
         * Calcule le rectangle de sélection (gère les directions de tracé)
         */
        private Rectangle getSelectionRectangle() {
            int x = Math.min(startPoint.x, currentPoint.x);
            int y = Math.min(startPoint.y, currentPoint.y);
            int width = Math.abs(currentPoint.x - startPoint.x);
            int height = Math.abs(currentPoint.y - startPoint.y);
            return new Rectangle(x, y, width, height);
        }

        /**
         * Termine la capture et retourne l'image sélectionnée
         */
        private void finishCapture() {
            Rectangle selection = getSelectionRectangle();

            // Vérifier que la sélection a une taille minimale
            if (selection.width < 10 || selection.height < 10) {
                logger.warn("Sélection trop petite ({}x{}), capture annulée",
                           selection.width, selection.height);
                cancelCapture();
                return;
            }

            // Extraire la zone sélectionnée du screenshot original
            BufferedImage captured = screenshot.getSubimage(
                selection.x, selection.y, selection.width, selection.height
            );

            // Copier l'image (getSubimage partage le raster)
            BufferedImage result = new BufferedImage(
                captured.getWidth(), captured.getHeight(), BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = result.createGraphics();
            g.drawImage(captured, 0, 0, null);
            g.dispose();

            logger.info("Capture réussie : {}x{} pixels", result.getWidth(), result.getHeight());

            dispose();
            future.complete(result);
        }

        /**
         * Annule la capture
         */
        private void cancelCapture() {
            logger.info("Capture annulée par l'utilisateur");
            dispose();
            future.complete(null);
        }
    }
}
