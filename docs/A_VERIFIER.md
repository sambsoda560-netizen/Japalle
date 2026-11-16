# Hypothèses à vérifier sur un vrai téléphone

Ces valeurs sont dans `Config.kt`. Elles ont été posées pour démarrer et ne sont **pas confirmées**.

| Hypothèse | Valeur actuelle | Comment vérifier |
|---|---|---|
| Nom de paquet de Wave | `com.wave.personal` | page Play Store de Wave (paramètre `id=` dans l'adresse) |
| Boutons de l'accueil de Wave | « Crédit », « Transfert » | si l'agent bloque, Jàppale affiche le bouton cherché et l'écran qu'il a vu |
| Ordre des champs dans Wave | destinataire, puis montant | idem |
| Wave laisse l'accessibilité lire ses écrans | inconnu | étape 1 de la feuille de route |
| Lignes du menu `#144#` pour le crédit | on cherche « crédit », puis « moi » | lancer un achat : si l'agent se trompe, l'écran de Jàppale affiche le menu qu'il a lu |
| Appli qui affiche les fenêtres USSD | `com.android.phone` | si l'agent ne remplit rien, la marque du téléphone utilise un autre nom |
| Boutons d'envoi des fenêtres USSD | Envoyer, Send, OK… | idem |
| La reconnaissance vocale française marche hors ligne | dépend du téléphone | couper internet et essayer |
