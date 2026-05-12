## EXIGENCES AJOUTÉES

### Exigence : Arrêter le suivi de l'objet sélectionné
Le système DOIT fournir un bouton permettant d'arrêter le suivi de l'objet actuellement sélectionné, ce qui efface la sélection et masque les données de suivi.

#### Scénario : Le bouton d'arrêt de suivi efface la sélection
- **LORSQUE** l'utilisateur appuie sur le bouton d'arrêt de suivi alors qu'un objet est sélectionné
- **ALORS** le système désélectionne l'objet, rétablit sa boîte englobante à la couleur normale, et masque l'affichage de la position et de la vitesse

#### Scénario : Le bouton d'arrêt de suivi n'a aucun effet lorsqu'aucun objet n'est sélectionné
- **LORSQUE** l'utilisateur appuie sur le bouton d'arrêt de suivi et qu'aucun objet n'est sélectionné
- **ALORS** le système ne fait rien (aucun changement d'état)