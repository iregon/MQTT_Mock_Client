package fxml.gui.handlers.subscriber;

import fxml.gui.handlers.GUIHandler;
import fxml.gui.subscriber.MessageGUI;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import services.mqtt.messagges.MqttMessageExtended;
import services.mqtt.subscriber.MqttSubscribersManager;
import services.utils.logs.Logger;
import services.utils.regex.RegexUtil;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GUIMessagesSubscriberHandler implements GUIHandler {

    private ScrollPane messagesPane;

    private VBox vbox_messagesContainer = new VBox();

    private ArrayList<MqttMessageExtended> messages;
    private String currentTopic = "ALL MESSAGES";

    private MqttSubscribersManager manager;

    public GUIMessagesSubscriberHandler(ScrollPane messagesPane, MqttSubscribersManager manager) {
        this.messagesPane = messagesPane;
        this.manager = manager;

        startMessagesObserver();
        startCurrentTopicObserver();

        setStaticGUISettings();
    }

    private void startMessagesObserver() {
        JavaFxObservable.emitOnChanged(manager.getObservableMessagesList())
                .subscribe(
                        list -> {
                            messages = new ArrayList<>(list);
                            Platform.runLater(this::updateGUI);
                        },
                        error -> Logger.getInstance().logError(MessageFormat.format("GUI Subscriber Messages Handler (Messages): {0}", error.getMessage())),
                        () -> {}
                );
    }

    private void startCurrentTopicObserver() {
        JavaFxObservable.emitOnChanged(manager.getObservableCurrentTopic())
                .subscribe(
                        topic -> {
                            currentTopic = topic.get(0);
                            Platform.runLater(this::updateGUI);
                        },
                        error -> Logger.getInstance().logError(MessageFormat.format("GUI Subscriber Messages Handler (Topic): {0}", error.getMessage())),
                        () -> {}
                );
    }

    private void setStaticGUISettings() {
        vbox_messagesContainer.setPadding(new Insets(5));
        vbox_messagesContainer.setSpacing(5);
    }

    /**
     * Method to be called for updating the graphics
     */
    @Override
    public void updateGUI() {
        vbox_messagesContainer.getChildren().clear();

        ArrayList<MqttMessageExtended> msgs = getMessagesFromCurrentTopic();

        // Show last messasges on top
        for (int i = msgs.size() - 1; i >= 0; i--) {
            Pane msgGui = MessageGUI.getInstance().generateGUI(msgs.get(i));
            msgGui.setPadding(new Insets(5));
            vbox_messagesContainer.getChildren().add(msgGui);
        }
        messagesPane.setContent(vbox_messagesContainer);
    }

    private ArrayList<MqttMessageExtended> getMessagesFromCurrentTopic() {
        if (currentTopic.equals("") || currentTopic.isEmpty()) return new ArrayList<>();
        else {
            // Condition for filtering
            Predicate<MqttMessageExtended> byTopic = msg -> RegexUtil.getInstance().match(currentTopic ,msg.getTopic());

            return messages.stream()
                    .filter(byTopic)
                    .collect(Collectors.toCollection(ArrayList::new));

        }
    }
}