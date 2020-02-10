## Role breakdown

|  | USER | GUIDE | ADMIN | SYSTEM-ADMIN |
| --- | --- | --- | --- | --- |
| Mamma | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [ ] </li></ul> |
| Mamma casalinga | <ul><li> - [x] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [ ] </li></ul> |
| Mamma casalinga, boss | <ul><li> - [x] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> |
| Mamma boss | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> |
| Nonno | <ul><li> - [ ] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [ ] </li></ul> |
| Nonno, boss | <ul><li> - [ ] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> |
| Nonno che si occupa molto dei bimbi | <ul><li> - [x] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> | <ul><li> - [ ] </li></ul> |
| Nonno che si occupa molto dei bimbi, boss | <ul><li> - [x] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [x] </li></ul> | <ul><li> - [ ] </li></ul> |

Se ci viene più comodo possiamo usare drive https://docs.google.com/document/d/179EY5c0OUg_GRBTm9K-zO88aBWxs957rbkmt13e51RU/edit?usp=sharing

Ad oggi (pre consegna es5):  
- Un genitore ha solo il ruolo USER  
- Tutti i nonni hanno ruolo ADMIN per una linea
- Quando aggiungiamo attraverso endpoint il ruolo ADMIN ha un account lo sommiamo a quello di USER
- I vari ADMIN possono effettuare/modificare una prenotazione solo nel giorno corrente e per la linea di cui è amministratore
- Il system admin può effettuare/modificare una prenotazione quando vuole

Ad oggi (consegna progetto)
- Un genitore ha il ruolo di USER, ma può assumerne altri
- I nonni possono essere GUIDE e/o ADMIN
- Quando aggiungiamo il ruolo ADMIN lo sommiamo agli altri
- Un ADMIN può aggiungere/modificare/cancellare una prenotazione solo per la sua linea e per il giorno corrente (GUIDE no)
- Sia ADMIN che GUIDE possono segnare un bimbo come handled/arrived
- Un SYS-ADMIN può fare quello che vuole

## User breakdown
System admin:   
applicazioni.internet.mmap@gmail.com  
12345@Sys  

Altri utenti:    
La password in ogni caso è -> 1!qwerty1!

Abbiamo 50 genitori con ROLE_USER e 2 figli, ad es:  
(genitori.json)
- miles.reilly@test.it        cf-5cfe2813c413075ac504f2bf Francine Little || cf-5cfe2813060656d86cf96a01 Hester Roy
- enid.crawford@test.it       cf-5cfe28131ee6759e737cf2c7 Jordan Merritt || cf-5cfe28131467b27c48d5de0d Noreen Gilbert
- morales.holloway@test.it    cf-5cfe2813323ed36c229a5798 Maria Martinez || cf-5cfe28131cad412d17c49722 Barber Rojas

In nonni_0.json abbiamo:
- 1 ADMIN  
  * reed.snyder@test.it   
- 4 ADMIN&GUIDE   
  * alexandra.winters@test.it   
  * briana.butler@test.it ADMIN  
  * klein.gillespie@test.it
  * pierce.hensley@test.it
- 25 GUIDE  
  * holcomb.waller@test.it  
  * hammond.bowen@test.it  
  * hurst.cohen@test.it",
  
In nonni_1.json abbiamo:
- 1 ADMIN  
  * mai.berg@test.it   
- 4 ADMIN&GUIDE   
  * cooley.bradshaw@test.it      
  * juliet.salazar@test.it   
  * lottie.berger@test.it  
  * neva.hopper@test.it
- 25 GUIDE   
  * burks.schwartz@test.it  
  * julianne.perry@test.it  
  * edwina.cantu@test.it  