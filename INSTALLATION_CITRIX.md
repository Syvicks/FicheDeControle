# Guide d'Installation sur VM Citrix

## Prerequis

- **Java 11 ou superieur** installe sur la VM Citrix
- **Droits d'ecriture** dans le dossier d'installation
- **Environ 20 MB** d'espace disque disponible

## Installation

### 1. Verifier Java

Ouvrez une invite de commande et tapez :
```batch
java -version
```

Vous devez voir une version **11 ou superieure**. Si Java n'est pas installe, contactez votre support IT.

### 2. Copier les fichiers

Decompressez le ZIP `FicheDeControle-1.0.zip` sur votre VM Citrix, par exemple :
```
C:\Applications\FicheDeControle\
```

### 3. Structure des fichiers

Apres extraction :
```
FicheDeControle/
├── FicheDeControle.jar          Application principale
├── Lancer.bat                   Double-cliquer pour lancer
├── config/
│   └── application.properties   Configuration (modifiable)
├── templates/
│   └── modele.docx              Modele Word (modifiable)
├── logs/                        Logs automatiques
└── docs/                        Documentation
```

## Lancement

**Double-cliquez sur `Lancer.bat`**

L'application demarre en quelques secondes.

## Utilisation

### Processus standard

1. Remplir le formulaire (N demande, CJ, parametreur, etc.)
2. Selectionner le type de demande, le risque et la nature
3. Optionnel : capturer des ecrans (bouton "Capturer" dans la section captures)
4. Cliquer sur "Generer le document Word"
5. Choisir l'emplacement de sauvegarde

### Champs obligatoires

- Contrat Juridique (CJ)
- Numero Formulaire/Demande
- Date d'effet
- Au moins un code PC

### Captures d'ecran

L'application permet d'inserer des captures directement dans le document Word :

1. Choisir le type de capture dans la liste deroulante
2. Cliquer sur "Capturer"
3. L'application se minimise et un overlay apparait
4. Tracer un rectangle sur la zone a capturer
5. L'image est ajoutee a la liste
6. Repeter pour d'autres captures si necessaire

Pour supprimer une capture : la selectionner dans la liste et cliquer "Supprimer".

## Configuration

### Modifier les listes deroulantes

1. Ouvrez `config\application.properties` avec Notepad
2. Modifiez les valeurs selon vos besoins
3. Sauvegardez et relancez l'application

Exemple - ajouter un parametreur :
```properties
parametreur.9=NOUVEAU NOM
```

Les listes sont numerotees a partir de 0. Utilisez le prochain index disponible.

### Modifier le modele Word

1. Remplacez `templates\modele.docx` par votre nouveau modele
2. Conservez les balises `{{TAG}}` aux endroits voulus
3. Relancez l'application

Balises disponibles :
- `{{NUM_FORMULAIRE}}`, `{{CONTRAT_JURIDIQUE}}`, `{{TYPE_DEMANDE}}`, `{{RISQUE}}`
- `{{NATURE_DEMANDE}}`, `{{DATE_DU_JOUR}}`, `{{DATE_EFFET}}`
- `{{DISPOSITIF}}`, `{{RAISON_SOCIAL}}`, `{{PARAMETREUR}}`
- `{{PRODUIT_GESTION}}`, `{{FORMULE}}`, `{{TAUX_CHARGEMENT}}`, `{{STRUCTURE}}`
- `{{PC1}}`, `{{PC2}}`, `{{PC3}}`
- `{{ACTION}}`, `{{OPERATION}}`, `{{EQUIPE_PRESTATION}}`, `{{AUCUN_PARAMETRAGE}}`
- `{{CAPTURES_COTISATIONS_FORMULAIRE}}`, `{{CAPTURES_TX_CHGT_FORMULAIRE}}`, etc.

## Resolution de Problemes

### Erreur "Java non installe"

Java n'est pas dans le PATH. Contactez votre support IT pour installer Java 11+.

### Erreur "JAR introuvable"

Le fichier `FicheDeControle.jar` est manquant. Verifiez que tous les fichiers ont ete copies.

### L'application ne demarre pas (memoire)

Ouvrez `Lancer.bat` avec Notepad et reduisez la memoire :
```batch
set JVM_OPTS=-Xmx256m -Xms128m
```

### Erreur lors de la generation

1. Verifiez que `templates\modele.docx` existe
2. Verifiez que les champs obligatoires sont remplis
3. Consultez `logs\app.log` pour l'erreur exacte

## Logs

Les logs sont dans `logs\app.log` (rotation quotidienne, 30 jours).

Ouvrez avec Notepad et cherchez les lignes `ERROR` ou `WARN`.

## Mises a Jour

1. Sauvegardez `config\application.properties` et `templates\modele.docx`
2. Remplacez `FicheDeControle.jar` et `Lancer.bat`
3. Restaurez vos fichiers de configuration
4. Relancez l'application

## Notes

- L'application fonctionne hors ligne
- Les donnees ne sont jamais envoyees a un serveur externe
- Les documents sont generes localement
- La saisie est automatiquement convertie en majuscules
