package rehla;

import models.Activite;
import models.Avis;
import services.ActiviteService;
import services.AvisService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        ActiviteService activiteService = new ActiviteService();
        AvisService avisService = new AvisService();

        Activite activite1 = new Activite(
                "Excursion Désert",
                "Une journée dans le désert avec quads et dromadaires.",
                120.0,
                5.5,
                "Aventure",
                3 // guideId
        );
        activiteService.add(activite1);


        List<Activite> activites = activiteService.getAll();
        int activiteId = activites.get(activites.size() - 1).getId();

        Avis avis1 = new Avis(5, "Super activité, très amusante!", Date.valueOf(LocalDate.now()), activiteId, 1);
        Avis avis2 = new Avis(4, "Très bon guide et expérience", Date.valueOf(LocalDate.now()), activiteId, 2);

        avisService.add(avis1);
        avisService.add(avis2);


        List<Avis> avisList = avisService.getAvisByActivite(activiteId);

        avis1.setId(avisList.get(0).getId());
        avis2.setId(avisList.get(1).getId());


        activites = activiteService.getAll();
        System.out.println("\n--- Activities after adding reviews ---");
        for (Activite a : activites) {
            System.out.println(a.getNom() + " | Note moyenne: " + a.getNoteMoyenne());
        }

        avis1.setNote(3);
        avis1.setCommentaire("C'était bien mais un peu fatigant.");
        avisService.update(avis1);

        System.out.println("\n--- Activities after updating a review ---");
        activites = activiteService.getAll();
        for (Activite a : activites) {
            System.out.println(a.getNom() + " | Note moyenne: " + a.getNoteMoyenne());
        }

        avisService.delete(avis2);

        System.out.println("\n--- Activities after deleting a review ---");
        activites = activiteService.getAll();
        for (Activite a : activites) {
            System.out.println(a.getNom() + " | Note moyenne: " + a.getNoteMoyenne());
        }

        System.out.println("\n--- Reviews for activity ---");
        avisList = avisService.getAvisByActivite(activiteId);
        for (Avis a : avisList) {
            System.out.println("Note: " + a.getNote() + " | Commentaire: " + a.getCommentaire());
        }
    }
}
