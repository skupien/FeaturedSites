# FeaturedSites
Mini-projekt na przedmiot Bazy Danych 2020.
Dokumentowa baza danych oparta na technologii MongoDB, pisana w javie. GUI w oparciu o framework NetBeans. <br><br>
Baza przechowuje wartości preferencje dla zapisanych osób oraz wylicza wartości dla nowo wprowadzonych osób
na podstawie połączeń z innymi już obecnymi użytkownikami. <br>
W momencie wprowadzenia nowej osoby będą podawane/zaznaczane osoby obecne w bazie,
które będą miały połączenie z nową osobą (połączenie - tzn. np: znajomość). <br>
Biorąc pod uwagę wartości ustalonych preferencji dla obecnych osób będą one przewidywane i automatycznie 
uzupełniane dla osoby dodanej.

## Struktura bazy danych:
Baza posiada dwie kolekcje: site, user.
### User:
Posiada rubryki na: Imię, nazwisko, wiek, stopień edukacji (None, primary, secondary, bachelor, master, doctor, professor).<br>
Dodatkowo posiada on 4 pola przyjmujące wartości [-100; 100] odzwierciedlające poglądy - polityka, ekonomia, religyjność i empatia. 
Będą one służyć do znajdywania proponowanych stron. <br>
Każdy użytkownik posiada tablicę "znajomych", po których dziedziczy powyższe poglądy (wyliczone jako średnia). <br>
Jako ostatnia rubryka to pole preferencji - kolekcja mniej ważnych upodobań, które przyjmują wartość -1, 0 lub 1. <br>
Przykładowy user w postaci json:
```
{
    "_id" : ObjectId("5ed62aec6d082278c576b6b9"),
    "name" : "Jan",
    "surname" : "Kowalski",
    "age" : 33.0,
    "education" : "Secondary",
    "politics" : -28.0,
    "economics" : 34.67,
    "religiousness" : 17.67,
    "empathy" : 10.33,
    "friends" : [ 
        ObjectId("5ed60ed5d7bd984c0ed2bc96"), 
        ObjectId("5ed60ed5d7bd984c0ed2bc97"), 
        ObjectId("5ed60ed5d7bd984c0ed2bc98")
    ],
    "preferences" : {
        "Vegan" : -1.0,
        "Aesthetics" : -1.0,
        "Ecology" : 0.0,
        "Synthwave" : -1.0,
        "Bikes" : -1.0,
        "Art" : 0.0,
        "Anime" : -1.0
    }
}
```

### Site:
Strony są podobne w srtukturze do użytkowników - posiadają rubryki na nazwę strony, średnią wieku użytkowników, średni stopień wykształcenia,
4 osie poglądów oraz pole preferencji analogiczne do tych użytkowników. <br>
Na podstawie tych rubryk będziemy szukać podobieństw danych użytkowników do potencjalnych proponowanych stron.
Przykładowa strona w postaci json:
```
{
    "_id" : ObjectId("5ed60edad7bd984c0ed2bcad"),
    "name" : "Codziennik Programisty",
    "age" : 30.9,
    "education" : 3.0,
    "politics" : -20.0,
    "economics" : 30.0,
    "religiousness" : -20.0,
    "empathy" : 0.0,
    "preferences" : {
        "Vegan" : 0.0,
        "Aesthetics" : 0.0,
        "Ecology" : 0.0,
        "Synthwave" : 1.0,
        "Bikes" : 0.0,
        "Art" : 0.0,
        "Anime" : 1.0
    }
}
```

## MongoJava.java
Kod, który odpowiada za połączenie z bazą danych. Jej najważniejsze funkcje to **addUserWithFriends()** oraz **findFeaturedSites()**.
### addUserWithFriends(String name, String surname, double age, String education, List<> friendsID)
Funkcja przyjmuje podstawowe informacje nowego użytkownika oraz listę znajomych. Z listy znajomych funkcja zbiera dla każdej 
osoby 4 pola poglądów oraz preferencji, wyliczając z poglądów średnią, a z sumy preferencji signum. Te wartości są przypisywane do nowego użytkownika.
### findFeaturedSites(double radius)
Funkcja znajduje dla każdej strony wszystkich użytkowników o podobnych poglądach. Wyliczana jest średnia z różnicy
poglądów strony a pojedynczego użytkownika. Gdy róznica jest mniejsza od **radius**, użytkownik jest dodawany do mapy.
### findFeaturedSitesForUser(ObjectId id, double radius)
Działa analogicznie do **findFeaturedSites**, ale wyszukuje dla wybranego użytkownika.


## MonGui.java
Klasa odpowiedzialna za GUI.

