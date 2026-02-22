package Controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
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
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Personne;
import models.Post;
import models.notification;
import org.json.JSONObject;
import services.CommentaireService;
import services.LikeService;
import services.PostService;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javafx.collections.ListChangeListener;
import services.notificationService;
import util.Session;


public class ClientPostsController {


    @FXML
    private BorderPane mainContent;

    @FXML
    private StackPane root;   // ajoute fx:id="root" au StackPane

    @FXML
    private TextField txtNewPost;


    private boolean isLiked = false;

    private Image heartEmpty;
    private Image heartFull;
    @FXML private ImageView commentIcon;

    private Image commentEmpty;

    @FXML
    private Button btnNotif;

    private ContextMenu notifMenu = new ContextMenu();

    @FXML
    private VBox postsContainer;

    private boolean isStarred = false;

    private Image starEmpty;
    private Image starFull;
    Personne currentUser = Session.getCurrentUser();
    private File selectedImageFile;
    private ObservableList<Post> postsList = FXCollections.observableArrayList();
PostService postService = new PostService();
    notificationService notificationService = new notificationService();
    private LikeService likeService = new LikeService();
    @FXML
    private void initialize() {
        if(currentUser == null ||
                currentUser.getRole() == null ||
                !currentUser.getRole().equalsIgnoreCase("admin")) {

            btnNotif.setVisible(false);
            btnNotif.setManaged(false);
        }

        heartEmpty = new Image(getClass().getResourceAsStream("/icons/heartwhite.png"));
        heartFull = new Image(getClass().getResourceAsStream("/icons/HeartRed.png"));


        commentEmpty = new Image(getClass()
                .getResourceAsStream("/icons/comment.png"));

        starEmpty = new Image(getClass().getResourceAsStream("/icons/whiteStar.png"));
        starFull = new Image(getClass().getResourceAsStream("/icons/yellowStar.png"));


        postsList.addListener((ListChangeListener<Post>) change -> {
            refreshUI();
        });


        loadPosts();
        postsContainer.setFillWidth(true);
        btnNotif.setOnAction(e -> toggleNotifications());


    }
    private void toggleNotifications() {

        if (notifMenu.isShowing()) {
            notifMenu.hide();
            return;
        }

        loadNotifications();
        notifMenu.show(btnNotif, Side.BOTTOM, 0, 8);
    }


    private void loadPosts() {

        PostService postService = new PostService();

        postsList.setAll(
                postService.getAll()
                        .stream()
                        .sorted((p1, p2) ->
                                p2.getDatePublication()
                                        .compareTo(p1.getDatePublication())
                        )
                        .toList()
        );

        refreshUI();
    }

    private void refreshUI() {

        postsContainer.getChildren().clear();

        for (Post post : postsList) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    private VBox createPostCard(Post post) {

        VBox card = new VBox(10);
        card.getStyleClass().addAll("post", "glass");

        // ⭐ ID utilisé pour navigation notification
        card.setUserData(post.getId());

        // ================= HEADER =================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

// Avatar
        ImageView avatar = new ImageView(
                new Image(getClass()
                        .getResource("/icons/usericon.png")
                        .toExternalForm())
        );
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
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

            File file = new File(post.getImage());

            if (file.exists()) {

                StackPane mediaPane = new StackPane();
                mediaPane.getStyleClass().add("media");

                ImageView postImg = new ImageView(
                        new Image(file.toURI().toString())
                );

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

        ImageView likeIcon = new ImageView(heartEmpty);


        if (likeService.isLikedByUser(currentUser, post)) {
            likeIcon.setImage(heartFull);
        }
        likeIcon.setFitWidth(25);
        likeIcon.setFitHeight(25);
        likeIcon.setPreserveRatio(true);
        likeIcon.setSmooth(true);

        likeBtn.setGraphic(likeIcon);
        int nbLikes = likeService.getNbLikes(post);

        Label likes = new Label(String.valueOf(nbLikes));
        likes.getStyleClass().add("muted");
        likes.setOnMouseClicked(e -> handleLikes(post));
        likes.setStyle("-fx-cursor: hand;");
        likeBtn.setOnAction(e -> {

            if (likeIcon.getImage() == heartEmpty) {

                likeService.addLike(currentUser, post);
                likeIcon.setImage(heartFull);

            } else {

                likeService.deleteLike(currentUser, post);
                likeIcon.setImage(heartEmpty);
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

        ImageView commentIcon = new ImageView(commentEmpty);
        commentIcon.setFitWidth(21);
        commentIcon.setFitHeight(21);

        commentBtn.setGraphic(commentIcon);

        commentBtn.setOnAction(e -> handleComment(post));

        CommentaireService commentService = new CommentaireService();
        int nbCommentaires = commentService.countByPost(post.getId());

        Label comments = new Label(String.valueOf(nbCommentaires));
        comments.getStyleClass().add("muted");


        commentBox.getChildren().addAll(commentBtn, comments);

        // STAR
        HBox starBox = new HBox(6);
        starBox.setAlignment(Pos.CENTER_LEFT);

        Button starBtn = new Button();
        starBtn.setStyle("-fx-background-color: transparent;");

        ImageView starIcon = new ImageView(starEmpty);
        starIcon.setFitWidth(18);
        starIcon.setFitHeight(18);

        starBtn.setGraphic(starIcon);

        starBtn.setOnAction(e -> {
            if (starIcon.getImage() == starEmpty) {
                starIcon.setImage(starFull);
            } else {
                starIcon.setImage(starEmpty);
            }
        });

        starBox.getChildren().add(starBtn);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label views = new Label("Vue par 128 personnes");
        views.getStyleClass().add("muted");

        footer.getChildren().addAll(likeBox, commentBox, starBox, footerSpacer, views);

        card.getChildren().add(footer);

        return card;
    }



    private void handleComment(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CommentPopup.fxml"));
            Parent popup = loader.load();
            CommentPopupController controller = loader.getController();

            // ⚡ Important : passer le post pour charger les commentaires
            controller.setPost(post);

            // 🔥 Blur seulement sur le blog
            GaussianBlur blur = new GaussianBlur(20);
            mainContent.setEffect(blur);

            // 🔥 Fond sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            controller.setOnClose(() -> {
                mainContent.setEffect(null);
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

                PostService postService = new PostService();

                // 🔥 Suppression en base
                postService.delete(post);

                // 🔥 Suppression de la liste observable
                postsList.remove(post);

                System.out.println("Post supprimé avec succès !");
            }
        });
    }
    private void showMenu(Button btn, Post post) {

        ContextMenu menu = new ContextMenu();

        ImageView editIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/icons/edit2.png"))
        );
        editIcon.setFitWidth(16);
        editIcon.setFitHeight(16);

        ImageView deleteIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/icons/delete.png"))
        );
        deleteIcon.setFitWidth(16);
        deleteIcon.setFitHeight(16);

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

            // Encodage JSON sûr
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

        notification notif = new notification(
                message,
                "POST",
                post.getId(),
                null,
                auteur.getId(),
                1   // admin
        );

        notificationService.add(notif);
    }


