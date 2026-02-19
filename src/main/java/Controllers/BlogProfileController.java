package Controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import models.Personne;
import models.Post;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private final List<Post> allPosts = new ArrayList<>();


    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    private void initialize() {
        initProfileInfo();
        initSamplePosts();
        renderPosts(allPosts);

        publishButton.setOnAction(e -> handlePublish());
        followButton.setOnAction(e -> toggleFollow());
        initTabs();
    }

    private void initProfileInfo() {
        // Tu peux remplacer par tes vraies images
        Image avatarImage = new Image(
                getClass().getResource("/icons/usericon.png").toExternalForm(),
                120, 120, true, true
        );
        Image smallAvatar = new Image(
                getClass().getResource("/icons/usericon.png").toExternalForm(),
                36, 36, true, true
        );

        profileAvatar.setImage(avatarImage);
        currentUserAvatar.setImage(smallAvatar);
        composerAvatar.setImage(smallAvatar);

        profileName.setText("Yosr Amamou");
        profileHeadline.setText("Développeuse Full-Stack • Tech Blogger • Cloud & IA Enthusiast");
        currentUserName.setText("Yosr");
        postsCountLabel.setText("18");
        followersCountLabel.setText("1.2K");
        favoritesCountLabel.setText("87");
    }

    private void initSamplePosts() {
        allPosts.add(new Post(
                "Yosr Amamou",
                "Comment j’ai construit mon premier blog JavaFX",
                "Dans cet article, je partage les étapes et les astuces pour créer une interface de blog moderne en JavaFX..."
        ));
        allPosts.add(new Post(
                "Yosr Amamou",
                "Déployer une application Spring Boot sur le cloud",
                "Aujourd’hui, on va voir comment déployer simplement une appli Spring Boot sur un provider cloud en quelques minutes..."
        ));
        allPosts.add(new Post(
                "Yosr Amamou",
                "Mes outils favoris pour booster la productivité",
                "Entre VS Code, IntelliJ, Notion et quelques extensions magiques, voici ma stack d’outils pour rester focus et efficace..."
        ));
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

        ImageView avatar = new ImageView(profileAvatar.getImage());
        avatar.setFitWidth(32);
        avatar.setFitHeight(32);

        VBox authorBox = new VBox(2);
        Label authorLabel = new Label("auteur");
        authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Label dateLabel = new Label(dateFormatter.format(LocalDate.now()));
        dateLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 11px;");
        authorBox.getChildren().addAll(authorLabel, dateLabel);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button moreButton = new Button("⋮");
        moreButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #757575;");

        header.getChildren().addAll(avatar, authorBox, headerSpacer, moreButton);

        // Titre
        Label titleLabel = new Label("post1");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);

        // Contenu
        Text contentText = new Text("content1 ");
        contentText.wrappingWidthProperty().bind(card.widthProperty().subtract(24));
        contentText.setFill(Color.web("#424242"));
        contentText.setStyle("-fx-font-size: 13px;");

        // Footer avec like / comment / fav
        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 0, 0, 0));

        Button likeButton = new Button();
        likeButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        Label likesLabel = new Label();
        likesLabel.setStyle("-fx-text-fill: #616161; -fx-font-size: 11px;");




        Button commentButton = new Button("💬 Commenter");
        commentButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #616161;"
                + "-fx-font-size: 12px; -fx-cursor: hand;");



        Button favButton = new Button();
        favButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");




        footer.getChildren().addAll(likeButton, likesLabel, commentButton, favButton);

        card.getChildren().addAll(header, titleLabel, contentText, footer);

        VBox.setMargin(card, new Insets(0, 4, 0, 4));
        return card;
    }


    private void handlePublish() {
        String title = newPostTitleField.getText().trim();
        String content = newPostContentArea.getText().trim();

        if (title.isEmpty() || content.isEmpty()) {
            showInfoDialog("Publication", "Merci de remplir le titre et le contenu.");
            return;
        }

        Post post = new Post(profileName.getText(), title, content);
        allPosts.add(0, post); // en haut de la liste
        renderPosts(allPosts);

        newPostTitleField.clear();
        newPostContentArea.clear();
        postsCountLabel.setText(String.valueOf(Integer.parseInt(postsCountLabel.getText()) + 1));
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

        tabFavorites.setOnAction(e -> {
            tabFavorites.setSelected(true);
            tabPosts.setSelected(false);
            tabAbout.setSelected(false);


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



    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(root.getScene().getWindow());
        alert.showAndWait();
    }
}
