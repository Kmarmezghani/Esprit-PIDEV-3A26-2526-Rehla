package Controllers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.*;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.MenuItem;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image  ;

import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import models.*;
import org.json.JSONObject;
import services.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

import javafx.collections.ListChangeListener;
import util.Session;


public class ClientPostsController {


    @FXML
    private BorderPane mainContent;

    @FXML
    private StackPane root;

    @FXML
    private TextField txtNewPost;

    @FXML private Button btnProfile;

    @FXML private ContextMenu profileMenu;
    @FXML
    private MenuItem menuMyActivities;
    private boolean isLiked = false;

    private Image heartEmpty;
    private Image heartFull;
    private Timeline timeUpdater;
    @FXML private ImageView commentIcon;

    private Image commentEmpty;


    private ContextMenu notifMenu = new ContextMenu();
    private boolean isLoadingPosts = false;
    @FXML
    private Label lblMessageBadge;
    @FXML private Label lblNotifCount;
    @FXML private TextField searchField;
    @FXML private Button btnNotif;
    @FXML
    private VBox postsContainer;
    @FXML
    private Button btnMessage;


    private ContextMenu messageMenu = new ContextMenu();
    private boolean isStarred = false;

    private Image starEmpty;
    private Image starFull;
    Personne currentUser = Session.getCurrentUser();
    private File selectedImageFile;
    private ObservableList<Post> postsList = FXCollections.observableArrayList();
private PostService postService = new PostService();
   private NotificationService notificationService = new NotificationService();
    private MessageService messageService = new MessageService();
    private PersonneService personneService = new PersonneService();
    private LikeService likeService = new LikeService();
   private CommentaireService commentService = new CommentaireService();
    private FavorisService favorisService = new FavorisService();
    private ConversationService ConversationService = new ConversationService();
    private Map<Label, LocalDateTime> timeLabels = new HashMap<>();
    private Timeline conversationUpdater;
    private Image avatarImage;
    private Image shareImage;
    private Image editImage;
    private Image deleteImage;
    private static final String UPLOAD_DIR = "C:/shared_uploads/";
    private static final Image HEART_EMPTY =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/blackHeart.png")
                    .toExternalForm());

    private static final Image HEART_FULL =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/HeartRed.png")
                    .toExternalForm());

    private static final Image STAR_EMPTY =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/blackStar.png")
                    .toExternalForm());

    private static final Image STAR_FULL =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/yellowStar.png")
                    .toExternalForm());

    private static final Image SHARE_IMAGE =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/shareNoir.png")
                    .toExternalForm());

    private static final Image EDIT_IMAGE =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/editblue.png")
                    .toExternalForm());

    private static final Image DELETE_IMAGE =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/poubelle.png")
                    .toExternalForm());
    private static final Image COMMENT_EMPTY =
            new Image(ClientPostsController.class
                    .getResource("/Backoffice/icons/commentB.png")
                    .toExternalForm());
    @FXML
    private void initialize() {


//        timeUpdater = new Timeline(
//                new KeyFrame(javafx.util.Duration.seconds(10), e -> refreshConversationTimes())
//        );
//        timeUpdater.setCycleCount(Timeline.INDEFINITE);
//        timeUpdater.play();
//        conversationUpdater = new Timeline(
//                new KeyFrame(javafx.util.Duration.seconds(5), e -> {
//                    if (messageMenu.isShowing()) {
//                        loadConversations();
//                    }
//                })
//        );
//        conversationUpdater.setCycleCount(Timeline.INDEFINITE);
//        conversationUpdater.play();
        Timeline badgeUpdater = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(5),
                        e -> updateUnreadMessagesBadge())
        );

        badgeUpdater.setCycleCount(Timeline.INDEFINITE);
        badgeUpdater.play();

        updateUnreadMessagesBadge();

        loadPosts();
        postsContainer.setFillWidth(true);

        btnMessage.setOnAction(e -> toggleMessages());
        System.out.println("Java Zone = " + ZoneId.systemDefault());
        Personne u = Session.getCurrentUser();
        boolean isGuide = (u != null) && "GUIDE".equalsIgnoreCase(u.getRole());

        if (!isGuide) {
            profileMenu.getItems().remove(menuMyActivities); // pas d’espace vide
        }
        refreshNotifCount();
    }
