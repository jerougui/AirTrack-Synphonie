## EXIGENCES AJOUTÉES

### Exigence : Afficher les données de suivi pour l'objet sélectionné
Le système DOIT afficher la position estimée (x, y, z) et la vitesse (vx, vy, vz) de l'objet sélectionné à l'écran.

#### Scénario : Les données de suivi sont visibles lorsque l'objet est sélectionné
- **LORSQUE** un objet est sélectionné par toucher
- **ALORS** le système affiche la position et la vitesse actuelles de l'objet en temps réel

#### Scénario : Les données de suivi sont mises à jour à chaque frame
- **LORSQUE** une nouvelle frame de détection est disponible et qu'un objet est sélectionné
- **ALORS** la position et la vitesse affichées sont mises à jour pour refléter les dernières estimations

#### Scénario : Aucune donnée de suivi lorsqu'aucun objet n'est sélectionné
- **LORSQUE** aucun objet n'a été sélectionné
- **ALORS** le système n'affiche aucune information de position ou de vitesse