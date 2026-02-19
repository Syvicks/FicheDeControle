package com.fichedecontrole.ui;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fichedecontrole.model.ElementPleiade;
import com.fichedecontrole.model.FicheDto;
import com.fichedecontrole.model.NatureDemande;
import com.fichedecontrole.model.Risque;
import com.fichedecontrole.model.TypeDemande;
import com.fichedecontrole.config.ConfigManager;
import com.fichedecontrole.service.DocumentGenerationService;
import com.fichedecontrole.service.DocumentGenerationException;
import com.fichedecontrole.ui.components.ScreenCapturePanel;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Fenêtre principale de l'application de génération de fiches de contrôle Word
 */
public class FicheDeControleFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(FicheDeControleFrame.class);

    // Composants UI
    private JTextField txtNumDemande;
    private JTextField txtContratJuridique;

    // Type demande
    private JRadioButton rdoTypO2;
    private JRadioButton rdoTypEContractu;
    private ButtonGroup grpTypeDemande;

    // Risque
    private JRadioButton rdoRisqueFSS;
    private JRadioButton rdoRisquePrev;
    private ButtonGroup grpRisque;

    // Nature demande
    private JRadioButton rdoNatCreation;
    private JRadioButton rdoNatModif;
    private JRadioButton rdoNatReprisePassif;
    private JRadioButton rdoNatResiliation;
    private JRadioButton rdoNatAucun;
    private ButtonGroup grpNatureDemande;

    // Éléments (conditionnels)
    private JCheckBox chkPG;
    private JCheckBox chkPC;
    private JCheckBox chkRG;
    private JCheckBox chkFormule;
    private JCheckBox chkCotis;
    private JCheckBox chkTxChgt;
    private JCheckBox chkDispo;
    private JCheckBox chkCJ;
    private JCheckBox chkAutre;
    private JPanel pnlElements;             // conteneur affiché/masqué selon la nature
    private JPanel pnlElementsCreation;     // groupe PG / PC / RG  (séparateur "+")
    private JPanel pnlElementsModification; // groupe Formule / Cotis / ...  (séparateur " / ")

    private JFormattedTextField txtDateEffet;
    private JComboBox<String> cmbFormule;
    private DefaultListModel<String> formuleListModel;
    private JList<String> lstFormules;
    private JButton btnAjouterFormule;
    private JButton btnSupprimerFormule;
    private JTextField txtTauxChargement;
    private JComboBox<String> cmbStructure;
    private JCheckBox chkStructure2;
    private JComboBox<String> cmbStructure2;
    private JTextField txtDispositif;
    private JTextField txtRaisonSociale;
    private JComboBox<String> cmbParametreur;
    private JTextField txtPC1;
    private JTextField txtPC2;
    private JTextField txtPC3;
    private JLabel lblDateJour;
    private JButton btnGenerer;
    private ScreenCapturePanel screenCapturePanel;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DocumentGenerationService documentService;

    public FicheDeControleFrame() {
        logger.info("Initialisation de la fenetre FicheDeControle");
        this.documentService = new DocumentGenerationService();
        initUI();
        enablePopupOnAll(this);
    }
    
    /**
     * Initialisation de l'interface utilisateur
     */
    private void initUI() {
        setTitle("FicheDeControle");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 900);
        setLocationRelativeTo(null);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Panel du titre
        JPanel titlePanel = createTitlePanel();

        // Panel central : formulaire + captures
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));

        // Panel du formulaire avec scroll
        JScrollPane scrollPane = new JScrollPane(createFormPanel());
        scrollPane.setBorder(null);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel des boutons
        JPanel buttonPanel = createButtonPanel();

        // Assemblage
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }
    
    /**
     * Crée le panel de titre
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel title = new JLabel("Génération de Fiche de Contrôle", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        // Date du jour
        lblDateJour = new JLabel("Date du jour : " + LocalDate.now().format(DATE_FORMATTER));
        lblDateJour.setFont(new Font("Arial", Font.ITALIC, 11));
        lblDateJour.setHorizontalAlignment(SwingConstants.CENTER);
        
        panel.add(title, BorderLayout.NORTH);
        panel.add(lblDateJour, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Crée le panel du formulaire.
     *
     * Principe du layout :
     *  - L'outer panel est un GridBagLayout à 1 colonne (chaque "ligne" = 1 composant pleine largeur).
     *  - Les lignes pairées sont regroupées en "sections" : un panel GridLayout(1,2) contenant
     *    un demi-panneau gauche et un demi-panneau droit, chacun avec son propre GridBagLayout
     *    2 colonnes (label | champ). Tous les labels d'un même demi-panneau partagent la même
     *    largeur de colonne → les champs s'alignent verticalement au sein de chaque moitié.
     *  - GridLayout(1,2) garantit 50%/50% entre gauche et droite.
     *  - Les lignes pleine largeur utilisent fullLabeledRow (BorderLayout WEST+CENTER).
     *  - La dernière ligne (PC + captures) reçoit weighty=1 pour occuper l'espace restant.
     */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Informations du document"),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        int row = 0;

        // ══ Section 1 : N° demande / CJ, Paramétreur / Date, Type / Risque ══
        // Deux demi-panneaux partagés : les labels de la même moitié s'alignent.
        JPanel leftHalf1 = new JPanel(new GridBagLayout());
        JPanel rightHalf1 = new JPanel(new GridBagLayout());

        txtNumDemande = new JTextField(10);
        setMaxLength(txtNumDemande, 15);
        txtContratJuridique = new JTextField(10);
        setMaxLength(txtContratJuridique, 25);
        addToHalf(leftHalf1,  "N° demande :",  txtNumDemande,       0);
        addToHalf(rightHalf1, "CJ :",          txtContratJuridique, 0);

        cmbParametreur = new JComboBox<>(ConfigManager.getList("parametreur"));
        try {
            MaskFormatter dateMask = new MaskFormatter("##/##/####");
            dateMask.setPlaceholderCharacter('_');
            dateMask.setValidCharacters("0123456789");
            txtDateEffet = new JFormattedTextField(dateMask);
        } catch (ParseException ex) {
            logger.warn("Erreur lors de la création du masque de date", ex);
            txtDateEffet = new JFormattedTextField();
        }
        LocalDate today = LocalDate.now();
        LocalDate dateToSet = today.getMonthValue() < 6
            ? LocalDate.of(today.getYear(), 1, 1)
            : LocalDate.of(today.getYear(), 6, 1);
        txtDateEffet.setText(dateToSet.format(DATE_FORMATTER));
        addToHalf(leftHalf1,  "Paramétreur :",  cmbParametreur, 1);
        addToHalf(rightHalf1, "Date d'effet :", txtDateEffet,   1);

        JPanel pnlTypeDemande = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rdoTypO2 = new JRadioButton("O2", true);
        rdoTypEContractu = new JRadioButton("E-Contractu");
        grpTypeDemande = new ButtonGroup();
        grpTypeDemande.add(rdoTypO2);
        grpTypeDemande.add(rdoTypEContractu);
        pnlTypeDemande.add(rdoTypO2);
        pnlTypeDemande.add(rdoTypEContractu);

        JPanel pnlRisque = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rdoRisqueFSS = new JRadioButton("FSS", true);
        rdoRisquePrev = new JRadioButton("Prev");
        grpRisque = new ButtonGroup();
        grpRisque.add(rdoRisqueFSS);
        grpRisque.add(rdoRisquePrev);
        pnlRisque.add(rdoRisqueFSS);
        pnlRisque.add(rdoRisquePrev);
        addToHalf(leftHalf1,  "Type demande :", pnlTypeDemande, 2);
        addToHalf(rightHalf1, "Risque :",       pnlRisque,      2);

        gbc.gridy = row++;
        panel.add(halfSection(leftHalf1, rightHalf1), gbc);

        // ── Nature demande (pleine largeur) ────────────────────────────────
        JPanel pnlNatureDemande = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rdoNatCreation = new JRadioButton(NatureDemande.CREATION.getDisplayName(), true);
        rdoNatModif = new JRadioButton(NatureDemande.MODIFICATION.getDisplayName());
        rdoNatReprisePassif = new JRadioButton(NatureDemande.REPRISE_PASSIF.getDisplayName());
        rdoNatResiliation = new JRadioButton(NatureDemande.RESILIATION.getDisplayName());
        rdoNatAucun = new JRadioButton(NatureDemande.AUCUN.getDisplayName());
        grpNatureDemande = new ButtonGroup();
        grpNatureDemande.add(rdoNatCreation);
        grpNatureDemande.add(rdoNatModif);
        grpNatureDemande.add(rdoNatReprisePassif);
        grpNatureDemande.add(rdoNatResiliation);
        grpNatureDemande.add(rdoNatAucun);
        pnlNatureDemande.add(rdoNatCreation);
        pnlNatureDemande.add(rdoNatModif);
        pnlNatureDemande.add(rdoNatReprisePassif);
        pnlNatureDemande.add(rdoNatResiliation);
        pnlNatureDemande.add(rdoNatAucun);
        gbc.gridy = row++;
        panel.add(fullLabeledRow("Nature demande :", pnlNatureDemande), gbc);

        // ── Éléments Création : PG / PC / RG  (séparateur "+") ─────────────
        pnlElementsCreation = new JPanel(new GridLayout(1, 3, 15, 0));
        chkPG = new JCheckBox(ElementPleiade.PG.getLibelle());
        chkPC = new JCheckBox(ElementPleiade.PC.getLibelle());
        chkRG = new JCheckBox(ElementPleiade.RG.getLibelle());
        pnlElementsCreation.add(chkPG);
        pnlElementsCreation.add(chkPC);
        pnlElementsCreation.add(chkRG);

        // ── Éléments Modification : Formule / Cotis / Tx Chgt / Dispo / CJ / Autre  (séparateur " / ")
        pnlElementsModification = new JPanel(new GridLayout(2, 3, 15, 5));
        chkFormule = new JCheckBox(ElementPleiade.FORMULE.getLibelle());
        chkCotis   = new JCheckBox(ElementPleiade.COTIS.getLibelle());
        chkTxChgt  = new JCheckBox(ElementPleiade.TX_CHGT.getLibelle());
        chkDispo   = new JCheckBox(ElementPleiade.DISPO.getLibelle());
        chkCJ      = new JCheckBox(ElementPleiade.CJ.getLibelle());
        chkAutre   = new JCheckBox(ElementPleiade.AUTRE.getLibelle());
        pnlElementsModification.add(chkFormule);
        pnlElementsModification.add(chkCotis);
        pnlElementsModification.add(chkTxChgt);
        pnlElementsModification.add(chkDispo);
        pnlElementsModification.add(chkCJ);
        pnlElementsModification.add(chkAutre);
        pnlElementsModification.setVisible(false); // caché car Création est sélectionnée par défaut

        // Conteneur global : affiche l'un ou l'autre selon la nature
        pnlElements = new JPanel(new BorderLayout(0, 5));
        pnlElements.add(pnlElementsCreation,     BorderLayout.NORTH);
        pnlElements.add(pnlElementsModification, BorderLayout.CENTER);
        gbc.gridy = row++;
        panel.add(pnlElements, gbc);

        ActionListener naturDemandeListener = e -> {
            boolean isCreation = rdoNatCreation.isSelected();
            boolean isModif    = rdoNatModif.isSelected();
            pnlElements.setVisible(isCreation || isModif);
            pnlElementsCreation.setVisible(isCreation);
            pnlElementsModification.setVisible(isModif);
            // Décocher le groupe devenu invisible
            if (isCreation) {
                chkFormule.setSelected(false); chkCotis.setSelected(false);
                chkTxChgt.setSelected(false);  chkDispo.setSelected(false);
                chkCJ.setSelected(false);      chkAutre.setSelected(false);
            } else if (isModif) {
                chkPG.setSelected(false); chkPC.setSelected(false); chkRG.setSelected(false);
            }
            panel.revalidate();
            panel.repaint();
        };
        rdoNatCreation.addActionListener(naturDemandeListener);
        rdoNatModif.addActionListener(naturDemandeListener);
        rdoNatReprisePassif.addActionListener(naturDemandeListener);
        rdoNatResiliation.addActionListener(naturDemandeListener);
        rdoNatAucun.addActionListener(naturDemandeListener);

        // ══ Section 2 : Dispositif / Raison sociale, Formule / Taux chgt ═══
        JPanel leftHalf2 = new JPanel(new GridBagLayout());
        JPanel rightHalf2 = new JPanel(new GridBagLayout());

        txtDispositif = new JTextField();
        setMaxLength(txtDispositif, 30);
        txtRaisonSociale = new JTextField();
        setMaxLength(txtRaisonSociale, 30);
        addToHalf(leftHalf2,  "Dispositif :",    txtDispositif,  0);
        addToHalf(rightHalf2, "Raison sociale :", txtRaisonSociale, 0);

        JPanel pnlFormuleComplet = new JPanel();
        pnlFormuleComplet.setLayout(new BoxLayout(pnlFormuleComplet, BoxLayout.Y_AXIS));

        JPanel pnlFormuleInput = new JPanel(new BorderLayout(5, 0));
        pnlFormuleInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbFormule = new JComboBox<>(ConfigManager.getList("formule"));
        cmbFormule.setEditable(true);
        setMaxLength(cmbFormule, 10);
        btnAjouterFormule = new JButton("+ Ajouter");
        btnAjouterFormule.addActionListener(e -> ajouterFormule());
        pnlFormuleInput.add(cmbFormule, BorderLayout.CENTER);
        pnlFormuleInput.add(btnAjouterFormule, BorderLayout.EAST);
        pnlFormuleComplet.add(pnlFormuleInput);
        pnlFormuleComplet.add(Box.createVerticalStrut(4));

        formuleListModel = new DefaultListModel<>();
        lstFormules = new JList<>(formuleListModel);
        lstFormules.setVisibleRowCount(3);
        JScrollPane scrollFormules = new JScrollPane(lstFormules);
        scrollFormules.setPreferredSize(new Dimension(0, 55));
        scrollFormules.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlFormuleComplet.add(scrollFormules);
        pnlFormuleComplet.add(Box.createVerticalStrut(4));

        JPanel pnlBtnSupprFormule = new JPanel(new BorderLayout());
        pnlBtnSupprFormule.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSupprimerFormule = new JButton("Supprimer");
        btnSupprimerFormule.addActionListener(e -> supprimerFormule());
        pnlBtnSupprFormule.add(btnSupprimerFormule, BorderLayout.WEST);
        pnlFormuleComplet.add(pnlBtnSupprFormule);

        txtTauxChargement = new JTextField(8);
        setMaxLength(txtTauxChargement, 10);
        addToHalfTop(leftHalf2,  "Formule(s) :",      pnlFormuleComplet,  1);
        addToHalfTop(rightHalf2, "Taux chargement :", txtTauxChargement,  1);

        gbc.gridy = row++;
        panel.add(halfSection(leftHalf2, rightHalf2), gbc);

        // ── Structure (pleine largeur) ──────────────────────────────────────
        cmbStructure = new JComboBox<>(ConfigManager.getList("structure"));
        cmbStructure.setEditable(true);
        setUpperCase(cmbStructure);
        gbc.gridy = row++;
        panel.add(fullLabeledRow("Structure :", cmbStructure), gbc);

        // ── 2ème structure (checkbox + ComboBox masqué par défaut) ──────────
        chkStructure2 = new JCheckBox("2ème structure :");
        cmbStructure2 = new JComboBox<>(ConfigManager.getList("structure"));
        cmbStructure2.setEditable(true);
        setUpperCase(cmbStructure2);
        cmbStructure2.setVisible(false);
        chkStructure2.addActionListener(e -> {
            cmbStructure2.setVisible(chkStructure2.isSelected());
            panel.revalidate();
            panel.repaint();
        });
        gbc.gridy = row++;
        panel.add(fullLabeledRow(chkStructure2, cmbStructure2), gbc);

        // ── Produits Ciblés + Captures (occupe l'espace restant) ────────────
        JPanel pcPanel = new JPanel();
        pcPanel.setLayout(new BoxLayout(pcPanel, BoxLayout.Y_AXIS));

        JPanel pc1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        pc1Panel.add(new JLabel("1."));
        txtPC1 = new JTextField(30);
        setMaxLength(txtPC1, 36);
        pc1Panel.add(txtPC1);
        pcPanel.add(pc1Panel);

        JPanel pc2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        pc2Panel.add(new JLabel("2."));
        txtPC2 = new JTextField(30);
        setMaxLength(txtPC2, 36);
        pc2Panel.add(txtPC2);
        pcPanel.add(pc2Panel);

        JPanel pc3Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        pc3Panel.add(new JLabel("3."));
        txtPC3 = new JTextField(30);
        setMaxLength(txtPC3, 36);
        pc3Panel.add(txtPC3);
        pcPanel.add(pc3Panel);

        JPanel mainContentPanel = new JPanel(new BorderLayout(15, 0));

        JPanel pcLeftPanel = new JPanel(new BorderLayout());
        JPanel pcNorthContent = new JPanel();
        pcNorthContent.setLayout(new BoxLayout(pcNorthContent, BoxLayout.Y_AXIS));
        JPanel pcLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        pcLabelPanel.add(boldLabel("Produits Ciblés :"));
        pcNorthContent.add(pcLabelPanel);
        pcNorthContent.add(pcPanel);
        pcLeftPanel.add(pcNorthContent, BorderLayout.NORTH);
        mainContentPanel.add(pcLeftPanel, BorderLayout.WEST);

        screenCapturePanel = new ScreenCapturePanel();
        screenCapturePanel.setParentFrame(this);
        mainContentPanel.add(screenCapturePanel, BorderLayout.CENTER);

        gbc.gridy = row;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(mainContentPanel, gbc);

        return panel;
    }

    /**
     * Assemble deux demi-panneaux côte à côte avec une séparation stricte à 50/50.
     * GridLayout(1, 2) garantit l'égalité quelle que soit la taille des contenus.
     */
    private JPanel halfSection(JPanel left, JPanel right) {
        JPanel section = new JPanel(new GridLayout(1, 2, 15, 0));
        section.add(left);
        section.add(right);
        return section;
    }

    /**
     * Ajoute une ligne (label + champ) dans un demi-panneau partagé.
     * Tous les labels du même demi-panneau partagent la même largeur de colonne :
     * les champs s'alignent automatiquement.
     */
    private void addToHalf(JPanel half, String labelText, JComponent field, int rowIndex) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = rowIndex;

        g.gridx = 0;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(3, 0, 3, 8);
        half.add(boldLabel(labelText), g);

        g.gridx = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(3, 0, 3, 0);
        half.add(field, g);
    }

    /**
     * Variante de addToHalf avec alignement en haut de la cellule.
     * À utiliser quand le champ est plus haut que les autres (ex : panel Formule).
     */
    private void addToHalfTop(JPanel half, String labelText, JComponent field, int rowIndex) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = rowIndex;
        g.weighty = 1.0; // absorbe l'espace vertical restant → épingle les lignes précédentes en haut

        g.gridx = 0;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(3, 0, 3, 8);
        half.add(boldLabel(labelText), g);

        g.gridx = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(3, 0, 3, 0);
        half.add(field, g);
    }

    /** Ligne pleine largeur avec label en gras à gauche et composant qui remplit le reste. */
    private JPanel fullLabeledRow(String labelText, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.add(boldLabel(labelText), BorderLayout.WEST);
        row.add(comp, BorderLayout.CENTER);
        return row;
    }

    /** Variante de fullLabeledRow avec un composant Swing comme "label" (ex : JCheckBox). */
    private JPanel fullLabeledRow(JComponent label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.add(label, BorderLayout.WEST);
        row.add(comp, BorderLayout.CENTER);
        return row;
    }
    
    /**
     * Crée le panel des boutons
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        
        btnGenerer = new JButton("✅ Générer le document Word");
        btnGenerer.setFont(new Font("Arial", Font.BOLD, 13));
        btnGenerer.setPreferredSize(new Dimension(250, 40));
        btnGenerer.addActionListener(e -> genererDocument());
        
        JButton btnAnnuler = new JButton("🔄 Réinitialiser");
        btnAnnuler.setFont(new Font("Arial", Font.PLAIN, 13));
        btnAnnuler.setPreferredSize(new Dimension(150, 40));
        btnAnnuler.addActionListener(e -> resetForm());
        
        panel.add(btnGenerer);
        panel.add(btnAnnuler);
        
        return panel;
    }
    
    /**
     * Génère le document Word
     */
    private void genererDocument() {
        try {
            // Récupération des données depuis l'UI
            FicheDto fiche = collectFormData();

            // Générer le nom de fichier via le service
            String nomFichier = documentService.generateFileName(
                fiche.getNumFormulaire(),
                fiche.getContratJuridique(),
                fiche.getListePC()
            );

            // Choisir où sauvegarder
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Enregistrer la fiche de contrôle");
            fileChooser.setSelectedFile(new File(nomFichier + ".docx"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outputFile = fileChooser.getSelectedFile();

                // Générer le document via le service (inclut la validation)
                documentService.generateDocument(fiche, outputFile);

                // Afficher succès
                JOptionPane.showMessageDialog(this,
                    "Document généré avec succès !\n\nFichier : " + outputFile.getName(),
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (DocumentGenerationException ex) {
            // Erreur de validation ou de génération
            if (ex.hasValidationErrors()) {
                showValidationErrorDialog(ex.getValidationErrors());
            } else {
                logger.error("Erreur lors de la génération du document", ex);
                JOptionPane.showMessageDialog(this,
                    "Erreur lors de la génération du document :\n" + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            logger.error("Erreur inattendue lors de la génération", ex);
            JOptionPane.showMessageDialog(this,
                "Erreur inattendue :\n" + ex.getMessage(),
                "Erreur",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Collecte les données du formulaire dans un DTO
     */
    private FicheDto collectFormData() {
        String contratJuridique = txtContratJuridique.getText().trim();
        String numDemande = txtNumDemande.getText().trim();

        // Type demande (O2 ou E-Contractu)
        TypeDemande typeDemande = rdoTypO2.isSelected() ? TypeDemande.O2 : TypeDemande.E_CONTRACTU;

        // Risque (FSS ou Prev)
        Risque risque = rdoRisqueFSS.isSelected() ? Risque.FSS : Risque.PREV;

        // Nature demande
        NatureDemande natureDemande;
        if (rdoNatCreation.isSelected()) natureDemande = NatureDemande.CREATION;
        else if (rdoNatModif.isSelected()) natureDemande = NatureDemande.MODIFICATION;
        else if (rdoNatReprisePassif.isSelected()) natureDemande = NatureDemande.REPRISE_PASSIF;
        else if (rdoNatResiliation.isSelected()) natureDemande = NatureDemande.RESILIATION;
        else natureDemande = NatureDemande.AUCUN;

        // Éléments : groupe PG/PC/RG pour Création, reste pour Modification
        java.util.List<String> elements = new java.util.ArrayList<>();
        if (rdoNatCreation.isSelected()) {
            if (chkPG.isSelected()) elements.add(ElementPleiade.PG.getDisplayName());
            if (chkPC.isSelected()) elements.add(ElementPleiade.PC.getDisplayName());
            if (chkRG.isSelected()) elements.add(ElementPleiade.RG.getDisplayName());
        } else if (rdoNatModif.isSelected()) {
            if (chkFormule.isSelected()) elements.add(ElementPleiade.FORMULE.getDisplayName());
            if (chkCotis.isSelected())   elements.add(ElementPleiade.COTIS.getDisplayName());
            if (chkTxChgt.isSelected())  elements.add(ElementPleiade.TX_CHGT.getDisplayName());
            if (chkDispo.isSelected())   elements.add(ElementPleiade.DISPO.getDisplayName());
            if (chkCJ.isSelected())      elements.add(ElementPleiade.CJ.getDisplayName());
            if (chkAutre.isSelected())   elements.add(ElementPleiade.AUTRE.getDisplayName());
        }

        // Récupération de la date
        String dateEffet = txtDateEffet.getText().trim();

        String dispositif = txtDispositif.getText().trim();
        String raisonSocial = txtRaisonSociale.getText().trim();
        String parametreur = (String) cmbParametreur.getSelectedItem();
        // Formules : récupérer depuis la liste
        java.util.List<String> formules = new java.util.ArrayList<>();
        for (int i = 0; i < formuleListModel.size(); i++) {
            formules.add(formuleListModel.getElementAt(i));
        }
        if (formuleListModel.size() == 0) {
            formules.add("N/A");
        }

        String tauxChargement = txtTauxChargement.getText().trim().isEmpty() ? "N/A" : txtTauxChargement.getText().trim();

        // Structure : récupérer le texte saisi (editable ComboBox)
        String structure = cmbStructure.getEditor().getItem().toString().trim();

        // Structure 2 (si checkbox cochée)
        String structure2 = "";
        if (chkStructure2.isSelected()) {
            structure2 = cmbStructure2.getEditor().getItem().toString().trim();
        }

        // Collecter les 3 PCs (filtrer les vides)
        java.util.List<String> pcList = new java.util.ArrayList<>();
        if (!txtPC1.getText().trim().isEmpty()) {
            pcList.add(txtPC1.getText().trim());
        }
        if (!txtPC2.getText().trim().isEmpty()) {
            pcList.add(txtPC2.getText().trim());
        }
        if (!txtPC3.getText().trim().isEmpty()) {
            pcList.add(txtPC3.getText().trim());
        }
        String[] listePC = pcList.toArray(new String[0]);

        FicheDto fiche = new FicheDto(
            contratJuridique,
            numDemande,
            typeDemande,
            risque,
            natureDemande,
            elements,
            dateEffet,
            dispositif,
            raisonSocial,
            parametreur,
            formules,
            tauxChargement,
            structure,
            structure2,
            listePC
        );

        // Ajouter les captures d'écran
        fiche.setCaptures(screenCapturePanel.getCaptures());

        return fiche;
    }

    /**
     * Affiche un dialogue avec les erreurs de validation
     */
    private void showValidationErrorDialog(java.util.Map<String, String> errors) {
        StringBuilder message = new StringBuilder("Veuillez corriger les erreurs suivantes :\n\n");
        errors.forEach((field, error) -> message.append("• ").append(error).append("\n"));

        JOptionPane.showMessageDialog(this,
            message.toString(),
            "Erreurs de validation",
            JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Ajoute la formule sélectionnée/saisie à la liste
     */
    private void ajouterFormule() {
        String formule = cmbFormule.getEditor().getItem().toString().trim();
        if (!formule.isEmpty() && !formuleListModel.contains(formule)) {
            formuleListModel.addElement(formule);
            cmbFormule.getEditor().setItem("");
        }
    }

    /**
     * Supprime la formule sélectionnée de la liste
     */
    private void supprimerFormule() {
        int selectedIndex = lstFormules.getSelectedIndex();
        if (selectedIndex != -1) {
            formuleListModel.remove(selectedIndex);
        }
    }

    /**
     * Réinitialise le formulaire
     */
    private void resetForm() {
        txtContratJuridique.setText("");
        txtNumDemande.setText("");
        txtPC1.setText("");
        txtPC2.setText("");
        txtPC3.setText("");
        txtTauxChargement.setText("");
        txtDispositif.setText("");
        txtRaisonSociale.setText("");
        LocalDate today = LocalDate.now();
        LocalDate dateToSet = today.getMonthValue() < 6 
            ? LocalDate.of(today.getYear(), 1, 1) 
            : LocalDate.of(today.getYear(), 6, 1);
        txtDateEffet.setText(dateToSet.format(DATE_FORMATTER));

        // Réinitialiser les radio buttons
        rdoTypO2.setSelected(true);
        rdoRisqueFSS.setSelected(true);
        rdoNatCreation.setSelected(true);

        // Réinitialiser les checkboxes
        chkPG.setSelected(false); chkPC.setSelected(false); chkRG.setSelected(false);
        chkFormule.setSelected(false); chkCotis.setSelected(false);
        chkTxChgt.setSelected(false); chkDispo.setSelected(false);
        chkCJ.setSelected(false); chkAutre.setSelected(false);

        // Revenir à l'état initial : Création sélectionnée → groupe PG/PC/RG visible
        pnlElements.setVisible(true);
        pnlElementsCreation.setVisible(true);
        pnlElementsModification.setVisible(false);

        cmbParametreur.setSelectedIndex(0);

        // Réinitialiser les formules
        formuleListModel.clear();
        if (cmbFormule.getItemCount() > 0) {
            cmbFormule.setSelectedIndex(0);
        }

        // Réinitialiser les structures
        if (cmbStructure.getItemCount() > 0) {
            cmbStructure.setSelectedIndex(0);
        }
        chkStructure2.setSelected(false);
        cmbStructure2.setVisible(false);
        if (cmbStructure2.getItemCount() > 0) {
            cmbStructure2.setSelectedIndex(0);
        }

        // Réinitialiser les captures d'écran
        screenCapturePanel.reset();
    }

    /**
     * Permet d'ajouter un menu contextuel avec les options Couper, Copier, Coller et Tout sélectionner à un JTextComponent
     * @param textComponent le JTextComponent auquel ajouter le menu contextuel
     */
    private void addCopyPasteMenu(JTextComponent textComponent) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem cut = new JMenuItem("Couper");
        cut.addActionListener(e -> textComponent.cut());

        JMenuItem copy = new JMenuItem("Copier");
        copy.addActionListener(e -> textComponent.copy());

        JMenuItem paste = new JMenuItem("Coller");
        paste.addActionListener(e -> textComponent.paste());

        JMenuItem selectAll = new JMenuItem("Tout sélectionner");
        selectAll.addActionListener(e -> textComponent.selectAll());

        menu.add(cut);
        menu.add(copy);
        menu.add(paste);
        menu.addSeparator();
        menu.add(selectAll);

        // Activer/désactiver les items selon l'état réel au moment de l'ouverture
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                boolean hasSelection = textComponent.getSelectionStart() != textComponent.getSelectionEnd();
                boolean editable = textComponent.isEditable() && textComponent.isEnabled();
                boolean clipboardHasText = false;
                try {
                    clipboardHasText = java.awt.Toolkit.getDefaultToolkit()
                        .getSystemClipboard().isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor);
                } catch (IllegalStateException ignored) {}

                cut.setEnabled(hasSelection && editable);
                copy.setEnabled(hasSelection);
                paste.setEnabled(editable && clipboardHasText);
                selectAll.setEnabled(textComponent.getDocument().getLength() > 0);
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });

        textComponent.setComponentPopupMenu(menu);
    }

    /**
     * Permet d'ajouter le menu contextuel à tous les JTextComponent d'un conteneur
     * @param root le conteneur racine à parcourir pour trouver les JTextComponent
     */
    private void enablePopupOnAll(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof JTextComponent) {
                addCopyPasteMenu((JTextComponent) c);
            }
            if (c instanceof Container) {
                enablePopupOnAll((Container) c);
            }
        }
    }

    /**
     * Classe interne pour limiter la longueur et forcer les majuscules
     */
    private static class LengthLimitFilter extends DocumentFilter {
        private final int maxLength;

        public LengthLimitFilter(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;

            String upper = string.toUpperCase().replace("\n", "").replace("\r", "");
            if ((fb.getDocument().getLength() + upper.length()) <= maxLength) {
                super.insertString(fb, offset, upper, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) {
                super.replace(fb, offset, length, text, attrs);
                return;
            }

            String upper = text.toUpperCase().replace("\n", "").replace("\r", "");
            int currentLength = fb.getDocument().getLength();
            int newLength = currentLength - length + upper.length();

            if (newLength <= maxLength) {
                super.replace(fb, offset, length, upper, attrs);
            }
        }
    }

    /**
     * Crée un JLabel avec le texte en gras
     */
    private JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    /**
     * Applique une limite de caractères à un JTextField
     */
    private void setMaxLength(JTextField field, int maxLength) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new LengthLimitFilter(maxLength));
    }

    /**
     * Applique une limite de caractères à un JComboBox éditable
     */
    private void setMaxLength(JComboBox<String> comboBox, int maxLength) {
        Component editor = comboBox.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField textField = (JTextField) editor;
            ((AbstractDocument) textField.getDocument()).setDocumentFilter(new LengthLimitFilter(maxLength));
        }
    }

    /**
     * Applique uniquement le filtre majuscules à un JComboBox éditable
     */
    private void setUpperCase(JComboBox<String> comboBox) {
        Component editor = comboBox.getEditor().getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField textField = (JTextField) editor;
            ((AbstractDocument) textField.getDocument()).setDocumentFilter(new LengthLimitFilter(Integer.MAX_VALUE));
        }
    }
}
