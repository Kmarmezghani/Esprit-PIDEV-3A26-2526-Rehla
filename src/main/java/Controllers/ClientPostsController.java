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
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.control.MenuItem;

import javafx.scene.image.ImageView;
import javafx.scene.image.Image  ;

import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Personne;
import models.Post;
import services.PostService;

import java.io.File;
import java.time.LocalDate;

import javafx.collections.ListChangeListener;


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
    private VBox postsContainer;

    private boolean isStarred = false;

    private Image starEmpty;
    private Image starFull;

    private File selectedImageFile;
    private ObservableList<Post> postsList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {

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

    }

    private void loadPosts() {

        PostService postService = new PostService();
        postsList.setAll(postService.getAll());

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

        // ================= HEADER =================
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView avatar = new ImageView(
                new Image(getClass().getResource("/icons/usericon.png").toExternalForm())
        );
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.getStyleClass().add("avatar");

        VBox userInfo = new VBox(2);
        System.out.println("auteeeeeee" +post.getAuteur().getPrenom());

        // Nom auteur
        Label name = new Label(
                post.getAuteur().getPrenom() + " " +
                        post.getAuteur().getNom()
        );

        name.getStyleClass().add("name");

        // Date + Public (statique)
        Label date = new Label(post.getDatePublication() + " • Public");
        date.getStyleClass().add("muted");

        HBox tagsRow = new HBox(6);
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        // TAGS STATIQUES
        Label tag1 = new Label("#Design");
        tag1.getStyleClass().add("tag");

        Label tag2 = new Label("#JavaFX");
        tag2.getStyleClass().add("tag");

        Label fire = new Label("🔥");

        Label badge = new Label("Tendance");
        badge.getStyleClass().add("badge");

        tagsRow.getChildren().addAll(tag1, tag2, fire, badge);

        userInfo.getChildren().addAll(name, date, tagsRow);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnOptions = new Button("⋯");
        btnOptions.getStyleClass().add("btn-icon");
        btnOptions.setOnAction(e -> showMenu(btnOptions, post));

        header.getChildren().addAll(avatar, userInfo, spacer, btnOptions);

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
        likeIcon.setFitWidth(25);
        likeIcon.setFitHeight(25);
        likeIcon.setPreserveRatio(true);
        likeIcon.setSmooth(true);

        likeBtn.setGraphic(likeIcon);

        Label likes = new Label("3");
        likes.getStyleClass().add("muted");

        likeBtn.setOnAction(e -> {
            if (likeIcon.getImage() == heartEmpty) {
                likeIcon.setImage(heartFull);
            } else {
                likeIcon.setImage(heartEmpty);
            }
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

        Label comments = new Label("3");
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



    @FXML
    private void handleComment(Post event) {
        try {

            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource("/CommentPopup.fxml"));

            Parent popup = loader.load();
            CommentPopupController controller = loader.getController();

            // 🔥 Blur seulement sur le blog
            GaussianBlur blur = new GaussianBlur(20);
            mainContent.setEffect(blur);

            // 🔥 Fond sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

            // IMPORTANT: ne pas toucher la taille du popup
            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            root.getChildren().add(overlay);

            // 🔥 Fermeture
            controller.setOnClose(() -> {
                mainContent.setEffect(null);
                root.getChildren().remove(overlay);
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
    @FXML
    private void handleAddPost() {

        String contenu = txtNewPost.getText();

        if (contenu == null || contenu.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText("Contenu vide !");
            alert.setContentText("Veuillez écrire quelque chose.");
            alert.show();
            return;
        }

        Personne auteur = new Personne();
        auteur.setId(1);

        String imagePath = null;

        if (selectedImageFile != null) {
            imagePath = selectedImageFile.getAbsolutePath();
        }

        Post newPost = new Post(
                0,
                "",
                contenu,
                LocalDate.now(),
                0,
                auteur,
                imagePath
        );

        PostService postService = new PostService();
        postService.add(newPost);


        loadPosts();

        txtNewPost.clear();
        selectedImageFile = null;

        System.out.println("Post ajouté avec image !");
        showToast("Publication publiée !");
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
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> root.getChildren().remove(toast));
        }).start();
    }
    private void handleUpdatePost(Post post) {
        try {
            // Charger le FXML du popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UpdatePostPopup.fxml"));
            Parent popup = loader.load();

            // Récupérer le controller pour lui passer le post à éditer
            UpdatePostPopupController controller = loader.getController();
            controller.setPost(post); // méthode à créer dans le controller pour initialiser les champs

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
}





