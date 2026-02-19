package com.fichedecontrole.ui.components;

import com.fichedecontrole.model.CaptureCategory;
import com.fichedecontrole.model.ScreenCapture;
import com.fichedecontrole.service.ScreenCaptureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de gestion des captures d'écran dans le formulaire.
 * Contient un ComboBox pour choisir le type, un bouton pour capturer,
 * et une liste affichant les captures effectuées avec possibilité de supprimer.
 */
public class ScreenCapturePanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(ScreenCapturePanel.class);

    private final ScreenCaptureService captureService;
    private final List<ScreenCapture> captures;
    private final DefaultListModel<ScreenCapture> listModel;

    // Composants UI
    private JComboBox<CaptureCategory> cmbCategory;
    private JButton btnCapture;
    private JList<ScreenCapture> lstCaptures;
    private JButton btnDelete;
    private JLabel lblCount;

    // Référence vers la fenêtre parente (pour minimiser/restaurer)
    private JFrame parentFrame;

    public ScreenCapturePanel() {
        this.captureService = new ScreenCaptureService();
        this.captures = new ArrayList<>();
        this.listModel = new DefaultListModel<>();
        initUI();
    }

    public void setParentFrame(JFrame frame) {
        this.parentFrame = frame;
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Captures d'écran",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 12)
        ));

        // === Panel du haut : ComboBox + Bouton Capturer ===
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));

        topPanel.add(new JLabel("Type :"));

        cmbCategory = new JComboBox<>(CaptureCategory.values());
        cmbCategory.setPreferredSize(new Dimension(220, 28));
        topPanel.add(cmbCategory);

        btnCapture = new JButton("Capturer");
        btnCapture.setFont(new Font("Arial", Font.BOLD, 12));
        btnCapture.setPreferredSize(new Dimension(110, 28));
        btnCapture.addActionListener(e -> startCapture());
        topPanel.add(btnCapture);

        add(topPanel, BorderLayout.NORTH);

        // === Panel central : Liste des captures ===
        lstCaptures = new JList<>(listModel);
        lstCaptures.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstCaptures.setVisibleRowCount(4);
        lstCaptures.setCellRenderer(new CaptureListRenderer());

        JScrollPane scrollPane = new JScrollPane(lstCaptures);
        scrollPane.setPreferredSize(new Dimension(0, 100));
        add(scrollPane, BorderLayout.CENTER);

        // === Panel du bas : Bouton Supprimer + Compteur ===
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));

        btnDelete = new JButton("Supprimer");
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> deleteSelectedCapture());
        bottomPanel.add(btnDelete);

        lblCount = new JLabel("0 capture(s)");
        lblCount.setForeground(Color.GRAY);
        bottomPanel.add(lblCount);

        add(bottomPanel, BorderLayout.SOUTH);

        // Activer/désactiver le bouton supprimer selon la sélection
        lstCaptures.addListSelectionListener(e -> {
            btnDelete.setEnabled(!lstCaptures.isSelectionEmpty());
        });
    }

    /**
     * Lance le processus de capture d'écran
     */
    private void startCapture() {
        CaptureCategory selectedCategory = (CaptureCategory) cmbCategory.getSelectedItem();
        if (selectedCategory == null) {
            return;
        }

        // Vérifier si la catégorie est unique et déjà capturée
        if (!selectedCategory.isMultiple() && hasCaptureForCategory(selectedCategory)) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Une capture existe déjà pour \"" + selectedCategory.getDisplayName() + "\".\n" +
                "Voulez-vous la remplacer ?",
                "Capture existante",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            // Supprimer l'ancienne capture de cette catégorie
            removeCapturesForCategory(selectedCategory);
        }

        // Minimiser la fenêtre parente
        if (parentFrame != null) {
            parentFrame.setState(Frame.ICONIFIED);
        }

        // Petit délai pour laisser la fenêtre se minimiser
        Timer delayTimer = new Timer(400, e -> {
            ((Timer) e.getSource()).stop();

            captureService.captureRegion().thenAccept(image -> {
                SwingUtilities.invokeLater(() -> {
                    // Restaurer la fenêtre parente
                    if (parentFrame != null) {
                        parentFrame.setState(Frame.NORMAL);
                        parentFrame.toFront();
                    }

                    if (image != null) {
                        addCapture(selectedCategory, image);
                    }
                });
            });
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }

    /**
     * Ajoute une capture à la liste
     */
    private void addCapture(CaptureCategory category, BufferedImage image) {
        int index = countCapturesForCategory(category) + 1;
        ScreenCapture capture = new ScreenCapture(category, image, index);
        captures.add(capture);
        listModel.addElement(capture);
        updateCount();
        logger.info("Capture ajoutée : {}", capture.getDisplayName());
    }

    /**
     * Supprime la capture sélectionnée
     */
    private void deleteSelectedCapture() {
        int selectedIndex = lstCaptures.getSelectedIndex();
        if (selectedIndex >= 0) {
            ScreenCapture capture = listModel.get(selectedIndex);
            captures.remove(capture);
            listModel.remove(selectedIndex);
            updateCount();
            logger.info("Capture supprimée : {}", capture.getDisplayName());
        }
    }

    /**
     * Vérifie si une capture existe déjà pour une catégorie
     */
    private boolean hasCaptureForCategory(CaptureCategory category) {
        return captures.stream().anyMatch(c -> c.getCategory() == category);
    }

    /**
     * Supprime toutes les captures d'une catégorie
     */
    private void removeCapturesForCategory(CaptureCategory category) {
        captures.removeIf(c -> c.getCategory() == category);
        // Reconstruire le modèle de liste
        listModel.clear();
        captures.forEach(listModel::addElement);
        updateCount();
    }

    /**
     * Compte les captures pour une catégorie
     */
    private int countCapturesForCategory(CaptureCategory category) {
        return (int) captures.stream().filter(c -> c.getCategory() == category).count();
    }

    /**
     * Met à jour le compteur de captures
     */
    private void updateCount() {
        lblCount.setText(captures.size() + " capture(s)");
    }

    /**
     * Retourne la liste des captures effectuées
     */
    public List<ScreenCapture> getCaptures() {
        return new ArrayList<>(captures);
    }

    /**
     * Réinitialise toutes les captures
     */
    public void reset() {
        captures.clear();
        listModel.clear();
        updateCount();
        cmbCategory.setSelectedIndex(0);
    }

    /**
     * Renderer personnalisé pour afficher les captures dans la liste
     */
    private static class CaptureListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

            if (value instanceof ScreenCapture) {
                ScreenCapture capture = (ScreenCapture) value;
                label.setText((index + 1) + ". " + capture.getDisplayName());

                // Créer une miniature de 40x30 px
                BufferedImage thumb = createThumbnail(capture.getImage(), 40, 30);
                label.setIcon(new ImageIcon(thumb));
                label.setIconTextGap(8);
            }

            return label;
        }

        private BufferedImage createThumbnail(BufferedImage original, int maxWidth, int maxHeight) {
            double scale = Math.min(
                (double) maxWidth / original.getWidth(),
                (double) maxHeight / original.getHeight()
            );
            int w = (int) (original.getWidth() * scale);
            int h = (int) (original.getHeight() * scale);

            BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = thumb.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                               RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, w, h, null);
            g.dispose();
            return thumb;
        }
    }
}
