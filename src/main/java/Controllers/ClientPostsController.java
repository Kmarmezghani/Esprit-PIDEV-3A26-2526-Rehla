package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import javafx.util.Duration;
import models.Personne;
import models.Post;
import models.notification;
import org.json.JSONObject;
import services.*;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.collections.ListChangeListener;
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
    private boolean isLoadingPosts = false;

    @FXML
    private VBox postsContainer;

    private boolean isStarred = false;

    private Image starEmpty;
    private Image starFull;
    Personne currentUser = Session.getCurrentUser();
    private File selectedImageFile;
    private ObservableList<Post> postsList = FXCollections.observableArrayList();
private PostService postService = new PostService();
   private notificationService notificationService = new notificationService();
    private PersonneService personneService = new PersonneService();
    private LikeService likeService = new LikeService();
   private CommentaireService commentService = new CommentaireService();
    private Image avatarImage;
    private Image shareImage;
    private Image editImage;
    private Image deleteImage;

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

        avatarImage = new Image(
                Objects.requireNonNull(getClass().getResource("/icons/usericon.png")).toExternalForm()
        );

        shareImage = new Image(
                Objects.requireNonNull(getClass().getResource("/icons/share.png")).toExternalForm()
        );

        editImage = new Image(
                Objects.requireNonNull(getClass().getResource("/icons/edit2.png")).toExternalForm()
        );

        deleteImage = new Image(
                Objects.requireNonNull(getClass().getResource("/icons/delete.png")).toExternalForm()
        );




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

        // ⭐ ID utilisé pour navigation notification
        card.setUserData(post.getId());

        // ================= HEADER =================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

// Avatar
        ImageView avatar = new ImageView(avatarImage);

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

                Image img = new Image(
                        file.toURI().toString(),
                        680,
                        0,
                        true,
                        true,
                        true
                );
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


        int nbCommentaires =
                commentCounts.getOrDefault(post.getId(), 0);


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

// SHARE -----------------------------------------
        HBox shareBox = new HBox();
        shareBox.setAlignment(Pos.CENTER_RIGHT);

        Button shareBtn = new Button();
        shareBtn.setStyle("-fx-background-color: transparent;");

        ImageView shareIcon = new ImageView(shareImage);

        shareIcon.setFitWidth(30);
        shareIcon.setFitHeight(30);
        shareIcon.setPreserveRatio(true);

        shareBtn.setGraphic(shareIcon);

        shareBox.getChildren().add(shareBtn);
        shareBtn.setOnAction(e -> sharePost(post));


        footer.getChildren().addAll(likeBox, commentBox, starBox, footerSpacer,shareBox);

        card.getChildren().add(footer);

        return card;
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
                postService.delete(post);
                postsList.remove(post);

                System.out.println("Post supprimé avec succès !");
                loadPosts();
            }
        });
    }
    private void showMenu(Button btn, Post post) {

        ContextMenu menu = new ContextMenu();

        ImageView editIcon = new ImageView(editImage);
        editIcon.setFitWidth(16);
        editIcon.setFitHeight(16);

        ImageView deleteIcon = new ImageView(deleteImage);
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


    private void loadNotifications() {

        if (currentUser == null) return;

        List<notification> list =
                notificationService.getByReceiver(currentUser.getId()); // admin

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

        Platform.runLater(() -> {

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(300),
                            e -> {

                                VBox postNode = findPostNodeById(n.getPostId());

                                if(postNode != null){

                                    scrollToNode(postNode);

                                    postNode.setStyle(
                                            "-fx-border-color:red;" +
                                                    "-fx-border-width:3px;" +
                                                    "-fx-border-radius:10;"
                                    );
                                }
                            })
            );

            timeline.play();

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

        String imagePath = selectedImageFile != null
                ? selectedImageFile.getAbsolutePath()
                : null;

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
                new KeyFrame(Duration.seconds(7),
                        e -> root.getChildren().remove(toast))
        );

        timeline.setCycleCount(1);
        timeline.play();
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

            controller.setOnClose(() -> {
                mainContent.setEffect(null);
                root.getChildren().remove(overlay);

                loadPosts();
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





