## EXIGENCES AJOUTÉES

### Exigence : Sélectionner un objet par toucher
Le système DOIT permettre à l'utilisateur de sélectionner un objet détecté en tapant sur sa boîte englobante.

#### Scénario : Toucher à l'intérieur de la boîte englobante sélectionne l'objet
- **LORSQUE** l'utilisateur tape sur un point situé à l'intérieur de la boîte englobante d'un objet détecté
- **ALORS** le système marque cet objet comme sélectionné et change la couleur de sa boîte englobante en vert

#### Scénario : Toucher à l'extérieur de la boîte englobante ne sélectionne pas
- **LORSQUE** l'utilisateur tape sur un point qui n'est pas situé à l'intérieur d'aucune boîte englobante d'objet détecté
- **ALORS** le système ne change pas la sélection et aucune boîte englobante ne devient verte