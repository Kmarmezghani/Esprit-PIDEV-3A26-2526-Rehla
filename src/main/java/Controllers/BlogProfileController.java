package Controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Personne;
import models.Post;
import services.CommentaireService;
import services.FavorisService;
import services.LikeService;
import services.PostService;
import util.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BlogProfileController {

    @FXML private BorderPane root;

    @FXML private ImageView profileAvatar;
    @FXML private ImageView currentUserAvatar;
    @FXML private ImageView composerAvatar;

    @FXML private Label profileName;
    @FXML private Label profileHeadline;
    @FXML private Label postsCountLabel;
    @FXML private Label followersCountLabel;
    @FXML private Label favoritesCountLabel;
    @FXML private Label currentUserName;

    @FXML private TextField searchField;
    @FXML private ToggleButton tabPosts;
    @FXML private ToggleButton tabFavorites;
    @FXML private ToggleButton tabAbout;

    @FXML private TextField newPostTitleField;
    @FXML private TextArea newPostContentArea;
    @FXML private Button publishButton;
    @FXML private VBox postsContainer;
    @FXML private Button followButton;
    @FXML
    private VBox contentArea;
    private final List<Post> allPosts = new ArrayList<>();
    private PostService postService = new PostService();
    private CommentaireService commentaireService = new CommentaireService();
    private FavorisService favorisService = new FavorisService();
    @FXML private Button fullscreenButton;
    @FXML private Button closeButton;
    @FXML
    private StackPane stackRoot;   // racine pour overlay
    private LikeService likeService = new LikeService();
    Personne currentUser;// utilisateur connecté
    @FXML
    private Button imageButton;
    @FXML
    private VBox newPostBox;
       // ton contenu principal
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final Map<Post, VBox> postCardMap = new HashMap<>();
    private String selectedImagePath = null;
    private static final Image ICON_LIKE_EMPTY =
            new Image(BlogProfileController.class.getResource("/icons/blackHeart.png").toExternalForm());

    private static final Image ICON_LIKE_FULL =
            new Image(BlogProfileController.class.getResource("/icons/HeartRed.png").toExternalForm());

    private static final Image ICON_COMMENT =
            new Image(BlogProfileController.class.getResource("/icons/commentB.png").toExternalForm());

    private static final Image ICON_FAV_EMPTY =
            new Image(BlogProfileController.class.getResource("/icons/blackStar.png").toExternalForm());

    private static final Image ICON_FAV_FULL =
            new Image(BlogProfileController.class.getResource("/icons/yellowStar.png").toExternalForm());

    private static final Image ICON_EDIT =
            new Image(BlogProfileController.class.getResource("/icons/editblue.png").toExternalForm());

    private static final Image ICON_DELETE =
            new Image(BlogProfileController.class.getResource("/icons/delete.png").toExternalForm());
    private static final Image AVATAR =
            new Image(BlogProfileController.class.getResource("/icons/usericon.png").toExternalForm());

    private static final ImageView LIKE_EMPTY_ICON = new ImageView(ICON_LIKE_EMPTY);
    private static final ImageView LIKE_FULL_ICON  = new ImageView(ICON_LIKE_FULL);
    private static final ImageView COMMENT_ICON    = new ImageView(ICON_COMMENT);
    private static final ImageView FAV_EMPTY_ICON  = new ImageView(ICON_FAV_EMPTY);
    private static final ImageView FAV_FULL_ICON   = new ImageView(ICON_FAV_FULL);
    private static final ImageView AVATAR_ICON     = new ImageView(AVATAR);

    private final Map<Integer, Integer> likesCountMap = new HashMap<>();
    private final Map<Integer, Boolean> likedByUserMap = new HashMap<>();
    private final Map<Integer, Integer> commentsCountMap = new HashMap<>();
    private final Map<Integer, Boolean> favByUserMap = new HashMap<>();

    // Méthode utilitaire pour cloner et redimensionner
    private ImageView cloneIcon(ImageView template, double size){
        ImageView iv = new ImageView(template.getImage());
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setPreserveRatio(true);
        return iv;
    }


    @FXML
    private void initialize() {

        currentUser = Session.getCurrentUser();
        System.out.println("USER ID = " + currentUser.getId());
        if (currentUser == null) {
            System.out.println("ERREUR: currentUser est NULL");
            return;
        }
        closeButton.setOnAction(e -> {
            root.setOpacity(1);
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

        publishButton.setOnAction(e -> handlePublish());
        followButton.setOnAction(e -> toggleFollow());
        initTabs();

        loadPostsFromDatabase();
        postsContainer.setAlignment(Pos.TOP_CENTER);
        postsContainer.setFillWidth(false);

    }
    private void loadPostsFromDatabase() {

        if (currentUser == null) return;

        allPosts.clear();
        likesCountMap.clear();
        likedByUserMap.clear();
        commentsCountMap.clear();
        favByUserMap.clear();

        List<Post> postsFromDB = postService.getPostsByPersonne(currentUser);

        allPosts.addAll(
                postsFromDB.stream()
                        .sorted((p1, p2) ->
                                p2.getDatePublication()
                                        .compareTo(p1.getDatePublication()))
                        .toList()
        );

        // 🔥 Préchargement des données UNE SEULE FOIS
        for (Post post : allPosts) {

            int postId = post.getId();

            likesCountMap.put(postId,
                    likeService.getNbLikes(post));

            likedByUserMap.put(postId,
                    likeService.isLikedByUser(currentUser, post));

            commentsCountMap.put(postId,
                    commentaireService.countByPost(postId));

            favByUserMap.put(postId,
                    favorisService.isFavori(currentUser, post));
        }

        renderPosts(allPosts);

        postsCountLabel.setText(String.valueOf(allPosts.size()));
    }



    private void initProfileInfo() {
        profileAvatar.setImage(AVATAR);
        currentUserAvatar.setImage(AVATAR);
        composerAvatar.setImage(AVATAR);
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



//    private void renderPosts(List<Post> posts) {
//
//        postsContainer.getChildren().clear();
//
//        if(posts == null) return;
//
//        for(Post post : posts){
//            if(post != null){
//                postsContainer.getChildren().add(createPostCard(post));
//            }
//        }
//    }


    private void renderPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        postCardMap.clear();

        for(Post post : posts){
            if(post != null){
                VBox card = createPostCard(post);
                postsContainer.getChildren().add(card);
                postCardMap.put(post, card);
            }
        }
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-width: 1;"
        );
           // largeur préférée
        card.setAlignment(Pos.TOP_LEFT);
         // évite les recalculs layout
        card.setMaxWidth(1000);
        card.setPrefWidth(1000);
        VBox.setMargin(card, new Insets(15,0,15,0));
        card.setMinWidth(550);
        card.setCache(true);
        card.setCacheHint(CacheHint.SPEED);


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

                    // 🔥 Mise à jour locale
                    allPosts.remove(post);

                    VBox card2 = postCardMap.remove(post);
                    if (card2 != null) {
                        postsContainer.getChildren().remove(card);
                    }

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
        card.setSpacing(10);


        // 👉 IMAGE CENTRÉE
        if (post.getImage() != null && !post.getImage().isEmpty()) {

            try {
                File file = new File(post.getImage());

                if (file.exists()) {

                    Image img = loadSafeImage(file);

                    ImageView postImage = new ImageView(img);
                    postImage.setCache(true);
                    postImage.setFitWidth(400);
                    postImage.setPreserveRatio(true);
                    postImage.setCacheHint(CacheHint.SPEED);


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

// ✅ créer 2 icônes propres
        ImageView likeEmpty = cloneIcon(LIKE_EMPTY_ICON, 24);
        ImageView likeFull  = cloneIcon(LIKE_FULL_ICON, 24);

        int postId = post.getId();

        boolean isLiked = likedByUserMap.getOrDefault(postId, false);
        int nbLikes = likesCountMap.getOrDefault(postId, 0);
        likeButton.setGraphic(isLiked ? likeFull : likeEmpty);
        Label likesLabel = new Label(String.valueOf(nbLikes));
        likesLabel.setStyle("-fx-text-fill: #616161; -fx-font-size: 11px;");

// container
        HBox likeContainer = new HBox(4, likeButton, likesLabel);
        likeContainer.setAlignment(Pos.CENTER_LEFT);

        final boolean[] liked = { isLiked };

        likeButton.setOnAction(e -> {

            if (liked[0]) {

                likeService.deleteLike(currentUser, post);

                liked[0] = false;
                likeButton.setGraphic(likeEmpty);

                int newCount = Integer.parseInt(likesLabel.getText()) - 1;
                likesLabel.setText(String.valueOf(newCount));
                likesCountMap.put(postId, newCount);
                likedByUserMap.put(postId, false);

            } else {

                likeService.addLike(currentUser, post);

                liked[0] = true;
                likeButton.setGraphic(likeFull);

                int newCount = Integer.parseInt(likesLabel.getText()) + 1;
                likesLabel.setText(String.valueOf(newCount));
                likesCountMap.put(postId, newCount);
                likedByUserMap.put(postId, true);
            }
        });
// ================= COMMENT =================

        Button commentButton = new Button();
        commentButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        commentButton.setPadding(Insets.EMPTY);

        commentButton.setGraphic(cloneIcon(COMMENT_ICON, 22));


// Nombre de commentaires
        int nbCommentaires = commentsCountMap.getOrDefault(postId, 0);

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


        ImageView favEmpty = cloneIcon(FAV_EMPTY_ICON, 24);
        ImageView favFull  = cloneIcon(FAV_FULL_ICON, 24);

        boolean isFav = favByUserMap.getOrDefault(postId, false);
        favButton.setGraphic(isFav ? favFull : favEmpty);

        final boolean[] isFavState = { isFav };

        favButton.setOnAction(e -> {

            if (isFavState[0]) {

                favorisService.removeFavori(currentUser, post);

                isFavState[0] = false;
                favButton.setGraphic(favEmpty);
                favByUserMap.put(postId, false);

            } else {

                favorisService.addFavori(currentUser, post);

                isFavState[0] = true;
                favButton.setGraphic(favFull);
                favByUserMap.put(postId, true);
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
        footer.setCache(true);
        header.setCache(true);

        VBox.setMargin(card, new Insets(0, 4, 0, 4));
        return card;

    }

    private Image loadSafeImage(File file) {
        return new Image(
                file.toURI().toString(),
                600,   // max width
                400,   // max height
                true,
                true,
                false
        );

    }
    private boolean confirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(root.getScene().getWindow());

        return alert.showAndWait().filter(response -> response == ButtonType.OK).isPresent();
    }

    private void handlePublish() {

        String title = newPostTitleField.getText() != null
                ? newPostTitleField.getText().trim()
                : "";

        String content = newPostContentArea.getText() != null
                ? newPostContentArea.getText().trim()
                : "";

        if (title.isEmpty() || content.isEmpty()) {
            showInfoDialog("Publication", "Merci de remplir le titre et le contenu.");
            return;
        }

        try {

            Post post = new Post();
            post.setTitre(title);
            post.setContenu(content);
            post.setDatePublication(LocalDateTime.now());
            post.setAuteur(currentUser);   // important si tu utilises la DB

            // Si une image a été choisie
            if (selectedImagePath != null) {
                post.setImage(selectedImagePath);
            }

            // Sauvegarde en base
            post = postService.addPost(post);

            allPosts.add(0, post); // en haut de la liste

// 🔥 Mettre à jour cache
            int postId = post.getId();

            likesCountMap.put(postId, 0);
            likedByUserMap.put(postId, false);
            commentsCountMap.put(postId, 0);
            favByUserMap.put(postId, false);

// 🔥 Créer la carte UNE SEULE FOIS
            VBox card = createPostCard(post);

            postsContainer.getChildren().add(0, card);
            postCardMap.put(post, card);

            postsCountLabel.setText(String.valueOf(allPosts.size()));

            // Reset champs
            newPostTitleField.clear();
            newPostContentArea.clear();
            selectedImagePath = null;

        } catch (Exception e) {
            e.printStackTrace();
            showInfoDialog("Erreur", "Une erreur est survenue lors de la publication.");
        }
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
        tabPosts.setOnAction(e -> switchTab("posts"));
        tabFavorites.setOnAction(e -> switchTab("favorites"));
        tabAbout.setOnAction(e -> switchTab("about"));

        // onglet par défaut
        switchTab("posts");
    }
    private void showPosts() {
        renderPosts(allPosts);
    }

    private void showFavorites() {

        List<Post> favPosts =
                favorisService.getFavorisPosts(currentUser.getId());

        if(favPosts == null){
            favPosts = new ArrayList<>();
        }

        Set<Integer> favIds = new HashSet<>();

        for(Post p : favPosts){
            favIds.add(p.getId());
        }

        postsContainer.getChildren().forEach(node -> {

            if(node instanceof VBox card){

                Post post = getPostFromCard(card);

                if(post != null){
                    card.setVisible(favIds.contains(post.getId()));
                    card.setManaged(favIds.contains(post.getId()));
                }

            }

        });
    }
    private Post getPostFromCard(VBox card){

        return postCardMap.entrySet()
                .stream()
                .filter(e -> e.getValue() == card)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }



    private void showAbout() {
        Label aboutLabel = new Label("Section About");
        contentArea.getChildren().add(aboutLabel);
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



//            GaussianBlur blur = new GaussianBlur(10);
//            root.setEffect(blur);

            StackPane overlay = new StackPane();
            // avant d'ajouter overlay, retire l'ancien
            stackRoot.getChildren().removeIf(node -> node.getStyle().contains("rgba(0,0,0,0.5)"));
            stackRoot.getChildren().add(overlay);
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            overlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            stackRoot.getChildren().add(overlay);


            controller.setOnClose(() -> {


                stackRoot.getChildren().remove(overlay);

                VBox oldCard = postCardMap.get(post);

                if (oldCard != null) {
                    int index = postsContainer.getChildren().indexOf(oldCard);

                    VBox newCard = createPostCard(post);

                    postsContainer.getChildren().set(index, newCard);
                    postCardMap.put(post, newCard);
                }
            });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }

    private void handleComment(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/CommentPopup.fxml"));
            Parent popup = loader.load();

            CommentPopupController controller = loader.getController();
            controller.setPost(post);

            // 🔥 Blur du profil
//            GaussianBlur blur = new GaussianBlur(20);
//            root.setEffect(blur);

            // 🔥 Overlay sombre
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
            overlay.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
            overlay.getChildren().add(popup);
            StackPane.setAlignment(popup, Pos.CENTER);

            stackRoot.getChildren().add(overlay);

            controller.setOnClose(() -> {

                root.setOpacity(1);
                stackRoot.getChildren().remove(overlay);

                // 🔥 Update seulement le compteur commentaire
                int postId = post.getId();

                int newCount = commentaireService.countByPost(postId);
                commentsCountMap.put(postId, newCount);

                VBox card = postCardMap.get(post);

                if (card != null) {
                    int index = postsContainer.getChildren().indexOf(card);

                    VBox newCard = createPostCard(post);

                    postsContainer.getChildren().set(index, newCard);
                    postCardMap.put(post, newCard);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleChooseImage() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) root.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            System.out.println("Image sélectionnée : " + selectedImagePath);

            showInfoDialog("Image", "Image sélectionnée avec succès !");
        }
    }
    private void switchTab(String tab) {

        if(currentUser == null) return;

        tabPosts.setSelected(false);
        tabFavorites.setSelected(false);
        tabAbout.setSelected(false);

        postsContainer.setVisible(false);
        newPostBox.setVisible(false);

        switch (tab) {

            case "posts":

                tabPosts.setSelected(true);

                newPostBox.setVisible(true);
                postsContainer.setVisible(true);

                showPosts();
                break;

            case "favorites":

                tabFavorites.setSelected(true);

                newPostBox.setVisible(false);
                postsContainer.setVisible(true);

                showFavorites();
                break;

            case "about":

                tabAbout.setSelected(true);

                postsContainer.getChildren().clear();

                Label aboutLabel = new Label("Section About");
                contentArea.getChildren().setAll(aboutLabel);

                break;
        }
    }

    // Profile Menu Actions
    @FXML
    void goToMyProfileUpdate(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProfilePage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Profile", "Could not open profile page.");
        }
    }
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
