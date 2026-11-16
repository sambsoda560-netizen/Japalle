# Jàppale

Une appli Android à **un seul bouton** : tu appuies, tu dis ce que tu veux, l'appli répète pour confirmer, puis elle prépare l'action à ta place (acheter du crédit, envoyer de l'argent…). Le code secret, c'est toujours l'utilisateur qui le tape.

L'objectif final est un agent qui parle **wolof**, pour les personnes peu ou pas lettrées. Cette première version fonctionne en **français**, pour tester toute la chaîne avant de brancher les modèles wolof.

## État du projet

Squelette de départ. Le code n'a **pas encore été compilé ni testé sur un téléphone**. Voir la [feuille de route](docs/ROADMAP.md).

| Fonction | État |
|---|---|
| Bouton unique + écoute en français | écrit, à tester |
| Confirmation vocale (« … ? Dis oui, ou non. ») | écrit, à tester |
| Comprendre « achète du crédit » / « envoie de l'argent » | écrit (règles simples), tests unitaires fournis |
| Achat de crédit dans Wave, pour son propre numéro | script complet écrit d'après un relevé réel ; **à tester avec un petit montant** |
| Menus USSD `#144#` | écrit mais **désactivé** (`Config.USSD_ACTIF`) |
| Envoi d'argent dans Wave | ouvre « Transfert » et écrit le nom dans la recherche, puis guide à la voix ; **il manque le relevé de l'écran du montant** |
| Appli Orange Money | pas encore : faire le mode réglage dans l'appli |
| Compréhension par IA (Groq ou Grok), avec questions de précision | écrit, à tester avec une clé |
| Wolof | plus tard |

## Obtenir l'appli sans Android Studio

À chaque envoi de code sur GitHub, l'appli est compilée automatiquement.

1. Sur GitHub, ouvrir l'onglet **Actions** et cliquer sur la dernière exécution de « Compiler l'appli ».
2. Si elle est verte : en bas de la page, télécharger **jappale-debug** (un fichier zip qui contient `app-debug.apk`).
3. Envoyer l'APK sur le téléphone, l'ouvrir, et accepter l'installation depuis une source inconnue.
4. Si elle est rouge : cliquer sur l'étape en erreur et copier le message pour le corriger.

Android limite l'activation de l'accessibilité pour les applis installées hors Play Store. Si le service est grisé dans les paramètres : `Paramètres > Applications > Jàppale > menu ⋮ > Autoriser les paramètres restreints`.

## Cerveau (IA) : rendre l'agent plus intelligent

Sans réglage, l'agent comprend avec des mots-clés. Avec une clé d'API, il utilise un modèle de langage : phrases libres, montants en toutes lettres, et questions quand il manque une information (« Combien ? », « À qui ? »).

1. Créer une clé d'API chez le fournisseur choisi : Groq (`console.groq.com`) ou xAI pour Grok (`console.x.ai`).
2. Dans Jàppale, appuyer sur **Cerveau (IA)**, choisir le fournisseur, coller la clé, enregistrer.
3. Si le nom du modèle proposé n'existe plus chez le fournisseur, l'écran affiche l'erreur : il suffit de mettre un nom de modèle actuel.

La clé reste sur le téléphone. **Ne jamais l'écrire dans le code : ce dépôt est public.** Si l'IA ne répond pas (pas d'internet, clé invalide), l'agent retombe sur les mots-clés.

## Mode réglage : apprendre les écrans d'une appli

Pour que l'agent sache piloter Wave ou l'appli Orange Money, il faut connaître leurs boutons et leurs champs.

1. Dans Jàppale, appuyer sur **Mode réglage** (en bas de l'écran).
2. Ouvrir l'appli à régler et faire l'opération à la main (par exemple un envoi), en s'arrêtant **avant** le code secret.
3. Revenir dans Jàppale et appuyer sur **Terminer le relevé et l'envoyer**.

Le relevé est un texte qui liste les boutons `[B]` et les champs `[E]` de chaque écran. Tous les chiffres sont remplacés par `#`, mais les noms (contacts, titulaire du compte) restent visibles : le relire avant de le partager. Si le relevé est vide, l'appli protège ses écrans contre l'accessibilité.

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
