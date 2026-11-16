# Hypothèses à vérifier sur un vrai téléphone

Ces valeurs sont dans `Config.kt`. Elles ont été posées pour démarrer et ne sont **pas confirmées**.

| Hypothèse | Valeur actuelle | Comment vérifier |
|---|---|---|
| Nom de paquet de Wave | `com.wave.personal` | page Play Store de Wave (paramètre `id=` dans l'adresse) |
| Texte du bouton d'envoi dans Wave | `Envoyer` | ouvrir Wave avec le service activé, lire Logcat |
| Ordre des champs dans Wave | destinataire, puis montant | idem |
| Wave laisse l'accessibilité lire ses écrans | inconnu | étape 1 de la feuille de route |
| Code USSD d'achat de crédit | `#144#` (menu Orange Money, pas un achat direct) | tester avec une puce Orange ; chercher le code direct par opérateur |
| La reconnaissance vocale française marche hors ligne | dépend du téléphone | couper internet et essayer |
