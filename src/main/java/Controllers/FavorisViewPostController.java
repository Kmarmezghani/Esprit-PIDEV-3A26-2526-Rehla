package Controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.CacheHint;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javafx.stage.Stage;
import models.Personne;
import models.Post;
import services.CommentaireService;
import services.LikeService;
import services.PostService;
import util.Session;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FavorisViewPostController {
    @FXML
    private ToggleButton tabFavorites;

    @FXML
    private ToggleButton tabPosts;
    @FXML private BorderPane root;

    @FXML private ImageView profileAvatar;
    @FXML private ImageView currentUserAvatar;


    @FXML private Label profileName;
    @FXML private Label profileHeadline;
    @FXML private Label postsCountLabel;
    @FXML private Label followersCountLabel;
    @FXML private Label favoritesCountLabel;
    @FXML private Label currentUserName;

    @FXML private ToggleButton tabAbout;

    @FXML private VBox postsContainer;
    @FXML private Button followButton;

    private final List<Post> allPosts = new ArrayList<>();
    private PostService postService = new PostService();
    private CommentaireService commentaireService = new CommentaireService();
    @FXML private Button fullscreenButton;
    @FXML private Button closeButton;
    @FXML
    private StackPane stackRoot;   // racine pour overlay
    private LikeService likeService = new LikeService();
    Personne currentUser = Session.getCurrentUser();// utilisateur connecté
    @FXML
    private Button imageButton;
    // ton contenu principal
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private String selectedImagePath = null;
    private static final Image ICON_LIKE_EMPTY =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/blackHeart.png").toExternalForm());

    private static final Image ICON_LIKE_FULL =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/HeartRed.png").toExternalForm());

    private static final Image ICON_COMMENT =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/commentB.png").toExternalForm());

    private static final Image ICON_FAV_EMPTY =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/blackStar.png").toExternalForm());

    private static final Image ICON_FAV_FULL =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/yellowStar.png").toExternalForm());

    private static final Image ICON_EDIT =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/editblue.png").toExternalForm());

    private static final Image ICON_DELETE =
            new Image(BlogProfileController.class.getResource("/Backoffice/icons/delete.png").toExternalForm());


    @FXML
    private void initialize() {
        closeButton.setOnAction(e -> {
            root.setCache(false);
            root.setEffect(null);
            Stage stage = (Stage) root.getScene().getWindow();
            stage.close();
        });

        // Fullscreen : bascule la fenêtre en plein écran
        fullscreenButton.setOnAction(e -> {
            Stage stage = (Stage) root.getScene().getWindow();
            stage.setFullScreen(!stage.isFullScreen());
        });
        initProfileInfo();

        renderPosts(allPosts);

        followButton.setOnAction(e -> toggleFollow());
        initTabs();

        loadPostsFromDatabase();

    }
    private void loadPostsFromDatabase() {

        if (currentUser == null) return;

        allPosts.clear();

        List<Post> postsFromDB =
                postService.getPostsByPersonne(currentUser);

        // 🔥 Tri par date publication + heure (le plus récent en premier)
        allPosts.addAll(
                postsFromDB.stream()
                        .sorted((p1, p2) ->
                                p2.getDatePublication()
                                        .compareTo(p1.getDatePublication()))
                        .toList()
        );

        renderPosts(allPosts);

        postsCountLabel.setText(String.valueOf(allPosts.size()));
    }



    private void initProfileInfo() {
        // Tu peux remplacer par tes vraies images
        Image avatarImage = new Image(
                getClass().getResource("/Backoffice/icons/usericon.png").toExternalForm(),
                120, 120, true, true
        );
        Image smallAvatar = new Image(
                getClass().getResource("/Backoffice/icons/usericon.png").toExternalForm(),
                36, 36, true, true
        );

        profileAvatar.setImage(avatarImage);
        currentUserAvatar.setImage(smallAvatar);
        profileHeadline.setText(
                currentUser.getRole() != null
                        ? currentUser.getRole()
                        : "Utilisateur"
        );
        profileName.setText(currentUser.getNom() + " " + currentUser.getPrenom());



        currentUserName.setText(currentUser.getPrenom());
        postsCountLabel.setText("18");
        followersCountLabel.setText("1.2K");
        favoritesCountLabel.setText("87");
    }



    private void renderPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        for (Post post : posts) {
            postsContainer.getChildren().add(createPostCard(post));
        }
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10;"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12,0,0,2);");

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Image profileImg = profileAvatar.getImage();
        ImageView avatar = new ImageView();
        if(profileImg != null) {
            avatar.setImage(profileImg);
            avatar.setFitWidth(32);
            avatar.setFitHeight(32);
        } else {
            System.err.println("Profile avatar non chargé !");
        }

        VBox authorBox = new VBox(2);

        Label authorLabel = new Label(post.getAuteur().getNom() + " " + post.getAuteur().getPrenom());


        authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Label dateLabel = new Label(
                post.getDatePublication().format(dateFormatter)
        );
        dateLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");

        authorBox.getChildren().addAll(authorLabel, dateLabel);

// ================= DELETE & UPDATE =================------------------------------------------------------------------

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

