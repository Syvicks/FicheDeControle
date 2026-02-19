package com.fichedecontrole;

import com.fichedecontrole.ui.FicheDeControleFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Point d'entrée principal de l'application
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Demarrage de FicheDeControle v1.0");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Impossible de definir le Look and Feel systeme", e);
        }

        SwingUtilities.invokeLater(() -> {
            FicheDeControleFrame app = new FicheDeControleFrame();
            app.setVisible(true);
            logger.info("Interface utilisateur affichee");
        });
    }
}
