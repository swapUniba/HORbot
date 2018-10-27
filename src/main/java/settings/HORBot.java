package settings;

import common.HORLogger;
import common.HORmessages;
import common.PropertyUtilities;
import common.Survey;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Define HORBot class
 *
 * @author Roberto B. Stanziale
 * @version 1.0
 */
public class HORBot extends TelegramLongPollingBot implements LoggerInterface {

    // TELEGRAM COMMANDS
    private final static String START = "/start";
    private final static String LOGIN = "/login";
    private final static String SURVEY = "/survey";
    private final static String SHOWANSWER = "/showanswer";
    private final static String RESETANSWER = "/resetanswer";
    private final static String HELP = "/help";
    private String command = "UnknownCommand";

    // SURVEYS
    private Survey s = new Survey("questions.txt");

    /**
     * Get message from chat and send a new message
     * @param update message received
     */
    public void onUpdateReceived(Update update) {
        // Check text message
        if(update.hasMessage() && update.getMessage().hasText()) {

            // Set user info for logging
            String user_first_name = update.getMessage().getChat().getFirstName();
            String user_last_name = update.getMessage().getChat().getLastName();
            String user_username = update.getMessage().getChat().getUserName();
            long user_id = update.getMessage().getChat().getId();

            // Set chat ID
            Long sender_id = update.getMessage().getChatId();

            // Set text received
            String received_text = update.getMessage().getText();

            // Set message structure
            SendMessage message = new SendMessage()
                    .setChatId(sender_id);

            // START COMMAND
            if (received_text.equals(START)) {
                this.command = START;
                message.setText("Ciao, per cominciare effettua il login per Myrror attraverso il comando /login!");
            }
            // SHOW ANSWER COMMAND
            else if (received_text.equals(SHOWANSWER)) {
                this.command = SHOWANSWER;
                message.setText(s.toString());
            }
            // RESET ANSWERS
            else if (received_text.equals(RESETANSWER)) {
                this.command = RESETANSWER;
                s.resetAnswers();
                message.setText("Risposte del questionario reimpostate.");
            }
            // HELP COMMAND
            else if (received_text.equals(HELP)) {
                this.command = HELP;
                message.setText("Puoi utilizzarmi con i seguenti comandi:\n\n" +
                        "/login - Effettua il login per Myrror\n" +
                        "/survey - Inizia il questionario\n" +
                        "/showanswer - Visualizza le risposte del questionario\n" +
                        "/resetanswer - Reimposta le risposte del questionario\n" +
                        "/help - Informazioni sui comandi");
            }
            // LOGIN COMMAND
            else if (received_text.equals(LOGIN)) {
                this.command = LOGIN;
                message.setText("Inviami le credenziali secondo questo modello:\n\nemail\npassword");
            }
            else if (!received_text.equals(LOGIN) && this.command.equals(LOGIN)) {
                this.command = "Comando sconosciuto";
                message.setText(HORmessages.messageLogin(received_text));
            }
            // SURVEY COMMAND
            else if (received_text.equals(SURVEY)) {
                this.command = SURVEY;

                if (!s.isComplete()) {
                    message.setText(s.getNextQuestion());

                    // Add keyboard to message
                    message.setReplyMarkup(HORmessages.setKeyboard());
                } else {
                    message.setText("Questionario già completato.");
                }
            }
            else if (!received_text.equals(SURVEY) && this.command.equals(SURVEY)) {
                s.setNextAnswer(received_text);

                if (!s.isComplete()) {
                    message.setText(s.getNextQuestion());

                    // Add keyboard to message
                    message.setReplyMarkup(HORmessages.setKeyboard());
                } else {
                    message.setText("Questionario completato.");

                    // Remove keyboard from message
                    ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                    message.setReplyMarkup(keyboardMarkup);
                    this.command = "Comando sconosciuto";
                }
            }
            // UNKNOWN COMMAND
            else {
                message.setText("Comando sconosciuto: " + received_text);
            }

            // Log message values
            logger.info(new HORLogger().logUserInfo(user_first_name, user_last_name, user_username, Long.toString(user_id)));

            try{
                // Send answer
                execute(message);

            } catch(TelegramApiException e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Get Bot Username
     * @return Bot Username
     */
    public String getBotUsername() {
        return new PropertyUtilities().getProperty("username");
    }

    /**
     * Get Bot Token
     * @return Bot Token
     */
    public String getBotToken() {
        return new PropertyUtilities().getProperty("token");
    }
}