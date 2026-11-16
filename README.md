# Jàppale

Une appli Android à **un seul bouton** : tu appuies, tu dis ce que tu veux, l'appli répète pour confirmer, puis elle prépare l'action à ta place (acheter du crédit, envoyer de l'argent…). Le code secret, c'est toujours l'utilisateur qui le tape.

L'objectif final est un agent qui parle **wolof**, pour les personnes peu ou pas lettrées. Cette première version fonctionne en **français**, pour tester toute la chaîne avant de brancher les modèles wolof.

## État du projet

Squelette de départ. Le code n'a **pas encore été compilé ni testé sur un téléphone**. Voir la [feuille de route](docs/ROADMAP.md).

| Fonction | État |
|---|---|
| Bouton unique + écoute en français | écrit, à tester |
| Confirmation vocale (« … c'est ça ? ») | écrit, à tester |
| Comprendre « achète du crédit » / « envoie de l'argent » | écrit (règles simples), tests unitaires fournis |
| Achat de crédit par code USSD | écrit, **code USSD à vérifier** |
| Envoi Wave par le service d'accessibilité | squelette, **écrans Wave à calibrer** |
| Wolof | plus tard |

## Obtenir l'appli sans Android Studio

À chaque envoi de code sur GitHub, l'appli est compilée automatiquement.

1. Sur GitHub, ouvrir l'onglet **Actions** et cliquer sur la dernière exécution de « Compiler l'appli ».
2. Si elle est verte : en bas de la page, télécharger **jappale-debug** (un fichier zip qui contient `app-debug.apk`).
3. Envoyer l'APK sur le téléphone, l'ouvrir, et accepter l'installation depuis une source inconnue.
4. Si elle est rouge : cliquer sur l'étape en erreur et copier le message pour le corriger.

Android limite l'activation de l'accessibilité pour les applis installées hors Play Store. Si le service est grisé dans les paramètres : `Paramètres > Applications > Jàppale > menu ⋮ > Autoriser les paramètres restreints`.

## Lancer le projet avec Android Studio

1. Installer [Android Studio](https://developer.android.com/studio).
2. Ouvrir ce dossier (`File > Open`). Android Studio télécharge Gradle et les dépendances.
3. Brancher un téléphone Android (8.0 ou plus) en mode développeur, puis `Run`.
4. Pour l'envoi Wave : activer le service dans `Paramètres > Accessibilité > Jàppale`.

Tests de la compréhension des phrases : `./gradlew test` (ou clic droit sur `RuleBasedParserTest` dans Android Studio).

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) : comment l'appli est construite et pourquoi.
- [docs/ROADMAP.md](docs/ROADMAP.md) : les étapes, dans l'ordre.
- [docs/A_VERIFIER.md](docs/A_VERIFIER.md) : les hypothèses à confirmer sur un vrai téléphone.

## Principes

1. **L'appli ne voit jamais le code secret.** Elle s'arrête avant.
2. **Rien ne part sans un « oui ».** Toute action est répétée à voix haute et confirmée.
3. **L'IA comprend, un script fixe exécute.** L'IA ne pilote jamais le téléphone librement.
