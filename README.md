# FicheDeControle

Application Java Swing pour la generation automatisee de fiches de controle au format Word (.docx).

## Fonctionnalites

- Formulaire intuitif pour saisir les informations (radio buttons, checkboxes conditionnelles, listes deroulantes)
- Generation automatique de documents Word (.docx) via manipulation XML directe
- Captures d'ecran integrees : selection rectangulaire, insertion directe dans le Word
- Remplacements conditionnels dans le template Word (action, operation, equipe prestation, etc.)
- Saisie forcee en majuscules avec limites de caracteres
- Configuration externe (listes, commentaires, template) sans recompilation
- Logging automatique (SLF4J + Logback) avec rotation quotidienne
- Validation des donnees du formulaire avant generation
- Package ZIP de distribution pour deploiement facile

## Architecture

```
com.fichedecontrole/
├── Main.java                          Point d'entree
├── config/
│   └── ConfigManager.java             Chargement configuration (externe > classpath)
├── model/
│   ├── FicheDto.java                  DTO du formulaire
│   ├── TypeDemande.java               Enum : O2, E-Contractu
│   ├── Risque.java                    Enum : FSS, Prev
│   ├── CaptureCategory.java           Enum : 7 categories de captures
│   └── ScreenCapture.java             Capture d'ecran (categorie + image)
├── service/
│   ├── DocumentGenerationService.java Orchestrateur : validation + generation
│   ├── ValidationService.java         Validation des champs obligatoires
│   ├── ValidationResult.java          Resultat de validation
│   ├── DocumentGenerationException.java Exception metier
│   ├── FileNameGeneratorService.java  Nom de fichier : NUM - CJ - PCs
│   └── ScreenCaptureService.java      Capture ecran (Robot + overlay selection)
├── generator/
│   ├── WordGeneratorXML.java          Generation .docx via ZIP/XML
│   └── WordImageManager.java          Insertion images (DrawingML)
├── ui/
│   ├── FicheDeControleFrame.java      Fenetre principale Swing
│   └── components/
│       └── ScreenCapturePanel.java    Panel captures (ComboBox + liste)
└── util/
    └── DateUtils.java                 Formatage dates dd/MM/yyyy
```

## Prerequis

- **Java 11+** (teste avec Java 17)
- **Gradle 8.2+** (inclus via gradlew)

## Compilation et lancement

```bash
# Build complet (compile + tests + fatJar + ZIP)
.\gradlew.bat build

# JAR seul
.\gradlew.bat fatJar

# Lancer en dev
.\gradlew.bat run

# Lancer les tests
.\gradlew.bat test

# Package de deploiement (build/deploy/)
.\gradlew.bat deployPackage

# Nettoyer
.\gradlew.bat clean
```

En production : double-cliquer sur `Lancer.bat` ou `java -jar FicheDeControle.jar`

## Deploiement

Le build genere automatiquement un ZIP dans `build/distributions/FicheDeControle-1.0.zip` contenant :

```
FicheDeControle/
├── FicheDeControle.jar
├── Lancer.bat
├── config/
│   └── application.properties
├── templates/
│   └── modele.docx
├── logs/
└── docs/
```

Pour l'installation sur VM Citrix, voir [INSTALLATION_CITRIX.md](INSTALLATION_CITRIX.md).

## Configuration externe

Les fichiers dans `config/` et `templates/` sont charges en priorite sur ceux embarques dans le JAR. Modifiables sans recompiler.

### application.properties

Listes deroulantes (parametreurs, structures, formules) et commentaires conditionnels.

```properties
# Ajouter un parametreur
parametreur.9=NOUVEAU NOM

# Modifier un commentaire conditionnel
commentaire.operation.contrat=Les contrats peuvent etre saisis.
```

### templates/modele.docx

Modele Word avec balises `{{TAG}}` remplacees a la generation.

**Balises simples** (remplacees par les champs du formulaire) :

