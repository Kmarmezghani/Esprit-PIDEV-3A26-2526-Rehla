package Controllers;

import javafx.application.Platform;
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
import services.*;
import util.Session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BlogProfileController {

    @FXML private BorderPane root;
    @FXML private Button btnProfile;

    @FXML private ContextMenu profileMenu;
    @FXML private ImageView profileAvatar;

    @FXML private ImageView composerAvatar;

    @FXML private Label profileName;
    @FXML private Label profileHeadline;
    @FXML private Label postsCountLabel;
    @FXML private Label followersCountLabel;
    @FXML private Label favoritesCountLabel;


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
    private NotificationService notificationService = new NotificationService();
    @FXML
    private StackPane stackRoot;   // racine pour overlay
    private LikeService likeService = new LikeService();
    Personne currentUser = Session.getCurrentUser();// utilisateur connecté
    @FXML
    private Button imageButton;
    @FXML
    private VBox newPostBox;
    @FXML private Label lblNotifCount;
    @FXML private TextField searchField;
    @FXML private Button btnNotif;
    @FXML
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

        initProfileInfo();

        renderPosts(allPosts);

        publishButton.setOnAction(e -> handlePublish());
        followButton.setOnAction(e -> toggleFollow());
        initTabs();

        loadPostsFromDatabase();

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
    @FXML public void goToactivities(ActionEvent event) { }
    @FXML public void goToMyProfile(ActionEvent event) { }
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
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        stage.setScene(new Scene(root));
        stage.show();
    } catch (Exception e) {
        e.printStackTrace();
        showInfo("Profile", "Could not open profile page.");
    }
}
    @FXML public void goToMyActivities(ActionEvent event) { switchScene(event, "/Frontoffice/MyActivitiesPage.fxml"); }

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
        System.out.println("renderPosts called, children in postsContainer: " + postsContainer.getChildren().size());
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
        composerAvatar.setImage(smallAvatar);
        profileName.setText(currentUser.getNom() + " " + currentUser.getPrenom());
        profileName.setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 80px;"); // On force le noir ici

        profileHeadline.setText(
                currentUser.getRole() != null ? currentUser.getRole() : "Utilisateur"
        );
        profileHeadline.setStyle("-fx-text-fill: #000000; -fx-font-size: 20px;");

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

        ImageView favEmpty = getIcon(ICON_FAV_EMPTY, 24);
        ImageView favFull  = getIcon(ICON_FAV_FULL, 24);

        boolean isFav = favorisService.isFavori(currentUser, post);

        favButton.setGraphic(isFav ? favFull : favEmpty);

        favButton.setOnAction(e -> {

            boolean current = favorisService.isFavori(currentUser, post);

            if(current){
                favorisService.removeFavori(currentUser, post);
                favButton.setGraphic(favEmpty);
            }else{
                favorisService.addFavori(currentUser, post);
                favButton.setGraphic(favFull);
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
            postService.add(post);

            // Recharge depuis la DB (plus propre que allPosts.add)
            loadPostsFromDatabase();

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

        List<Post> favPosts = favorisService.getFavorisPosts(currentUser.getId());

        renderPosts(favPosts);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/updatePostPopup.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Frontoffice/ProfilePage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
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
