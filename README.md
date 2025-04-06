# Client_WizardSE2
![Wizard](https://github.com/user-attachments/assets/13f095bc-f1ce-4d9b-a463-4eb63f7e7d66)


Grundregeln: 3-6 Spieler
Standard 52 Karten, 4 Farben, Zahlen von 1 bis 13.
Zusatzkarten: 4 Wizards (Zauberer), 4 Jester (Narren). Insgesamt 60  Karten.
Wizard ist die höchste Karte, danach sind die 4 Farben mit Zahlen Werten absteigend, Jester ist am schlechtesten.
3 Spieler: 20 Runden. 4 Spieler: 15 Runden. 5 Spieler: 12 Runden. 6 Spieler: 10 Runden. 

Spielablauf:
1. Runde: Jeder Spieler bekommt eine Karte. Oberste Karte des Decks wird sichtbar gemacht => Trumpf für diese Runde.
Jeder Spieler soll vorhersagen, wie viele Stiche er machen wird. (Bei runde 1: Integer zwischen 0 und 1. Generell: Zwischen 0 und #KartenProSpielerInRunde). 
int KartenProSpieler = int aktuelle_runde
int maxStiche = KartenProSpieler
Stich: Jeder Spieler spielt reihum eine Karte durch Berühren der Karte. Wenn die erste Person, die eine Karte spielt, beispielsweise Rot hat, müssen alle anderen Spieler auch Rot spielen oder Wizard oder Jester, falls er etwas davon hat. Ansonsten darf er auch Karten anderer Farbe spielen. Warte bis jeder Spieler seine Karte gespielt hat, danach gewinnt die höchste Karte.
Wenn: Vorhersage == AnzahlStiche: 20 Punkte + 10 Punkte pro Stich
Wenn man zu viel oder zu wenig vorhergesagt hat, bekommt man pro Zahl -10 Punkte.
2. Runde: Karten neu durchmischen, jedem Spieler 2 Karten geben. Oberste Karte des Decks wird sichtbar gemacht => Trumpf für diese Runde.

Sonderfälle:
Falls die oberste Karte im Deck ein Narre ist, gibt es keinen Trumpf. 
Falls die oberste Karte im Deck ein Wizard ist, wird ein zufälliger Trumpf gewählt.
Falls alle Spieler die selbe Karte spielen, gewinnt der, der sie zuerst gespielt hat.
In der letzten Runde sind alle Karten im Spiel, es gibt keinen Trumpf.

