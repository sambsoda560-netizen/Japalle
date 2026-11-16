# Hypothèses à vérifier sur un vrai téléphone

Ces valeurs sont dans `Config.kt`. Elles ont été posées pour démarrer et ne sont **pas confirmées**.

| Hypothèse | Valeur actuelle | Comment vérifier |
|---|---|---|
| Nom de paquet de Wave | `com.wave.personal` | page Play Store de Wave (paramètre `id=` dans l'adresse) |
| Écrans de Wave pour le crédit | accueil « Crédit » → « Mon Numéro » → champ « Montant » → bouton « Acheter » (**confirmé par relevé**) | ce qui s'affiche après « Acheter » n'est pas encore connu |
| Écrans de Wave pour l'envoi | accueil « Transfert » → recherche « À » (**confirmé**) ; écran du montant **inconnu** | relevé à faire jusqu'au dernier écran avant validation |
| Ordre des champs dans Wave | destinataire, puis montant | idem |
| Wave laisse l'accessibilité lire ses écrans | **oui pour l'écran du code** (relevé du 16 nov. 2026, téléphone Transsion) ; accueil et écran « Crédit » pas encore vus | refaire un relevé en allant jusqu'à l'achat |
| Lignes du menu `#144#` pour le crédit | on cherche « crédit », puis « moi » | lancer un achat : si l'agent se trompe, l'écran de Jàppale affiche le menu qu'il a lu |
| Appli qui affiche les fenêtres USSD | `com.android.phone` | si l'agent ne remplit rien, la marque du téléphone utilise un autre nom |
| Boutons d'envoi des fenêtres USSD | Envoyer, Send, OK… | idem |
| La reconnaissance vocale française marche hors ligne | dépend du téléphone | couper internet et essayer |
