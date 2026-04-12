# Mise à jour IA-Local-Bridge v2.0

Cette mise à jour apporte des améliorations majeures en termes de stabilité et de fonctionnalités.

## Nouvelles Fonctionnalités

1.  **Dashboard Web Intégré** :
    *   Accessible à l'adresse `http://IP_DU_TELEPHONE:8080/`.
    *   Interface de chat complète pour tester l'IA directement depuis un navigateur.
    *   Section de configuration affichant l'URL de l'API prête à l'emploi.
2.  **API Améliorée** :
    *   Port par défaut : `8080`.
    *   Support des méthodes **GET** et **POST**.
    *   Support du **CORS** pour permettre les appels depuis des applications web tierces.
3.  **Gestion de la Stabilité** :
    *   **Anti-Clavier** : Le service ferme automatiquement le clavier après avoir collé le texte, évitant le décalage des coordonnées calibrées.
    *   **Défilement Intelligent** : L'application effectue deux défilements vers le bas avant de copier pour s'assurer que le bouton de copie du dernier message est visible.
    *   **Validation du Presse-papier** : Vérification plus stricte du contenu récupéré.

## Instructions d'utilisation

1.  Lancez l'application sur le téléphone.
2.  Activez le service d'accessibilité.
3.  Démarrez le serveur via la fenêtre flottante.
4.  Ouvrez l'URL affichée dans le Toast (ex: `http://192.168.1.15:8080`) sur votre ordinateur.
5.  Utilisez l'interface de chat ou l'URL `/ask?q=votre_question` pour vos intégrations.

---
*Note : Assurez-vous que le téléphone et l'ordinateur sont sur le même réseau Wi-Fi.*
