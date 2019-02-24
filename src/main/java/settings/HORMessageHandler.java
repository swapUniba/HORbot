package settings;

import com.vdurmont.emoji.EmojiParser;
import common.UserPreferences;
import common.Utils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import recommender.RecommendUtils;
import recommender.contentBased.beans.Item;
import recommender.contentBased.services.Recommender;
import recommender.contextAware.beans.UserContext;
import recommender.contextAware.services.ContextAwareRecommender;
import survey.context.beans.Context;
import survey.context.beans.Location;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.toIntExact;

/**
 * Define HOR message handler class
 *
 * @author Roberto B. Stanziale
 * @version 1.0
 */
public class HORMessageHandler {

    // USER COMMAND
    private Map<Integer, String> userCommand;

    // Recommender module for send items to users
    private Recommender recommenderForBari;
    private Recommender recommenderForTorino;

    /**
     * Constructor of HORMessageHandler
     */
    public HORMessageHandler() {
        this.userCommand = new HashMap<>();
    }

    /**
     * Initialize user command
     * @param user_id representing user id
     */
    public void initializeUser(long user_id) {
        this.userCommand.put(toIntExact(user_id), "unknown");
    }

    /**
     * Set message to send to user
     * @param user_id representing user id
     * @param userPreferences representing user preferences
     * @param message representing received message from user
     * @return a text message or a document message
     */
    public Object setMessage(long user_id,
                             UserPreferences userPreferences,
                             Message message) {
        String received_text = message.hasText() ? message.getText() : "";
        SendMessage sendMessage = new SendMessage();
        SendDocument sendDocument = new SendDocument();

        boolean noCommand = false;

        switch (received_text) {
            case HORCommands.START:
                this.userCommand.replace(toIntExact(user_id), HORCommands.START);
                sendMessage.setText(HORmessages.MESSAGE_START);
                break;

            case HORCommands.LOGIN:
                userCommand.replace(toIntExact(user_id), HORCommands.LOGIN);
                sendMessage.setText(HORmessages.MESSAGE_LOGIN);
                break;

            case HORCommands.SURVEY:
                userCommand.replace(toIntExact(user_id), HORCommands.SURVEY);

                if (!userPreferences.getSurvey().isComplete()) {
                    sendMessage.setText(HORmessages.MESSAGE_SURVEY_START +
                            userPreferences.getSurvey().getNextQuestion());
                    // Add keyboard to message
                    sendMessage.setReplyMarkup(HORmessages.setReplyKeyboardForSurvey());
                } else {
                    sendMessage.setText(HORmessages.MESSAGE_SURVEY_ALREADY_COMPLETE);
                }
                break;

            case HORCommands.SET_LOCATION:
                userCommand.replace(toIntExact(user_id), HORCommands.SET_LOCATION);
                sendMessage.setText(HORmessages.MESSAGE_POSITION);
                break;

            case HORCommands.SET_CONTEXTS:
                userCommand.replace(toIntExact(user_id), HORCommands.SET_CONTEXTS);

                if (!userPreferences.getSurveyContext().isComplete()) {
                    Context c = userPreferences.getSurveyContext().getNextContext();
                    sendMessage.setText(EmojiParser.parseToUnicode(c.toString()))
                            .setParseMode("markdown");
                } else {
                    sendMessage.setText(HORmessages.MESSAGE_ACTIVITIES_CHOSEN);
                    userCommand.replace(toIntExact(user_id), "unknown");
                }
                break;

            case HORCommands.SHOW_ANSWER:
                userCommand.replace(toIntExact(user_id), HORCommands.SHOW_ANSWER);
                sendMessage.setText(userPreferences.getSurvey().toString());
                break;

            case HORCommands.SHOW_CONTEXTS:
                userCommand.replace(toIntExact(user_id), HORCommands.SHOW_CONTEXTS);
                sendMessage.setText(EmojiParser.parseToUnicode(userPreferences.getSurveyContext().showContextChosen()));
                break;

            case HORCommands.RESET_ANSWER:
                userCommand.replace(toIntExact(user_id), HORCommands.RESET_ANSWER);
                userPreferences.getSurvey().resetAnswers();
                sendMessage.setText(HORmessages.MESSAGE_SURVEY_RESET);
                break;

            case HORCommands.RESET_CONTEXTS:
                userCommand.replace(toIntExact(user_id), HORCommands.RESET_CONTEXTS);

                for (Context c : userPreferences.getSurveyContext().getSurveyValues()) {
                    c.resetCheckValues();
                }
                sendMessage.setText(HORmessages.MESSAGE_ACTIVITIES_RESET);
                break;

            case HORCommands.GET_RECOMMEND:
                try {
                    if (userPreferences.isComplete()) {
                        UserContext userContext;
                        if (userPreferences.getUserContext() == null) {
                            userContext = new UserContext(userPreferences.getOntology());
                        } else {
                            userContext = userPreferences.getUserContext();
                        }
                        userPreferences.setUserContext(userContext);
                        int checkUserContext = ContextAwareRecommender.checkValuesUserContext(userContext);

                        if (checkUserContext == 0) {
                            Location location = userPreferences.getLocation();
                            this.initRecommender(location);

                            int recommendType = RecommendUtils.getRecommendType();
                            if(!userPreferences.checkListRecommendPOI()) {
                                // TODO: check this logic for chose recommend type
                                String query = RecommendUtils.generateQueryAccordingRecommendType(
                                        userPreferences,
                                        userContext,
                                        recommendType);
                                userPreferences.setRecommendPOI(this.getRecommender(location)
                                                .searchItems(query, location));
                            }

                            String text;
                            if (userPreferences.getRecommendPOI() != null) {
                                Item i = userPreferences.getRecommendPOI();
                                i.setRecommenderType(recommendType);
                                text = i.toString();
                            } else {
                                text = HORmessages.MESSAGE_NO_ACTIVITY;
                            }

                            sendMessage.setText(EmojiParser.parseToUnicode(text));
                        } else if (checkUserContext == 1) {
                            sendMessage.setText(HORmessages.MESSAGE_MISSING_COMPANY);
                        } else if (checkUserContext == 2) {
                            sendMessage.setText(HORmessages.MESSAGE_MISSING_RESTED);
                        } else if (checkUserContext == 3) {
                            sendMessage.setText(HORmessages.MESSAGE_MISSING_MOOD);
                        } else if (checkUserContext == 4) {
                            sendMessage.setText(HORmessages.MESSAGE_MISSING_ACTIVITY);
                        }

                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_REFERENCES_NON_COMPLETE);
                    }
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                break;

            case HORCommands.SET_COMPANY:
                userCommand.replace(toIntExact(user_id), HORCommands.SET_COMPANY);
                // Add keyboard to message
                sendMessage.setReplyMarkup(HORmessages.setReplyKeyboardForCompany());
                sendMessage.setText(HORmessages.MESSAGE_COMPANY);
                break;

            case HORCommands.SET_RESTED:
                userCommand.replace(toIntExact(user_id), HORCommands.SET_RESTED);
                sendMessage.setReplyMarkup(HORmessages.setReplyKeyboardBoolean());
                sendMessage.setText(HORmessages.MESSAGE_RESTED);
                break;

            case HORCommands.SET_MOOD:
                userCommand.replace(toIntExact(user_id), HORCommands.SET_MOOD);
                sendMessage.setReplyMarkup(HORmessages.setReplyKeyboardForMood());
                sendMessage.setText(HORmessages.MESSAGE_MOOD);
                break;

            case HORCommands.SET_ACTIVITY:
                userCommand.replace(toIntExact(user_id), HORCommands.SET_ACTIVITY);
                sendMessage.setReplyMarkup(HORmessages.setReplyKeyboardBoolean());
                sendMessage.setText(HORmessages.MESSAGE_ACTIVITY);
                break;

            case HORCommands.HELP:
                userCommand.replace(toIntExact(user_id), HORCommands.HELP);
                sendMessage.setText(HORmessages.MESSAGE_HELP);
                break;

            case HORCommands.LOGFILE:
                this.sendDocUploadingAFile(sendDocument,
                        Utils.createLogFile(user_id, userPreferences));
                break;

            default:
                sendMessage.setText(HORmessages.UNKNOWN_COMMAND + received_text);
                noCommand = true;
                break;
        }
        /*
         * If a command has a complex logic interaction
         * When the user chooses a command
         * that needs a longer sequence of questions and answers
         */
        if (noCommand) {
            ReplyKeyboardRemove keyboardMarkup;
            switch (this.userCommand.get(toIntExact(user_id))) {
                case HORCommands.LOGIN:
                    userCommand.replace(toIntExact(user_id), "unknown");
                    sendMessage.setText(HORmessages.messageLogin(received_text, userPreferences) + "\n" +
                            HORmessages.MESSAGE_LOGIN_COMPLETE);
                    break;

                case HORCommands.SURVEY:
                    userPreferences.getSurvey().setNextAnswer(received_text);

                    if (!userPreferences.getSurvey().isComplete()) {
                        sendMessage.setText(userPreferences.getSurvey().getNextQuestion());
                        // Add keyboard to message
                        sendMessage.setReplyMarkup(HORmessages.setReplyKeyboardForSurvey());
                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_SURVEY_COMPLETE);

                        // Remove keyboard from message
                        keyboardMarkup = new ReplyKeyboardRemove();
                        sendMessage.setReplyMarkup(keyboardMarkup);
                        userCommand.replace(toIntExact(user_id), "unknown");
                    }
                    break;

                case HORCommands.SET_LOCATION:
                    Location l = new Location(message.getLocation().getLongitude(), message.getLocation().getLatitude());
                    userPreferences.setLocation(l);
                    sendMessage.setText(HORmessages.MESSAGE_POSITION_SAVED);
                    userCommand.replace(toIntExact(user_id), "unknown");
                    break;

                case HORCommands.SET_CONTEXTS:
                    Context c = userPreferences.getSurveyContext().getNextContext();
                    if (HORmessages.setActivityFlags(c, received_text.split(" "))) {
                        String text = userPreferences.getSurveyContext().isComplete()
                                ? HORmessages.MESSAGE_ACTIVITIES_SAVED
                                : userPreferences.getSurveyContext().getNextContext().toString();

                        sendMessage.setText(EmojiParser.parseToUnicode(text))
                                .setParseMode("markdown");
                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_ACTIVITIES_ERROR);
                        userCommand.replace(toIntExact(user_id), "unknown");
                    }
                    break;

                case HORCommands.SET_COMPANY:
                    if (HORmessages.checkContextCompany(received_text)) {
                        userPreferences.getUserContext().setCompany(received_text);
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_UPDATE);
                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_ERROR);
                    }
                    // Remove keyboard from message
                    keyboardMarkup = new ReplyKeyboardRemove();
                    sendMessage.setReplyMarkup(keyboardMarkup);
                    userCommand.replace(toIntExact(user_id), "unknown");
                    break;

                case HORCommands.SET_RESTED:
                    if (HORmessages.checkContextBoolean(received_text)) {
                        userPreferences.getUserContext()
                                .setRested(received_text.equals("Sì"));
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_UPDATE);
                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_ERROR);
                    }
                    // Remove keyboard from message
                    keyboardMarkup = new ReplyKeyboardRemove();
                    sendMessage.setReplyMarkup(keyboardMarkup);
                    userCommand.replace(toIntExact(user_id), "unknown");
                    break;

                case HORCommands.SET_ACTIVITY:
                    if (HORmessages.checkContextBoolean(received_text)) {
                        userPreferences.getUserContext()
                                .setActivity(received_text.equals("Sì"));
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_UPDATE);
                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_ERROR);
                    }
                    // Remove keyboard from message
                    keyboardMarkup = new ReplyKeyboardRemove();
                    sendMessage.setReplyMarkup(keyboardMarkup);
                    userCommand.replace(toIntExact(user_id), "unknown");
                    break;

                case HORCommands.SET_MOOD:
                    if (HORmessages.checkContextMood(received_text)) {
                        userPreferences.getUserContext()
                                .setMood(received_text.equals("Buon umore"));
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_UPDATE);
                    } else {
                        sendMessage.setText(HORmessages.MESSAGE_CONTEXT_ERROR);
                    }
                    // Remove keyboard from message
                    keyboardMarkup = new ReplyKeyboardRemove();
                    sendMessage.setReplyMarkup(keyboardMarkup);
                    userCommand.replace(toIntExact(user_id), "unknown");
                    break;
            }
        }

        if (sendDocument.getDocument() != null) {
            return sendDocument;
        } else {
            return sendMessage;
        }
    }

    /**
     * Initialize recommender system according user position
     * Bari: 41.1115511, 16.7419939
     * Torino: 45.0702388, 7.6000489
     * @param location representing the user position
     */
    private void initRecommender(Location location) {
        double fromHereToBari = Utils.distance(location.getLatitude(), 41.1115511,
                location.getLongitude(), 16.7419939,
                0.0, 0.0);
        double fromHereToTorino = Utils.distance(location.getLatitude(), 45.0702388,
                location.getLongitude(), 7.6000489,
                0.0, 0.0);

        if (fromHereToBari < fromHereToTorino) {
            this.recommenderForBari = new Recommender(Utils.readCSV("/businesses_bari.csv"));
        } else {
            this.recommenderForTorino = new Recommender(Utils.readCSV("/businesses_torino.csv"));
        }
    }

    /**
     * Get recommender according user position
     * Bari: 41.1115511, 16.7419939
     * Torino: 45.0702388, 7.6000489
     * @param location representing the user position
     * @return Recommender
     */
    private Recommender getRecommender(Location location) {
        double fromHereToBari = Utils.distance(location.getLatitude(), 41.1115511,
                location.getLongitude(), 16.7419939,
                0.0, 0.0);
        double fromHereToTorino = Utils.distance(location.getLatitude(), 45.0702388,
                location.getLongitude(), 7.6000489,
                0.0, 0.0);

        return  fromHereToBari < fromHereToTorino
                ? this.recommenderForBari
                : this.recommenderForTorino;
    }

    /**
     * Generate request for send a preferences log file to user
     * @param sendDocumentRequest representing document to send to user
     * @param logFile representing the file to send user
     */
    private void sendDocUploadingAFile(SendDocument sendDocumentRequest, File logFile) {
        sendDocumentRequest.setDocument(logFile);
        sendDocumentRequest.setCaption("Users log file.");
    }
}