| Balise | Description |
|--------|-------------|
| `{{NUM_FORMULAIRE}}` | Numero de demande |
| `{{CONTRAT_JURIDIQUE}}` | Contrat juridique (CJ) |
| `{{TYPE_DEMANDE}}` | O2 ou E-Contractu |
| `{{RISQUE}}` | FSS ou Prev |
| `{{NATURE_DEMANDE}}` | Creation, Modif, etc. |
| `{{DATE_DU_JOUR}}` | Date de generation |
| `{{DATE_EFFET}}` | Date d'effet saisie |
| `{{DISPOSITIF}}` | Dispositif |
| `{{RAISON_SOCIAL}}` | Raison sociale |
| `{{PARAMETREUR}}` | Parametreur selectionne |
| `{{PRODUIT_GESTION}}` | 5 premiers caracteres du PC1 |
| `{{FORMULE}}` | Formule |
| `{{TAUX_CHARGEMENT}}` | Taux de chargement |
| `{{STRUCTURE}}` | Structure |
| `{{PC1}}`, `{{PC2}}`, `{{PC3}}` | Produits cibles |

**Balises conditionnelles**

| Balise | Condition | Valeur |
|--------|-----------|--------|
| `{{ACTION}}` | Nature = Creation | "cree" |
| `{{ACTION}}` | Nature = Modif | "modifie" |
| `{{AUCUN_PARAMETRAGE}}` | Nature contient "Aucun parametrage" | Commentaire config |
| `{{OPERATION}}` | Nature = Creation | commentaire.operation.contrat |
| `{{OPERATION}}` | Nature = Modif | commentaire.operation.avenant |
| `{{EQUIPE_PRESTATION}}` | Risque = FSS | commentaire.prestation.sante |
| `{{EQUIPE_PRESTATION}}` | Risque = Prev | commentaire.prestation.prev |

**Balises captures d'ecran** (remplacees par les images capturees) :

| Balise | Categorie |
|--------|-----------|
| `{{CAPTURES_COTISATIONS_FORMULAIRE}}` | Cotisations Formulaire |
| `{{CAPTURES_TX_CHGT_FORMULAIRE}}` | Taux chargement Formulaire |
| `{{CAPTURES_AUTRES_FORMULAIRES}}` | Autres formulaires |
| `{{CAPTURES_TX_CHGT_PLEIADE}}` | Taux chargement Pleiade |
| `{{CAPTURES_COTISATIONS_PLEIADE}}` | Cotisations Pleiade |
| `{{CAPTURES_TEST_ADHESION}}` | Test d'adhesion |
| `{{CAPTURES_AUTRES_INFORMATIONS}}` | Autres informations |

Pour plus de details sur les remplacements conditionnels : [GUIDE_REMPLACEMENTS_CONDITIONNELS.md](GUIDE_REMPLACEMENTS_CONDITIONNELS.md)

## Logs

Generes automatiquement dans `logs/app.log` :
- Rotation quotidienne, 30 jours d'historique
- Niveaux : ERROR, WARN, INFO, DEBUG

## Tests

```bash
.\gradlew.bat test
```

Tests implementes :
- ConfigManager : chargement configuration, getList, getValue
- ValidationService : validation champs obligatoires, format date, longueur PC
- DateUtils : formatage et parsing de dates

## Technologies

- **Java 11** - Langage
- **Swing** - Interface graphique
- **Logback 1.4.14** - Logging (SLF4J)
- **JUnit 5 + AssertJ** - Tests unitaires
- **Gradle 8.2** - Build

## Documentation

- [README.md](README.md) - Ce fichier (vue d'ensemble pour dev)
- [INSTALLATION_CITRIX.md](INSTALLATION_CITRIX.md) - Guide d'installation pour utilisateurs
- [GUIDE_REMPLACEMENTS_CONDITIONNELS.md](GUIDE_REMPLACEMENTS_CONDITIONNELS.md) - Maintenir les remplacements conditionnels