// HBox pour Update/Delete
        HBox buttonsBox = new HBox(4);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        if (post.getAuteur() != null && currentUser != null &&
                post.getAuteur().getId() == currentUser.getId()) {

            Button updateButton = new Button();
            updateButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            updateButton.setPadding(Insets.EMPTY);
            updateButton.setGraphic(getIcon(ICON_EDIT, 18));
            updateButton.setOnAction(e -> handleUpdatePost(post));

            Button deleteButton = new Button();
            deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            deleteButton.setPadding(Insets.EMPTY);
            deleteButton.setGraphic(getIcon(ICON_DELETE, 18));
            deleteButton.setOnAction(e -> {

                boolean confirm = confirmDialog(
                        "Supprimer le post",
                        "Voulez-vous vraiment supprimer ce post ?"
                );

                if (confirm) {
                    postService.delete(post);
                    allPosts.remove(post);
                    renderPosts(allPosts);
                    postsCountLabel.setText(String.valueOf(allPosts.size()));
                }
            });

            buttonsBox.getChildren().addAll(updateButton, deleteButton);
        }

        Button moreButton = new Button("⋮");
        moreButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #757575;");
        moreButton.setPadding(Insets.EMPTY);

// Ajouter tous au header
        header.getChildren().addAll(avatar, authorBox, spacer, buttonsBox, moreButton);


        // ------------------------------Titre------------------------------------------------------------------------------------
        Label titleLabel = new Label(post.getTitre());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        Text contentText = new Text(post.getContenu());
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(24));
        contentText.setFill(Color.web("#424242"));
        contentText.setStyle("-fx-font-size: 13px;");



        // Footer avec like / comment / fav
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 0, 0, 0));
        card.getChildren().addAll(header, titleLabel, contentText);


        // 👉 IMAGE CENTRÉE
        if (post.getImage() != null && !post.getImage().isEmpty()) {

            try {
                File file = new File(post.getImage());

                if (file.exists()) {

                    Image image = new Image(
                            file.toURI().toString(),
                            500, 0,      // largeur max 500
                            true,        // preserve ratio
                            true,        // smooth
                            true         // background loading
                    );

                    ImageView postImage = new ImageView(image);
                    postImage.setPreserveRatio(true);

                    HBox imageBox = new HBox(postImage);
                    imageBox.setAlignment(Pos.CENTER);

                    card.getChildren().add(imageBox);
                }

            } catch (Exception ex) {
                System.err.println("Erreur chargement image : " + ex.getMessage());
            }
        }



        card.getChildren().add(footer);

        // ================= LIKE =================
        Button likeButton = new Button();
        likeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        likeButton.setPadding(Insets.EMPTY);

// 🔥 On crée les ImageView UNE seule fois
        ImageView likeEmpty = getIcon(ICON_LIKE_EMPTY, 24);
        ImageView likeFull  = getIcon(ICON_LIKE_FULL, 24);

        boolean isLiked = likeService.isLikedByUser(currentUser, post);
        likeButton.setGraphic(isLiked ? likeFull : likeEmpty);

        int nbLikes = likeService.getNbLikes(post);

        Label likesLabel = new Label(String.valueOf(nbLikes));
        likesLabel.setStyle("-fx-text-fill: #616161; -fx-font-size: 11px;");
        likesLabel.setPadding(Insets.EMPTY);

        HBox likeContainer = new HBox(4, likeButton, likesLabel);
        likeContainer.setAlignment(Pos.CENTER_LEFT);

// 🔥 Gestion du clic propre
        likeButton.setOnAction(e -> {

            boolean currentlyLiked = likeButton.getGraphic() == likeFull;

            if (currentlyLiked) {
                likeService.deleteLike(currentUser, post);
                likeButton.setGraphic(likeEmpty);
            } else {
                likeService.addLike(currentUser, post);
                likeButton.setGraphic(likeFull);
            }

            int newCount = likeService.getNbLikes(post);
            likesLabel.setText(String.valueOf(newCount));
        });

// ================= COMMENT =================

        Button commentButton = new Button();
        commentButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        commentButton.setPadding(Insets.EMPTY);

// 🔥 Utilise l'image statique déjà chargée
        ImageView commentIcon = getIcon(ICON_COMMENT, 22);

        commentButton.setGraphic(commentIcon);

// Nombre de commentaires
        int nbCommentaires = commentaireService.countByPost(post.getId());

        Label commentsLabel = new Label(String.valueOf(nbCommentaires));
        commentsLabel.setStyle("-fx-text-fill: #616161; -fx-font-size: 11px;");

        HBox commentContainer = new HBox(4, commentButton, commentsLabel);
        commentContainer.setAlignment(Pos.CENTER_LEFT);

// Action
        commentButton.setOnAction(e -> handleComment(post));


// ================= FAVORIS =================
        Button favButton = new Button();
        favButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        favButton.setPadding(Insets.EMPTY);

// 🔥 ImageView créés à partir des images statiques
        ImageView favEmpty = getIcon(ICON_FAV_EMPTY, 24);
        ImageView favFull  = getIcon(ICON_FAV_FULL, 24);

        favButton.setGraphic(favEmpty);

