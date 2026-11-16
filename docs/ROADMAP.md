# Feuille de route

## Étape 0 — Faire tourner le squelette
- [ ] Ouvrir le projet dans Android Studio, corriger les erreurs de compilation éventuelles
- [ ] Lancer sur un vrai téléphone
- [ ] Vérifier : bouton → écoute → confirmation → « oui » / « non »

## Étape 1 — Le test qui décide de tout : Wave et l'accessibilité
- [ ] Activer le service d'accessibilité
- [ ] Noter ce que le service voit sur l'écran d'envoi de Wave (journal `Jappale` dans Logcat)
- [ ] Si Wave est lisible : calibrer les textes des boutons dans `Config.kt`
- [ ] Si Wave est protégé : passer au plan B (ouvrir Wave et guider à la voix)

## Étape 2 — Achat de crédit à la voix (première vraie fonction)
- [ ] Vérifier les codes USSD par opérateur (Orange, Free/Yas, Expresso)
- [ ] Tester sur 5 à 10 personnes réelles

## Étape 3 — Envoi d'argent
- [ ] Retrouver un numéro à partir d'un prénom (contacts du téléphone)
- [ ] Confirmation avec la photo du contact
- [ ] Orange Money par USSD

## Étape 4 — Meilleure compréhension
- [ ] Remplacer les règles par un LLM qui renvoie une `Intention`
- [ ] Gérer les demandes incomplètes (« envoie de l'argent » → « combien ? à qui ? »)

## Étape 5 — Wolof
- [ ] Comparer les modèles de reconnaissance wolof sur de vraies voix
- [ ] Brancher la reconnaissance et la voix wolof derrière les interfaces existantes
- [ ] Phrases de confirmation enregistrées par une vraie personne

## Étape 6 — Publication
- [ ] Écran d'explication et de consentement pour l'accessibilité
- [ ] Formulaire de déclaration Google Play
- [ ] Politique de confidentialité