/*----------------------------header--------------------------------------------------------------*/



    @FXML public void closewindow(ActionEvent event) { getStageFromEvent(event).close(); }
    @FXML public void minwindow(ActionEvent event) { getStageFromEvent(event).setIconified(true); }
    @FXML public void maxwindow(ActionEvent event) {
        Stage stage = getStageFromEvent(event);
        stage.setMaximized(!stage.isMaximized());

    }

    private Stage getStageFromEvent(ActionEvent event) {
        if (event == null) return getStage();

        Object src = event.getSource();
        if (src instanceof Node n) return (Stage) n.getScene().getWindow();
        if (src instanceof MenuItem mi) return (Stage) mi.getParentPopup().getOwnerWindow();
        throw new IllegalArgumentException("Unknown event source: " + src);
    }

    private void switchScene(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = getStageFromEvent(event);
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML public void goToHome(ActionEvent event) { switchScene(event, "/Frontoffice/HomePage.fxml"); }
    @FXML public void goToDestinations(ActionEvent event) { }
    @FXML public void goToPosts(ActionEvent event) { switchScene(event, "/Frontoffice/PostsPage.fxml"); }


    @FXML
    public void goToactivities(ActionEvent event) {
        switchSceneKeepSize((Node) event.getSource(), "/Frontoffice/ActivitiesPage.fxml");
    }
    private void switchSceneKeepSize(Node anyNodeOnScene, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = (Stage) anyNodeOnScene.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void goToMyProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/blogProfileView.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("/Frontoffice/css/blog_styles.css")).toExternalForm()
            );

            Stage stage = (Stage) searchField.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML public void goToMyPosts(ActionEvent event) { }

    private Stage getStage() {
        return (Stage) root.getScene().getWindow();
    }

    @FXML
    public void openProfileMenu(ActionEvent event) {
        if (profileMenu == null || btnProfile == null) return;

        if (profileMenu.isShowing()) {
            profileMenu.hide();
            return;
        }

        profileMenu.show(btnProfile, Side.BOTTOM, 0, 6);
    }
    @FXML
    void goToMyReservations(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyReservation.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) searchField.getScene().getWindow();
            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void goToMyActivities(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/MyActivitiesPage.fxml"));
            Parent root = loader.load();

            MenuItem item = (MenuItem) event.getSource();
            Stage stage = (Stage) item.getParentPopup().getOwnerWindow();

            if (stage.getScene() == null) stage.setScene(new Scene(root));
            else stage.getScene().setRoot(root);

            root.applyCss();
            root.layout();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will be returned to the login screen.");
        alert.showAndWait();
    }
    @FXML
    public void openNotifications(ActionEvent event) {
        try {
            refreshNotifCount();

            List<NotificationService.NotifRow> notifs =
                    notificationService.getLatestUnread(currentUser.getId(), 5);

            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-radius: 14; -fx-padding: 10; -fx-background-color: #f8fafc;");
            menu.getStyleClass().add("notifMenu");

            final double MENU_W = 320;

            if (notifs.isEmpty()) {
                Label lbl = new Label("Aucune notification");
                lbl.setWrapText(true);
                lbl.setPrefWidth(MENU_W);
                lbl.setMaxWidth(MENU_W);
                lbl.setAlignment(Pos.CENTER);
                lbl.setStyle("""
                    -fx-padding: 14 12;
                    -fx-text-fill: #6b7280;
                    -fx-font-size: 13px;
                """);
                menu.getItems().add(new CustomMenuItem(lbl, false));

            } else {
                for (var n : notifs) {

                    Label title = new Label(("WAITLIST_HOLD".equalsIgnoreCase(n.type) ? "⏳ Waitlist" : "🔔 Notification"));
                    title.setStyle("-fx-font-size: 12; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

                    Label msg = new Label(n.message);
                    msg.setWrapText(true);
                    msg.setMaxWidth(300);
                    msg.setStyle("-fx-font-size: 13; -fx-text-fill: #334155;");

                    Label time = new Label(n.createdAt != null ? n.createdAt.toString() : "");
                    time.setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;");

                    VBox card = new VBox(6, title, msg, time);
                    card.setStyle("""
                        -fx-background-color: white;
                        -fx-background-radius: 12;
                        -fx-padding: 12 12;
                        -fx-border-color: #e5e7eb;
                        -fx-border-radius: 12;
                    """);

                    CustomMenuItem it = new CustomMenuItem(card, true);

                    it.setOnAction(ev -> {
                        try {
                            notificationService.markRead(n.id);
                            refreshNotifCount();
                            // si tu veux faire une action spéciale WAITLIST_HOLD ici, tu peux.
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    menu.getItems().add(it);
                }

                menu.getItems().add(new SeparatorMenuItem());

                MenuItem mark = new MenuItem("Tout marquer comme lu");
                mark.setOnAction(e2 -> {
                    try {
                        notificationService.markAllRead(currentUser.getId());
                        refreshNotifCount();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                menu.getItems().add(mark);
            }

            // positionner sous le bouton notif
            var b = btnNotif.localToScreen(btnNotif.getBoundsInLocal());
            double x = b.getMaxX() - MENU_W;
            double y = b.getMaxY() + 8;

            x = Math.max(8, x);
            y = Math.max(8, y);

            menu.show(btnNotif, x, y);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void refreshNotifCount() {
        try {
            int n = notificationService.countUnread(currentUser.getId());

            if (lblNotifCount != null) {
                lblNotifCount.setText(String.valueOf(n));
                boolean show = n > 0;
                lblNotifCount.setVisible(show);
                lblNotifCount.setManaged(show);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*-------------------------------------------------------------------------------------------------------------------------*/


    private void toggleMessages(){

        if(messageMenu.isShowing()){
            messageMenu.hide();
            return;
        }

        if (messageMenu.getItems().isEmpty()) {
            loadConversations();
        }

        messageMenu.show(btnMessage, Side.BOTTOM, -250, 8);
    }
    private void loadConversations() {

        timeLabels.clear();
        messageMenu.getItems().clear();

        if(!messageMenu.getStyleClass().contains("facebook-menu")){
            messageMenu.getStyleClass().add("facebook-menu");
        }

        Label title = new Label("Chats");
        title.getStyleClass().add("menu-title");

        VBox headerBox = new VBox(title);
        headerBox.getStyleClass().add("menu-header");

        CustomMenuItem headerItem = new CustomMenuItem(headerBox);
        headerItem.setHideOnClick(false);

        messageMenu.getItems().add(headerItem);

        List<Conversation> conversations =
                ConversationService.getAllPossibleConversations(currentUser.getId());

        Map<Conversation, Message> lastMessageMap = new HashMap<>();
        for (Conversation conv : conversations) {
            lastMessageMap.put(conv, messageService.getLastMessage(conv.getId()));
        }

// On trie la liste en utilisant la Map
        conversations.sort((c1, c2) -> {
            Message m1 = lastMessageMap.get(c1);
            Message m2 = lastMessageMap.get(c2);
            if (m1 == null || m1.getSentAt() == null) return 1;
            if (m2 == null || m2.getSentAt() == null) return -1;
            return m2.getSentAt().compareTo(m1.getSentAt());
        });

        for (Conversation conv : conversations) {

            int otherUserId =
                    (conv.getUser1Id() == currentUser.getId())
                            ? conv.getUser2Id()
                            : conv.getUser1Id();

            Personne otherUser = personneService.getById(otherUserId);

            if(otherUser == null) continue;

            Message lastMessage = null;

            if(conv.getId() > 0){
                lastMessage = messageService.getLastMessage(conv.getId());
            }
            System.out.println("Conversation id = " + conv.getId());
            System.out.println("Last message = " + lastMessage);
            boolean isUnread = false;

            if (lastMessage != null) {
                isUnread = !lastMessage.isRead()
                        && lastMessage.getSenderId() != currentUser.getId();
            }

            String lastText = "Aucun message";

            if (lastMessage != null) {

                boolean isMe = lastMessage.getSenderId() == currentUser.getId();

                String prefix = isMe
                        ? "Vous : "
                        : otherUser.getPrenom() + " : ";

                lastText = prefix + lastMessage.getContenu();
            }

            if (lastText.length() > 25) {
                lastText = lastText.substring(0, 25) + "...";
            }

            String timeText = "";

            if (lastMessage != null) {
                timeText = formatTime(lastMessage.getSentAt());
            }

            String path = otherUser.getProfilePhoto();

            ImageView avatar = new ImageView();

            if (path != null && !path.isBlank()) {

                File file = new File(path.trim());

                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    avatar.setImage(img);
                }
            }

            avatar.setFitWidth(45);
            avatar.setFitHeight(45);

// Clip circulaire propre
            Circle clip = new Circle(22.5);
            clip.centerXProperty().bind(avatar.fitWidthProperty().divide(2));
            clip.centerYProperty().bind(avatar.fitHeightProperty().divide(2));
            avatar.setClip(clip);


            Label nameLabel = new Label(
                    otherUser.getNom() + " " + otherUser.getPrenom());

            nameLabel.getStyleClass().add("msg-name");

            Label messageLabel = new Label(lastText);
            messageLabel.getStyleClass().add("msg-text");
            messageLabel.setMaxWidth(220);
            messageLabel.setWrapText(true);

            VBox textBox = new VBox(nameLabel, messageLabel);
            textBox.setSpacing(3);

            Label timeLabel = new Label(timeText);
            timeLabel.getStyleClass().add("msg-time");

            if (lastMessage != null) {
                System.out.println("Time added for conversation " + conv.getId());
                timeLabels.put(timeLabel, lastMessage.getSentAt());
            }

            Circle redDot = new Circle(5);
            redDot.setStyle("-fx-fill: red;");
            redDot.setVisible(isUnread);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, avatar, textBox, spacer, redDot, timeLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("message-row");

            row.setMinWidth(360);
            row.setPrefWidth(360);
            row.setMaxWidth(360);

            CustomMenuItem item = new CustomMenuItem(row);
            item.setHideOnClick(true);

            item.setOnAction(e -> {

                Conversation realConv =
                        ConversationService.getConversationBetweenUsers(
                                currentUser.getId(),
                                otherUser.getId()
                        );

                if(realConv == null){

                    realConv =
                            ConversationService.createConversation(
                                    currentUser.getId(),
                                    otherUser.getId()
                            );
                }

                if (redDot.isVisible()) {

                    messageService.markConversationAsRead(
                            realConv.getId(),
                            currentUser.getId()
                    );

                    redDot.setVisible(false);
                }

                openChat(otherUser);

            });

            messageMenu.getItems().add(item);
        }

        refreshConversationTimes();
    }
    private void updateUnreadMessagesBadge(){

        int unreadCount =
                messageService.countUnreadMessagesForUser(currentUser.getId());


        if(unreadCount > 0){
            lblMessageBadge.setText(String.valueOf(unreadCount));
            lblMessageBadge.setVisible(true);
        }else{
            lblMessageBadge.setVisible(false);
        }
    }


    private void refreshConversationTimes() {

        for (Map.Entry<Label, LocalDateTime> entry : timeLabels.entrySet()) {

            Label label = entry.getKey();
            LocalDateTime time = entry.getValue();

            Platform.runLater(() -> label.setText(formatTime(time)));
            System.out.println("DB time = " + time);
            System.out.println("NOW     = " + LocalDateTime.now());
            System.out.println("MINUTES = " + Duration.between(time, LocalDateTime.now()).toMinutes());
            System.out.println("----------------------");
        }
        System.out.println("refresh...");

    }
    private String formatTime(LocalDateTime dateTime) {

        LocalDateTime now = LocalDateTime.now();

        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        long hours = java.time.Duration.between(dateTime, now).toHours();
        long days = java.time.Duration.between(dateTime, now).toDays();

        if (minutes < 1) return "à l'instant";
        if (minutes < 60) return "il y a " + minutes + " min";
        if (hours < 24) return "il y a " + hours + " h";
        if (days == 1) return "Hier";
        if (days < 7) return "il y a " + days + " j";

        return dateTime.toLocalDate().toString(); // fallback (date)
    }
    private void openChat(Personne user){

        try{

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Frontoffice/ChatPopup.fxml"));

            Parent popup = loader.load();

            ChatPopupController controller = loader.getController();


            controller.initChat(currentUser.getId(), user);

            Stage stage = new Stage();
            stage.setTitle("Chat avec " + user.getNom());
            stage.setScene(new Scene(popup));
            stage.show();

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    private void loadPosts(){

        if(isLoadingPosts) return;
        isLoadingPosts = true;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                List<Post> posts = postService.getAll()
                        .stream()
                        .sorted((p1, p2) ->
                                p2.getDatePublication()
                                        .compareTo(p1.getDatePublication()))
                        .toList();

                Map<Integer,Integer> commentCounts =
                        commentService.countByPosts(posts);

                Platform.runLater(() -> {

                    postsContainer.getChildren().clear();

                    for(Post post : posts){
                        postsContainer.getChildren()
                                .add(createPostCard(post, commentCounts));
                    }

                    isLoadingPosts = false;
                });

                return null;
            }
        };

        new Thread(task).start();
    }



    private VBox createPostCard(Post post,
                                Map<Integer, Integer> commentCounts){


        VBox card = new VBox(10);
        card.getStyleClass().addAll("post", "glass");

        card.setCache(true);
        card.setCacheHint(CacheHint.SPEED);

        card.setUserData(post.getId());

        // ================= HEADER =================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

// Avatar
        ImageView avatar = new ImageView();

        String path = currentUser.getProfilePhoto();

        if (path != null && !path.isBlank()) {

            File file = new File(path.trim());

            if (file.exists()) {
                Image img = new Image(file.toURI().toString(),
                        40, 40,  // resize direct (optimisation)
                        true,    // preserve ratio
                        true);   // smooth
                avatar.setImage(img);
            }
        }

        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(false);
        avatar.setCache(true);
        avatar.setCacheHint(CacheHint.SPEED);

        avatar.getStyleClass().add("avatar");


// User info
        VBox userInfo = new VBox(2);

// Nom auteur (avec sécurité null)
        String fullName = (post.getAuteur() != null)
                ? post.getAuteur().getPrenom() + " " + post.getAuteur().getNom()
                : "Utilisateur inconnu";

        Label name = new Label(fullName);
        name.getStyleClass().add("name");

// Date
        Label date = new Label(post.getDatePublication() + " • Public");
        date.getStyleClass().add("muted");

// Tags
        HBox tagsRow = new HBox(6);
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        Label tag1 = new Label("#Design");
        tag1.getStyleClass().add("tag");

        Label tag2 = new Label("#JavaFX");
        tag2.getStyleClass().add("tag");

        Label fire = new Label("🔥");

        Label badge = new Label("Tendance");
        badge.getStyleClass().add("badge");

        tagsRow.getChildren().addAll(tag1, tag2, fire, badge);

        userInfo.getChildren().addAll(name, date, tagsRow);

// Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

// Ajouter les éléments au header (IMPORTANT ⭐)
        header.getChildren().addAll(avatar, userInfo, spacer);

// Bouton options (Seulement si c’est mon post)
        if (currentUser != null &&
                post.getAuteur() != null &&
                Objects.equals(post.getAuteur().getId(), currentUser.getId())) {

            Button btnOptions = new Button("⋯");
            btnOptions.getStyleClass().add("btn-icon");

            btnOptions.setOnAction(e -> showMenu(btnOptions, post));

            header.getChildren().add(btnOptions);
        }




        // ================= CONTENU =================
        Label contenu = new Label(post.getContenu());
        contenu.setWrapText(true);
        contenu.getStyleClass().add("text");

        card.getChildren().addAll(header, contenu);

        // ================= IMAGE (OPTIONNELLE) =================
        if (post.getImage() != null && !post.getImage().isBlank()) {

            File file = new File(post.getImage().trim());

            if (file.exists()) {

                StackPane mediaPane = new StackPane();
                mediaPane.getStyleClass().add("media");

                Image img = loadSafeImage(file);

                ImageView postImg = new ImageView(img);


                postImg.setFitWidth(680);
                postImg.setFitHeight(320);
                postImg.setPreserveRatio(true);
                postImg.getStyleClass().add("post-img");

                HBox overlay = new HBox();
                overlay.setAlignment(Pos.TOP_RIGHT);
                overlay.getStyleClass().add("media-overlay");

                Label duration = new Label("⏱ 1 min");
                duration.getStyleClass().add("pill");

                overlay.getChildren().add(duration);

                mediaPane.getChildren().addAll(postImg, overlay);

                card.getChildren().add(mediaPane);

            } else {
                System.out.println("Fichier image introuvable : " + post.getImage());
            }
        }


        // ================= FOOTER =================
        HBox footer = new HBox(18);
        footer.setAlignment(Pos.CENTER_LEFT);

        // LIKE
        HBox likeBox = new HBox(6);
        likeBox.setAlignment(Pos.CENTER_LEFT);

        Button likeBtn = new Button();
        likeBtn.getStyleClass().add("like-btn");

        ImageView likeIcon = new ImageView(HEART_EMPTY);


        if (likeService.isLikedByUser(currentUser, post)) {
            likeIcon.setImage(HEART_FULL);
        }
        likeIcon.setFitWidth(25);
        likeIcon.setFitHeight(25);
        likeIcon.setPreserveRatio(true);
        likeIcon.setSmooth(false);
        likeIcon.setCache(true);
        likeIcon.setCacheHint(CacheHint.SPEED);

        likeBtn.setGraphic(likeIcon);
        int nbLikes = likeService.getNbLikes(post);

        Label likes = new Label(String.valueOf(nbLikes));
        likes.getStyleClass().add("muted");
        likes.setOnMouseClicked(e -> handleLikes(post));
        likes.setStyle("-fx-cursor: hand;");
        likeBtn.setOnAction(e -> {

            if (likeIcon.getImage() == HEART_EMPTY) {

                likeService.addLike(currentUser, post);
                likeIcon.setImage(HEART_FULL);

            } else {

                likeService.deleteLike(currentUser, post);
                likeIcon.setImage(HEART_EMPTY);
            }

            int newCount = likeService.getNbLikes(post);
            likes.setText(String.valueOf(newCount));
        });

        likeBox.getChildren().addAll(likeBtn, likes);


        // COMMENT
        HBox commentBox = new HBox(6);
        commentBox.setAlignment(Pos.CENTER_LEFT);

        Button commentBtn = new Button();
        commentBtn.setStyle("-fx-background-color: transparent;");

        ImageView commentIcon = new ImageView(COMMENT_EMPTY);
        commentIcon.setFitWidth(18);
        commentIcon.setFitHeight(18);
        commentIcon.setPreserveRatio(true);
        commentIcon.setSmooth(false);
        commentIcon.setCache(true);
        commentIcon.setCacheHint(CacheHint.SPEED);

        commentBtn.setGraphic(commentIcon);

        commentBtn.setOnAction(e -> handleComment(post));


        int nbCommentaires =
                commentCounts.getOrDefault(post.getId(), 0);


        Label comments = new Label(String.valueOf(nbCommentaires));
        comments.getStyleClass().add("muted");


        commentBox.getChildren().addAll(commentBtn, comments);

        // STAR
        // ================= FAVORIS =================
        HBox starBox = new HBox(6);
        starBox.setAlignment(Pos.CENTER_LEFT);

        Button starBtn = new Button();
        starBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        starBtn.setPadding(Insets.EMPTY);

        ImageView favEmptyIcon = new ImageView(STAR_EMPTY);
        favEmptyIcon.setFitWidth(22);
        favEmptyIcon.setFitHeight(22);
        favEmptyIcon.setPreserveRatio(true);
        favEmptyIcon.setSmooth(false);
        favEmptyIcon.setCache(true);
        favEmptyIcon.setCacheHint(CacheHint.SPEED);

        ImageView favFullIcon = new ImageView(STAR_FULL);
        favFullIcon.setFitWidth(22);
        favFullIcon.setFitHeight(22);
        favFullIcon.setPreserveRatio(true);
        favFullIcon.setSmooth(false);
        favFullIcon.setCache(true);
        favFullIcon.setCacheHint(CacheHint.SPEED);

// vérifier si le post est déjà en favoris
        boolean isFav = favorisService.isFavori(currentUser, post);

        starBtn.setGraphic(isFav ? favFullIcon : favEmptyIcon);

// action click
        starBtn.setOnAction(e -> {

            boolean current = favorisService.isFavori(currentUser, post);

            if(current){
                favorisService.removeFavori(currentUser, post);
                starBtn.setGraphic(favEmptyIcon);
            }else{
                favorisService.addFavori(currentUser, post);
                starBtn.setGraphic(favFullIcon);
            }

        });

// Hover effect
        starBtn.setOnMouseEntered(e -> starBtn.setOpacity(0.7));
        starBtn.setOnMouseExited(e -> starBtn.setOpacity(1));

        starBox.getChildren().add(starBtn);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

// SHARE -----------------------------------------
        HBox shareBox = new HBox();
        shareBox.setAlignment(Pos.CENTER_RIGHT);

        Button shareBtn = new Button();
        shareBtn.setStyle("-fx-background-color: transparent;");

        ImageView shareIcon = new ImageView(SHARE_IMAGE);

        shareIcon.setFitWidth(25);
        shareIcon.setFitHeight(25);
        shareIcon.setPreserveRatio(true);
        shareIcon.setPreserveRatio(true);
        shareIcon.setSmooth(false);
        shareIcon.setCache(true);
        shareIcon.setCacheHint(CacheHint.SPEED);
        shareBtn.setGraphic(shareIcon);

        shareBox.getChildren().add(shareBtn);


        shareBtn.setOnAction(e -> sharePost(post));


        footer.getChildren().addAll(likeBox, commentBox, starBox, footerSpacer,shareBox);

        card.getChildren().add(footer);

        return card;
    }
    private Image loadSafeImage(File file) {
        return new Image(
                file.toURI().toString(),
                600,   // max width
                400,   // max height
                true,
                false,
                true
        );
    }

    private void sharePost(Post post){

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                try{

                    URL url = new URL("https://api.ayrshare.com/api/post");

                    HttpURLConnection conn =
                            (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");

                    conn.setRequestProperty(
                            "Authorization",
                            "Bearer FC9BE493-5E8E44EF-90E07EA9-447554F5"
                    );

                    conn.setRequestProperty(
                            "Content-Type",
                            "application/json"
                    );

                    conn.setDoOutput(true);

                    // -------- Construire JSON --------

                    StringBuilder json = new StringBuilder();

                    json.append("{");

                    // Post text
                    json.append("\"post\":\"")
                            .append(post.getContenu()
                                    .replace("\"","\\\"")
                                    .replace("\n"," "))
                            .append("\",");

                    // Platforms
                    json.append("\"platforms\":[\"twitter\",\"facebook\"]");
                    System.out.println("Post ID = " + post.getId());
                    System.out.println("Image = " + post.getImage());


                    // Image upload vers Cloudinary
                    if(post.getImage() != null && !post.getImage().isBlank()){

                        File file = new File(post.getImage());

                        String imageUrl = uploadToCloudinary(file);
                        System.out.println("Image URL retournée: " + imageUrl);

                        if(imageUrl != null){
                            json.append(",\"mediaUrls\":[\"")
                                    .append(imageUrl)
                                    .append("\"]");
                        }
                    }

                    json.append("}");

                    try(OutputStream os = conn.getOutputStream()){
                        os.write(json.toString().getBytes("UTF-8"));
                    }

                    int code = conn.getResponseCode();

                    System.out.println("Ayrshare HTTP Code: " + code);

                    InputStream is;

                    if(code >= 200 && code < 300){
                        is = conn.getInputStream();
                    } else {
                        is = conn.getErrorStream();
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while((line = br.readLine()) != null){
                        response.append(line);
                    }

                    System.out.println("Ayrshare Response: " + response.toString());
                    System.out.println("JSON envoyé à Ayrshare:");
                    System.out.println(json.toString());

                    if(code == 200 || code == 201){
                        Platform.runLater(() ->
                                showToast("Partagé avec succès 🚀"));
                    }else{
                        Platform.runLater(() ->
                                showToast("Erreur partage ❌"));
                    }


                }catch(Exception e){
                    e.printStackTrace();
                }

                return null;
            }
        };

        new Thread(task).start();
    }
    private String uploadToCloudinary(File file){

        try{

            System.out.println("===== UPLOAD CLOUDINARY START =====");

            if(file == null){
                System.out.println("File is NULL !");
                return null;
            }

            System.out.println("File path: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());

            String cloudName = "duz53i6eh"; // ton cloud name
            String uploadPreset = "xbfnsoct";

            URL url = new URL("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String boundary = "Boundary-" + System.currentTimeMillis();

            conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            OutputStream os = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(os, "UTF-8"), true);

            // ---- upload_preset ----
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
            writer.append(uploadPreset).append("\r\n");
            writer.flush();

            // ---- file ----
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getName()).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            Files.copy(file.toPath(), os);
            os.flush();

            writer.append("\r\n").flush();
            writer.append("--").append(boundary).append("--").append("\r\n");
            writer.close();

            int status = conn.getResponseCode();
            System.out.println("HTTP Status: " + status);

            InputStream is;

            if (status >= 200 && status < 300) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;

            while((line = br.readLine()) != null){
                response.append(line);
            }

            System.out.println("Cloudinary Response: " + response.toString());

            if(status >= 200 && status < 300){
                JSONObject json = new JSONObject(response.toString());
                String secureUrl = json.getString("secure_url");
                System.out.println("Uploaded Image URL: " + secureUrl);
                System.out.println("===== UPLOAD SUCCESS =====");
                return secureUrl;
            }else{
                System.out.println("===== UPLOAD FAILED =====");
                return null;
            }

        }catch(Exception e){
            System.out.println("===== EXCEPTION IN UPLOAD =====");
            e.printStackTrace();
            return null;
        }
    }



    private void handleComment(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CommentPopup.fxml"));
            Parent popup = loader.load();
            CommentPopupController controller = loader.getController();

            controller.setPost(post);
            mainContent.setOpacity(0.6);

            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            controller.setOnClose(() -> {
                mainContent.setOpacity(1);
                root.getChildren().remove(overlay);
                loadPosts();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public void handleFullScreen(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setFullScreen(!stage.isFullScreen());
    }



    private void handleDeletePost(Post post) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce post ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {

            if (response == ButtonType.OK) {
                postService.delete(post);
                postsList.remove(post);

                System.out.println("Post supprimé avec succès !");
                loadPosts();
            }
        });
    }
    private void showMenu(Button btn, Post post) {

        ContextMenu menu = new ContextMenu();

        ImageView editIcon = new ImageView(EDIT_IMAGE);
        editIcon.setFitWidth(16);
        editIcon.setFitHeight(16);
        editIcon.setPreserveRatio(true);
        editIcon.setSmooth(false);
        editIcon.setCache(true);
        editIcon.setCacheHint(CacheHint.SPEED);
        ImageView deleteIcon = new ImageView(DELETE_IMAGE);
        deleteIcon.setFitWidth(16);
        deleteIcon.setFitHeight(16);
        deleteIcon.setPreserveRatio(true);
        deleteIcon.setSmooth(false);
        deleteIcon.setCache(true);
        deleteIcon.setCacheHint(CacheHint.SPEED);


        MenuItem updateItem = new MenuItem("Modifier", editIcon);
        MenuItem deleteItem = new MenuItem("Supprimer", deleteIcon);

        // ACTION UPDATE
        updateItem.setOnAction(e -> handleUpdatePost(post));

        // ACTION DELETE
        deleteItem.setOnAction(e -> handleDeletePost(post));

        menu.getItems().addAll(updateItem, deleteItem);

        menu.show(btn, Side.BOTTOM, 0, 5);
    }

    @FXML
    private void handleChooseImage(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) root.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            System.out.println("Image sélectionnée : " + selectedImageFile.getAbsolutePath());
            showToast("Votre image a été sélectionnée !");
        }
    }

    private double getToxicityScore(String text) {
        try {
            URL url = new URL("http://localhost:5000/predict");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            String jsonInput = "{\"text\": \"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes("UTF-8"));
                os.flush();
            }

            int status = conn.getResponseCode();

            InputStream is = (status == 200) ? conn.getInputStream() : conn.getErrorStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            if (status != 200) {
                System.err.println("Erreur Flask: HTTP " + status + " → " + response);
                return 0;
            }

            JSONObject obj = new JSONObject(response.toString());
            return obj.getDouble("score");

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    private void notifyAdmin(String contenu, double score, Post post) {

        Personne auteur = post.getAuteur();

        String typeContenu = (post.getId() != 0) ? "post" : "contenu";

        String message =
                "👤 " + auteur.getNom() + " " + auteur.getPrenom() +
                        " a publié un " + typeContenu +
                        " jugé suspect (score: " + String.format("%.2f", score) + ")" +
                        " | Post ID: " + post.getId();
        Personne admin = personneService.findByRole("admin");

        notification notif = new notification(
                message,
                "POST",
                post.getId(),
                null,
                auteur.getId(),
                admin.getId()

        );

        notificationService.add(notif);
    }




    @FXML
    private void handleAddPost() {

        String contenu = txtNewPost.getText();

        if (contenu == null || contenu.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Contenu vide !");
            alert.show();
            return;
        }

        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Utilisateur non authentifié !");
            alert.show();
            return;
        }

        Task<Double> task = new Task<>() {
            @Override
            protected Double call() {
                return getToxicityScore(contenu);
            }
        };

        task.setOnSucceeded(event -> {

            double score = task.getValue();
            System.out.println("Score toxicité = " + score);

            if (score >= 0.7) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Publication refusée !");
                alert.setContentText("Contenu non autorisé.");
                alert.show();
                return;
            }

            savePost(contenu, score);
        });

        task.setOnFailed(event -> {
            System.out.println("Erreur appel IA");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    private void savePost(String contenu, double score) {

        String imagePath = null;

        // ================= IMAGE =================
        if (selectedImageFile != null) {

            try {

                // créer dossier si inexistant
                Files.createDirectories(Paths.get(UPLOAD_DIR));

                // nom unique
                String fileName = UUID.randomUUID()
                        + "_"
                        + selectedImageFile.getName();

                // destination réelle
                Path destination = Paths.get(UPLOAD_DIR, fileName);

                // copie image
                Files.copy(
                        selectedImageFile.toPath(),
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );

                // chemin enregistré en BD
                imagePath = destination.toAbsolutePath()
                        .toString()
                        .replace("\\", "/");

                System.out.println("Image copiée vers : "
                        + destination.toAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // ================= POST =================
        Post newPost = new Post(
                0,
                "",
                contenu,
                LocalDateTime.now(),
                0,
                currentUser,
                imagePath
        );

        Post savedPost = postService.addPost(newPost);

        if (savedPost == null) {
            System.out.println("Erreur sauvegarde post");
            return;
        }

        if (score >= 0.1) {
            notifyAdmin(contenu, score, savedPost);
            showToast("Contenu sensible publié (admin notifié)");
        } else {
            showToast("Publication publiée !");
        }

        Map<Integer, Integer> singleCount = new HashMap<>();
        singleCount.put(savedPost.getId(), 0);

        postsContainer.getChildren()
                .add(0, createPostCard(savedPost, singleCount));

        txtNewPost.clear();
        selectedImageFile = null;
    }


    private void showToast(String message) {
        Label toast = new Label(message);
        toast.getStyleClass().add("toast");
        toast.setStyle(
                "-fx-background-color: rgba(0,0,0,0.7);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 10px 20px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 14px;"
        );

        root.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);

        Timeline timeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(7),
                        e -> root.getChildren().remove(toast))
        );

        timeline.setCycleCount(1);
        timeline.play();
    }
    private void handleUpdatePost(Post post) {
        try {
            // Charger le FXML du popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/UpdatePostPopup.fxml"));
            Parent popup = loader.load();
            UpdatePostPopupController controller = loader.getController();
            controller.setPost(post);

            mainContent.setOpacity(0.6);

            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            controller.setOnClose(() -> {
                mainContent.setOpacity(1);
                root.getChildren().remove(overlay);

            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleLikes(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/LikesPopup.fxml"));
            Parent popup = loader.load();

            LikesPopupController controller = loader.getController();
            controller.setPost(post);

            mainContent.setOpacity(0.6);

            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            controller.setOnClose(() -> {
                mainContent.setOpacity(1);
                root.getChildren().remove(overlay);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleLocalisation(ActionEvent event) {

        if (selectedImageFile == null) {
            showToast("Veuillez d'abord sélectionner une image !");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                sendImageToLocalizationAPI(selectedImageFile);
                return null;
            }
        };

        new Thread(task).start();
    }
    private void sendImageToLocalizationAPI(File file) {

        try {

            URL url = new URL("http://localhost:5001/localize");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            String boundary = "Boundary-" + System.currentTimeMillis();
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

            OutputStream os = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(
                    new OutputStreamWriter(os, "UTF-8"), true);

            // ---- file ----
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"image\"; filename=\"")
                    .append(file.getName()).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream\r\n\r\n");
            writer.flush();

            Files.copy(file.toPath(), os);
            os.flush();

            writer.append("\r\n");
            writer.append("--").append(boundary).append("--\r\n");
            writer.close();

            int status = conn.getResponseCode();

            InputStream is = (status == 200)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            if (status == 200) {

                JSONObject json = new JSONObject(response.toString());

                String city = json.optString("city", "");
                String country = json.optString("country", "");

                Platform.runLater(() ->
                        showToastLong("📍 Votre photo est située à "
                                + city + ", " + country));

            } else {

                Platform.runLater(() ->
                        showToast("Erreur localisation ❌"));

            }

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() ->
                    showToast("Erreur serveur ❌"));
        }
    }
    private void showToastLong(String message) {

        Label toast = new Label(message);
        toast.setStyle(
                "-fx-background-color: rgba(0,0,0,0.8);" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 12px 25px;" +
                        "-fx-background-radius: 20;" +
                        "-fx-font-size: 14px;"
        );

        root.getChildren().add(toast);
        StackPane.setAlignment(toast, Pos.TOP_CENTER);

        Timeline timeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(10),
                        e -> root.getChildren().remove(toast))
        );

        timeline.setCycleCount(1);
        timeline.play();
    }

}





