package Controllers;

import SocketServer.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import models.Conversation;
import models.Message;
import models.Personne;
import services.ConversationService;
import services.MessageService;

import java.util.List;
import java.util.Objects;

public class ChatPopupController {
    @FXML
    private Label lblReceiverName;

    @FXML
    private javafx.scene.control.ScrollPane scroll;
    @FXML
    private VBox messagesBox;
    @FXML
    private ImageView imgReceiverAvatar;
    @FXML
    private TextField txtMessage;

    private ChatClient client;
    private MessageService messageService = new MessageService();
    private ConversationService conversationService = new ConversationService();
    private int receiverId;
    private int currentUserId;
    private int conversationId;

    private Personne receiver;

    // Initialisation du chat
    public void initChat(int currentUserId, Personne receiver) {

        this.currentUserId = currentUserId;
        this.receiver = receiver;
        Image avatar = new Image(
                Objects.requireNonNull(
                        getClass().getResource("/Backoffice/icons/usericon.png")
                ).toExternalForm()
        );

        imgReceiverAvatar.setImage(avatar);

        Circle clip = new Circle(20, 20, 20);
        imgReceiverAvatar.setClip(clip);

        this.receiverId = receiver.getId();

        try {

            // ✅ récupérer conversation existante
            Conversation c =
                    conversationService.getConversationBetweenUsers(
                            currentUserId,
                            receiverId
                    );

            if(c == null){
                System.out.println("Erreur : conversation introuvable");
                return;
            }

            this.conversationId = c.getId();


            client = new ChatClient(currentUserId);

            // Charger historique
            List<Message> history = messageService.getMessagesByConversation(conversationId);
            for (Message m : history) {
                boolean isMe = m.getSenderId() == currentUserId;
                addMessageBubble(m.getContenu(), isMe, m.getSentAt());

            }
            lblReceiverName.setText(receiver.getNom() + " " + receiver.getPrenom());

            //  écouter socket
            client.startListening(msg -> {
                Platform.runLater(() -> {
                    String[] data = msg.split(";", 2);
                    if (data.length == 2) {
                        String messageText = data[1];
                        addMessageBubble(messageText, false, java.time.LocalDateTime.now());

                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Définir le destinataire
    public void setReceiver(Personne p) {
        receiver = p;
    }

    @FXML
    void sendMessage(){

        String text = txtMessage.getText();

        if(text == null || text.trim().isEmpty())
            return;

        if(conversationId == 0){
            System.out.println("Conversation non initialisée !");
            return;
        }

        //  Heure exacte du message
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        //  UI (avec vraie heure)
        addMessageBubble(text, true, now);

        //  Socket
        if(client != null){
            client.send(receiverId, text);
        }

        //  DB
        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(currentUserId);
        msg.setContenu(text);
        msg.setSentAt(now); // ✅ IMPORTANT

        messageService.sendMessage(msg);

        txtMessage.clear();
    }


    private void addMessageBubble(String text, boolean isMe, java.time.LocalDateTime sentAt) {

        Label messageLabel = new Label(text);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(250);

        // ✅ Utiliser le vrai temps
        String time = sentAt.format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        );

        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        VBox messageContainer = new VBox(3);
        messageContainer.getChildren().addAll(messageLabel, timeLabel);

        if (isMe) {
            messageLabel.setStyle(
                    "-fx-background-color: #1877f2;" +
                            "-fx-text-fill: white;" +
                            "-fx-padding: 8 12 8 12;" +
                            "-fx-background-radius: 20;"
            );
        } else {
            messageLabel.setStyle(
                    "-fx-background-color: #e4e6eb;" +
                            "-fx-text-fill: black;" +
                            "-fx-padding: 8 12 8 12;" +
                            "-fx-background-radius: 20;"
            );
        }

        HBox bubbleContainer = new HBox(5);

        if (isMe) {
            bubbleContainer.setStyle("-fx-alignment: CENTER_RIGHT;");
            bubbleContainer.getChildren().add(messageContainer);
        } else {

            ImageView profileImage = new ImageView(
                    new Image(getClass().getResourceAsStream("/Backoffice/icons/usericon.png"))
            );

            profileImage.setFitWidth(30);
            profileImage.setFitHeight(30);

            Circle clip = new Circle(15, 15, 15);
            profileImage.setClip(clip);

            bubbleContainer.setStyle("-fx-alignment: CENTER_LEFT;");
            bubbleContainer.getChildren().addAll(profileImage, messageContainer);
        }

        messagesBox.getChildren().add(bubbleContainer);

        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}