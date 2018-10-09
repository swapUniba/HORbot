# HORBot - RecSys

HorBot è un chat bot sviluppato con le API di Telegram utilizzato come Recommender System che mediante i dati raccolti da Myrror (attraverso le sue API) e dei questionari, è in grado di raccomandare un'attività all'utente che utilizza il chatbot.
Il seguente repository è stato creato per lo sviluppo del lavoro di tesi di Accesso Intelligente all'Informazione ed Elaborazione del Linguaggio Naturale dell'Università degli Studi di Bari.

Per sviluppare il sistema è stato fatto uso delle seguenti **tecnologie**:
* [Maven](https://maven.apache.org/)
* [Log4j](https://logging.apache.org/log4j/2.x/)
* [IntelliJ](https://www.jetbrains.com/idea/)
* [Telegram API](https://core.telegram.org/)

## Configurazione

Aggiungere nella directory _resources_ un file _horbot.properties_ settandolo come segue:

```
# HORBot properties
username=<YOUR_CHATBOT_NAME>
token=<YOUR_CHATBOT_TOKEN>
myrrorToken=<YOUR_MYRROR_TOKEN>
```
