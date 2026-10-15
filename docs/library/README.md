# Travel library 0.2.0

The 310 French, simplified Chinese, and pinyin entries come from the user-supplied
`phrases-voyage-chine.xlsx`. `source-map-0.2.0.json` records its SHA-256, each original
row ID, source category, priority, and context. English, Spanish, and Italian were
translated for this release. The Chinese and pinyin remain unchanged.

Translations were reviewed by AI. They have not been certified by a native human
translator. Medical and allergy wording should receive particular attention during
future bilingual review. Source links below support vocabulary and travel contexts;
they do not attest every complete translation word for word.

## Categories

The spreadsheet's 14 categories map onto the app's existing ten categories:

| App category | Cards |
| --- | ---: |
| Transports | 60 |
| Restaurants | 60 |
| Hôtels | 30 |
| Achats | 20 |
| Itinéraires | 24 |
| Urgences | 26 |
| Communication | 25 |
| Paiements | 20 |
| Tourisme | 15 |
| Divers | 30 |

Taxis, trains/metro/buses, and airports share **Transports**. Restaurant and dietary
cards share **Restaurants**. Phone/Internet and everyday needs share **Divers**.
Other source categories map directly to their existing app equivalents.

## Editing and upgrades

`app/src/main/assets/starter-library.json` is the shipped library, with four
translation fields per card. IDs are stable. Twelve equivalent cards retain their
0.1.0 IDs to preserve favorites and history. The original 15-card asset is retained
only as an upgrade comparison baseline, not installed on fresh devices.

A database metadata marker makes the update idempotent. Missing old built-ins are
treated as deliberate deletions. Changed content and personal cards are never
overwritten. Unchanged old cards receive the reviewed wording and new translations.
The three unmatched old cards (`vegetarian`, `ticket`, `water`) remain on upgrades
and receive Spanish and Italian translations when their original content is unchanged.

## Reference pages

- [Larousse — Communiquer en chinois](https://www.larousse.fr/dictionnaires/guide/chinois/communiquer) — Politesse, incompréhension, demandes simples et communication.
- [Larousse — Organiser son séjour](https://www.larousse.fr/dictionnaires/guide/chinois/organiser) — Hébergement, transport, repas, achats et orientation.
- [Chine Informations — Restauration](https://chine.in/mandarin/guide/?cat=29) — Commandes, ingrédients, boissons et préférences alimentaires.
- [Chine Informations — Transports](https://chine.in/mandarin/guide/?cat=22) — Billets, trajets, taxis et transports collectifs.
- [Chine Informations — Santé](https://chine.in/mandarin/guide/?cat=37) — Exprimer un symptôme, demander un médecin ou signaler une allergie.
- [Chine Informations — Shopping](https://chine.in/mandarin/guide/?cat=40) — Prix, tailles, choix d'un article et achats quotidiens.
- [Wikivoyage — Mandarin Chinese phrasebook](https://en.wikivoyage.org/wiki/Mandarin_phrasebook) — Inventaire large : bases, orientation, transport, logement, nourriture, téléphone et aide.
- [Fluent in 3 Months — Chinese restaurant phrases](https://www.fluentin3months.com/chinese-restaurant-phrases/) — Commander, demander des couverts, préciser un régime ou une allergie.
- [Shanghai — Hotel vocab for your next trip](https://english.shanghai.gov.cn/en-LearnChinese/20240823/5220ec39c0824cc1b0acc154e4f64a56.html) — Réservation, arrivée, départ, caution, bagages et Wi-Fi.
- [Shanghai — How to buy train tickets in China](https://english.shanghai.gov.cn/en-LearnChinese/20240509/692d92bee2b2432d980d974722f2b2c5.html) — Vocabulaire de la billetterie, du passeport et du train à grande vitesse.
- [Du Chinese — How to Buy a Train Ticket in China](https://duchinese.net/blog/2016/02/14/17-how-to-buy-a-train-ticket-in-china/) — Formulations pour demander un billet de train.
- [Mandarin Blueprint — Booking Hotels and Accommodation](https://www.mandarinblueprint.com/blog/booking-hotels-and-accommodation-in-mandarin-essential-phrases/) — Type de chambre, lits, prix, disponibilité et services.
- [Shanghai — Ordering food in Mandarin for newbies](https://english.shanghai.gov.cn/en-LearnChinese/20240516/3410e17a9855459eaaca8abc89ab9f8f.html) — Noms d'aliments, ingrédients et boissons.
- [Shanghai — Payments for foreign nationals](https://english.shanghai.gov.cn/en-Individuals-BankingandPayments-Payments/index.html) — Identifier les situations actuelles : paiement mobile, carte étrangère, espèces et change.
- [Allergy UK — Translation cards for travel](https://www.allergyuk.org/news/translation-cards/) — Comprendre l'intérêt d'une carte d'allergie personnalisée et de traductions dédiées.
- [book2 / 50languages — Au restaurant 2](https://www.goethe-verlag.com/book2/FR/FRZH/FRZH032.HTM) — Situations de restaurant avec phrases parallèles et audio.
- [GB/T 16159-2012 — Règles d'orthographe du pinyin](https://pinyin.info/rules/GBT16159-2012.html) — Choisir et documenter une convention cohérente de notation des tons.
- [That's Mandarin — Phrases pour prendre un taxi](https://www.thatsmandarin.com/chinese-language/5-chinese-phrases-and-questions-to-take-a-taxi/) — Point de prise en charge, plaque et instructions au chauffeur.
- [Larousse — Oublier](https://www.larousse.fr/dictionnaires/francais-chinois/oublier/28088) — Vérifier la lecture là pour 落 au sens de laisser quelque chose par inadvertance.
- [Hàn Diǎn — 肚子](https://www.zdic.net/hans/肚子) — Distinguer dùzi (ventre) de dǔzi (estomac animal comme aliment).
- [Hàn Diǎn — 吐](https://www.zdic.net/hans/吐) — Vérifier tù pour vomir, distinct de tǔ pour cracher.
- [Hàn Diǎn — 空房](https://www.zdic.net/hans/空房) — Vérifier kōngfáng pour une chambre inoccupée.
