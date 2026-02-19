# Guide : Remplacements Conditionnels

## Vue d'ensemble

Le systeme de remplacements conditionnels permet d'inserer du texte dynamique dans le template Word selon les donnees du formulaire.

## Architecture

```
WordGeneratorXML
  ├── Remplacements simples ({{TAG}} → valeur du DTO)
  └── WordImageManager.replaceCaptureTags() (images)
```

**Fichiers concernes** :
- `WordGeneratorXML.java` (generator/) - Remplacements simples + orchestration
- `application.properties` (resources/) - Valeurs des commentaires

## Remplacements actuels

### Remplacements simples (WordGeneratorXML)

Remplaces directement depuis les champs du DTO :

| Tag | Source |
|-----|--------|
| `{{NUM_FORMULAIRE}}` | fiche.getNumFormulaire() |
| `{{CONTRAT_JURIDIQUE}}` | fiche.getContratJuridique() |
| `{{TYPE_DEMANDE}}` | fiche.getTypeDemande().getDisplayName() |
| `{{RISQUE}}` | fiche.getRisque().getDisplayName() |
| `{{NATURE_DEMANDE}}` | fiche.getNatureDemande() |
| `{{DATE_DU_JOUR}}` | LocalDate.now() |
| `{{DATE_EFFET}}` | fiche.getDateEffet() |
| `{{DISPOSITIF}}` | fiche.getDispositif() |
| `{{RAISON_SOCIAL}}` | fiche.getRaisonSocial() |
| `{{PARAMETREUR}}` | fiche.getParametreur() |
| `{{PRODUIT_GESTION}}` | fiche.getProduitGestion() |
| `{{FORMULE}}` | fiche.getFormule() |
| `{{TAUX_CHARGEMENT}}` | fiche.getTauxChargement() |
| `{{STRUCTURE}}` | fiche.getStructure() |
| `{{PC1}}`, `{{PC2}}`, `{{PC3}}` | fiche.getListePC() |

### Remplacements conditionnels

| Tag | Condition | Cle config | Valeur par defaut |
|-----|-----------|-----------|-------------------|
| `{{ACTION}}` | Nature = "Creation" | (hardcode) | "cree" |
| `{{ACTION}}` | Nature = "Modif" | (hardcode) | "modifie" |
| `{{AUCUN_PARAMETRAGE}}` | Nature contient config `nature.demande.12` | `commentaire.sans.parametrage` | "Aucun parametrage produit n'est necessaire." |
| `{{OPERATION}}` | Nature = "Creation" | `commentaire.operation.contrat` | "Les contrats peuvent etre saisis." |
| `{{OPERATION}}` | Nature = "Modif" | `commentaire.operation.avenant` | "Les avenants peuvent etre saisis." |
| `{{EQUIPE_PRESTATION}}` | Risque = FSS | `commentaire.prestation.sante` | "Merci a l'equipe parametrage prestation sante..." |
| `{{EQUIPE_PRESTATION}}` | Risque = Prev | `commentaire.prestation.prev` | "Merci a l'equipe parametrage prestation prevoyance..." |

### Tags captures d'ecran (WordImageManager)

Remplaces par du DrawingML (XML pour images inline) :

| Tag | Categorie | Multiple |
|-----|-----------|----------|
| `{{CAPTURES_COTISATIONS_FORMULAIRE}}` | Cotisations Formulaire | Non |
| `{{CAPTURES_TX_CHGT_FORMULAIRE}}` | Taux chargement Formulaire | Non |
| `{{CAPTURES_AUTRES_FORMULAIRES}}` | Autres formulaires | Oui |
| `{{CAPTURES_TX_CHGT_PLEIADE}}` | Taux chargement Pleiade | Non |
| `{{CAPTURES_COTISATIONS_PLEIADE}}` | Cotisations Pleiade | Oui |
| `{{CAPTURES_TEST_ADHESION}}` | Test d'adhesion | Oui |
| `{{CAPTURES_AUTRES_INFORMATIONS}}` | Autres informations | Oui |

Si aucune capture n'est fournie pour une categorie, le tag est simplement supprime.

## Ajouter un nouveau remplacement conditionnel

### Etape 1 : Ajouter la cle dans application.properties

```properties
commentaire.mon.nouveau.tag=Texte a inserer dans le document.
```

### Etape 2 : Ajouter la constante dans WordGeneratorXML

```java
private static final String CONFIG_MON_TAG = "commentaire.mon.nouveau.tag";
```

### Etape 3 : Ajouter la logique dans applyConditionalReplacements()

```java
// Dans applyConditionalReplacements() ou applyContextualReplacements()
if (maCondition(fiche)) {
    String valeur = getConfigValueSafely(CONFIG_MON_TAG);
    if (valeur != null) {
        replaceTextInXml(xml, "MON_TAG", valeur);
        replacementMade = true;
    }
}
```

### Etape 4 : Ajouter le tag dans le template Word

Inserer `{{MON_TAG}}` a l'endroit voulu dans `modele.docx`.

## Bonnes pratiques

1. **Constantes** : Toujours definir les cles config comme constantes `private static final String`
2. **getConfigValueSafely()** : Toujours utiliser cette methode pour lire les configs (gere null + log les erreurs)
3. **Nommage** : Tags en MAJUSCULES_AVEC_UNDERSCORES, cles config en minuscules.avec.points
4. **Logging** : Utiliser `logger.debug()` pour tracer les remplacements effectues
5. **Null-safety** : Verifier le retour de getConfigValueSafely() avant de remplacer

## Configuration

Toutes les valeurs configurables sont dans `application.properties` :

```properties
# Commentaires conditionnels
commentaire.operation.contrat=Les contrats peuvent etre saisis.
commentaire.operation.avenant=Les avenants peuvent etre saisis.
commentaire.prestation.sante=Merci a l'equipe parametrage prestation sante de saisir les prestations.
commentaire.prestation.prev=Merci a l'equipe parametrage prestation prevoyance de saisir les prestations.
commentaire.sans.parametrage=Aucun parametrage produit n'est necessaire.
```

## Gestion du template Word (.docx)

Le fichier .docx est un ZIP contenant du XML. `WordGeneratorXML` manipule directement :
- `word/document.xml` : contenu du document (texte, tags, images)
- `word/_rels/document.xml.rels` : relations (liens vers les images)
- `[Content_Types].xml` : types MIME (ajout PNG si captures)
- `word/media/` : fichiers images PNG

Les tags `{{TAG}}` peuvent etre fragmentes par Word entre plusieurs balises `<w:t>`. La methode `remplacerPatternFragmente()` gere ce cas.