    private void loadNotifications() {

        List<notification> list = notificationService.getByReceiver(1); // admin

        notifMenu.getItems().clear();
        notifMenu.getStyleClass().add("notif-dropdown");

        MenuItem header = new MenuItem("Notifications");
        header.setDisable(true);
        header.getStyleClass().add("notif-header");

        notifMenu.getItems().add(header);
        notifMenu.getItems().add(new SeparatorMenuItem());

        if (list.isEmpty()) {

            MenuItem empty = new MenuItem("Aucune notification");
            empty.setDisable(true);
            empty.getStyleClass().add("notif-item");
            notifMenu.getItems().add(empty);

        } else {

            for (notification n : list) {

                MenuItem item = new MenuItem(n.getMessage());
                item.getStyleClass().add("notif-item");

                item.setOnAction(e -> {

                    handleNotificationClick(n);

                });

                notifMenu.getItems().add(item);
            }

        }
    }
    private void handleNotificationClick(notification n){

        loadPosts();

        javafx.application.Platform.runLater(() -> {

            // ⭐ Si notification concerne un post
            if(n.getPostId() != null){

                VBox postNode = findPostNodeById(n.getPostId());

                if(postNode != null){
                    scrollToNode(postNode);

                    postNode.setStyle(
                            "-fx-border-color:red;" +
                                    "-fx-border-width:3px;"
                    );
                }
            }

        });
    }


    private void scrollToNode(Node node){

        ScrollPane scroll =
                (ScrollPane) postsContainer
                        .getScene()
                        .lookup(".scroll");

        double height = postsContainer.getHeight();
        double y = node.getLayoutY();

        scroll.setVvalue(y / height);
    }

    private VBox findPostNodeById(int postId){

        for(Node node : postsContainer.getChildren()){

            if(node.getUserData() != null &&
                    node.getUserData().equals(postId)){
                return (VBox) node;
            }
        }

        return null;
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

        double score = getToxicityScore(contenu);
        System.out.println("Score toxicité = " + score);

        if (score >= 0.7) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Publication refusée !");
            alert.setContentText("Contenu non autorisé.");
            alert.show();
            return;
        }



        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Utilisateur non authentifié !");
            alert.show();
            return;
        }


        String imagePath = null;
        if (selectedImageFile != null) {
            imagePath = selectedImageFile.getAbsolutePath();
        }

        Post newPost = new Post(
                0,
                "",
                contenu,
                LocalDateTime.now(),
                0,
                currentUser,
                imagePath
        );

        PostService postService = new PostService();

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

        loadPosts();

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

        // Faire disparaître après 2 secondes
        new Thread(() -> {
            try {
                Thread.sleep(7000);
            } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> root.getChildren().remove(toast));
        }).start();
    }
    private void handleUpdatePost(Post post) {
        try {
            // Charger le FXML du popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UpdatePostPopup.fxml"));
            Parent popup = loader.load();
            UpdatePostPopupController controller = loader.getController();
            controller.setPost(post);

            // 🔥 Blur sur le blog
            GaussianBlur blur = new GaussianBlur(20);
            mainContent.setEffect(blur);

            // 🔥 Fond sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            // 🔥 Fermeture
            controller.setOnClose(() -> {
                mainContent.setEffect(null);
                root.getChildren().remove(overlay);
                loadPosts(); // rafraîchir la liste après modification
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLikes(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LikesPopup.fxml"));
            Parent popup = loader.load();

            LikesPopupController controller = loader.getController();
            controller.setPost(post);


            GaussianBlur blur = new GaussianBlur(20);
            mainContent.setEffect(blur);

            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            controller.setOnClose(() -> {
                mainContent.setEffect(null);
                root.getChildren().remove(overlay);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void goToProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/blogProfileView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(
                    getClass().getResource("/css/blog_styles.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}