// Etat local (à remplacer plus tard par un vrai service si besoin)
        final boolean[] favorited = { false };

        favButton.setOnAction(e -> {

            favorited[0] = !favorited[0];

            if (favorited[0]) {
                favButton.setGraphic(favFull);
            } else {
                favButton.setGraphic(favEmpty);
            }
        });

// Hover effect
        likeButton.setOnMouseEntered(e -> likeButton.setOpacity(0.7));
        likeButton.setOnMouseExited(e -> likeButton.setOpacity(1));

        favButton.setOnMouseEntered(e -> favButton.setOpacity(0.7));
        favButton.setOnMouseExited(e -> favButton.setOpacity(1));

        footer.getChildren().addAll(
                likeContainer,
                commentContainer,
                favButton
        );

        VBox.setMargin(card, new Insets(0, 4, 0, 4));
        return card;

    }


    private boolean confirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(root.getScene().getWindow());

        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }



    private void toggleFollow() {
        if ("S'abonner".equals(followButton.getText())) {
            followButton.setText("Abonné(e)");
            followButton.setStyle("-fx-background-color: #C8E6C9; -fx-text-fill: #2E7D32;"
                    + "-fx-font-weight: bold; -fx-background-radius: 20;");
        } else {
            followButton.setText("S'abonner");
            followButton.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white;"
                    + "-fx-font-weight: bold; -fx-background-radius: 20;");
        }
    }

    private void initTabs() {
        tabPosts.setOnAction(e -> {
            tabPosts.setSelected(true);
            tabFavorites.setSelected(false);
            tabAbout.setSelected(false);
            renderPosts(allPosts);
        });


        tabAbout.setOnAction(e -> {
            tabAbout.setSelected(true);
            tabPosts.setSelected(false);
            tabFavorites.setSelected(false);
            postsContainer.getChildren().clear();

            Label aboutTitle = new Label("À propos de ce blog");
            aboutTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

            Text aboutText = new Text(
                    "Bienvenue sur mon univers de blogs ! Ici je partage mes expériences en développement " +
                            "Full-Stack, mes projets Java/JavaFX, mes découvertes en IA, ainsi que des conseils " +
                            "pour les étudiants en informatique.\n\n" +
                            "N’hésitez pas à liker, commenter et ajouter en favoris les articles qui vous inspirent."
            );
            aboutText.setWrappingWidth(520);
            aboutText.setStyle("-fx-font-size: 13px;");

            VBox box = new VBox(10, aboutTitle, aboutText);
            box.setPadding(new Insets(12));
            box.setStyle("-fx-background-color: white; -fx-background-radius: 10;"
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12,0,0,2);");
            postsContainer.getChildren().add(box);
        });
    }
    private ImageView getIcon(Image image, double size) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }


    private void handleUpdatePost(Post post) {
        try {
            // Charger le FXML du popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/updatePostPopup.fxml"));
            Parent popup = loader.load();

            // Récupérer le controller pour lui passer le post à éditer
            UpdatePostPopupController controller = loader.getController();
            controller.setPost(post); // initialise les champs du popup

            // 🔥 Blur sur le blog
            root.setCache(true);
            root.setCacheHint(CacheHint.SPEED);
            GaussianBlur blur = new GaussianBlur(10);
            root.setEffect(blur);

            // 🔥 Fond sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            overlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            stackRoot.getChildren().add(overlay);


            controller.setOnClose(() -> {
                root.setEffect(null);
                stackRoot.getChildren().remove(overlay);
                loadPostsFromDatabase();
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleComment(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/CommentPopup.fxml"));
            Parent popup = loader.load();

            CommentPopupController controller = loader.getController();
            controller.setPost(post);

            // 🔥 Blur du profil
            GaussianBlur blur = new GaussianBlur(20);
            root.setEffect(blur);

            // 🔥 Overlay sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            overlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            stackRoot.getChildren().add(overlay);

            controller.setOnClose(() -> {
                root.setEffect(null);
                stackRoot.getChildren().remove(overlay);
                loadPostsFromDatabase(); // refresh si besoin
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void setActiveTab(String tab) {
//
//        tabFavorites.setSelected(false);
//        tabPosts.setSelected(false);
//
//        switch (tab) {
//            case "favorites":
//                tabFavorites.setSelected(true);
//                break;
//
//            case "posts":
//                tabPosts.setSelected(true);
//                break;
//        }
//    }

//    @FXML
//    private void goToBlogProfile() {
//        System.out.println("clicckkkkkkkk");
//
//        tabFavorites.setSelected(false);
//        tabPosts.setSelected(true);
//        tabAbout.setSelected(false);
//
//        try {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/BlogProfileView.fxml"));
//            Parent root = loader.load();
//
//            BlogProfileController controller = loader.getController();
//            Platform.runLater(() -> controller.setActiveTab2("posts"));
//
//            Stage stage = (Stage) tabFavorites.getScene().getWindow();
//            Scene scene = new Scene(root);
//
//            scene.getStylesheets().add(
//                    getClass().getResource("/css/blog_styles.css").toExternalForm()
//            );
//
//            stage.setScene(scene);
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}
