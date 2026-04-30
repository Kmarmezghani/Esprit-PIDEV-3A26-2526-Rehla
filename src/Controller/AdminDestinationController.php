<?php

namespace App\Controller;

use App\Entity\Pays;
use App\Entity\Ville;
use App\Entity\Attraction;
use App\Form\PaysType;
use App\Form\VilleType;
use App\Form\AttractionType;
use App\Repository\PaysRepository;
use App\Repository\VilleRepository;
use App\Repository\AttractionRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/admin/destination')]
class AdminDestinationController extends AbstractController
{
    #[Route('', name: 'admin_destination_index')]
    public function index(
        Request $request,
        PaysRepository $paysRepo,
        VilleRepository $villeRepo,
        AttractionRepository $attractionRepo
    ): Response {
        $tab = $request->query->get('tab', 'pays');
        $search = $request->query->get('search');
        $sort = $request->query->get('sort', 'nom');
        $direction = $request->query->get('direction', 'asc');

        // Pays filters
        $visitMin = $request->query->get('visitMin') !== '' && $request->query->get('visitMin') !== null ? (int) $request->query->get('visitMin') : null;
        $visitMax = $request->query->get('visitMax') !== '' && $request->query->get('visitMax') !== null ? (int) $request->query->get('visitMax') : null;

        // Ville filters
        $saison = $request->query->get('saison');
        $typeTourisme = $request->query->get('typeTourisme');

        // Attraction filters
        $type = $request->query->get('type');
        $prixMin = $request->query->get('prixMin') !== '' && $request->query->get('prixMin') !== null ? (float) $request->query->get('prixMin') : null;
        $prixMax = $request->query->get('prixMax') !== '' && $request->query->get('prixMax') !== null ? (float) $request->query->get('prixMax') : null;
        $estFerme = $request->query->get('estFerme');
        $estFermeBool = $estFerme === '1' ? true : ($estFerme === '0' ? false : null);

        return $this->render('admin/destination_admin.html.twig', [
            'pays' => ($tab === 'pays') ? $paysRepo->searchAndSort($search, $sort, $direction, $visitMin, $visitMax) : $paysRepo->searchAndSort(null, 'nom', 'asc'),
            'villes' => ($tab === 'ville') ? $villeRepo->searchAndSort($search, $sort, $direction, $saison, $typeTourisme) : $villeRepo->searchAndSort(null, 'nom', 'asc'),
            'attractions' => ($tab === 'attraction') ? $attractionRepo->searchAndSort($search, $sort, $direction, $type, $prixMin, $prixMax, $estFermeBool) : $attractionRepo->searchAndSort(null, 'nom', 'asc'),
            'selectedTab' => $tab,
            'search' => $search,
            'sort' => $sort,
            'direction' => $direction,
            // Filter values for the form
            'visitMin' => $visitMin,
            'visitMax' => $visitMax,
            'saison' => $saison,
            'typeTourisme' => $typeTourisme,
            'type' => $type,
            'prixMin' => $prixMin,
            'prixMax' => $prixMax,
            'estFerme' => $estFerme,
        ]);
    }

    // --- PAYS ACTIONS ---
    #[Route('/pays/new', name: 'admin_pays_new')]
    #[Route('/pays/{id}/edit', name: 'admin_pays_edit')]
    public function paysForm(Request $request, EntityManagerInterface $em, ?Pays $pays = null): Response
    {
        $pays = $pays ?? new Pays();
        $form = $this->createForm(PaysType::class, $pays);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Guarantee we get the image from POST data even if unmapped
            $postData = $request->request->all('pays');
            $img = $postData['image'] ?? '';

            if ($img !== '') {
                $pays->setImage($img);
            }
            
            $em->persist($pays);
            $em->flush();
            return $this->redirectToRoute('admin_destination_index', ['tab' => 'pays']);
        }

        if ($pays->getId()) {
            return $this->render('admin/pays_edit.html.twig', [
                'form' => $form->createView(),
                'pays' => $pays
            ]);
        }
        return $this->render('admin/pays_new.html.twig', [
            'form' => $form->createView()
        ]);
    }

    #[Route('/pays/{id}/delete', name: 'admin_pays_delete', methods: ['POST'])]
    public function paysDelete(Request $request, Pays $pays, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $pays->getId(), $request->request->get('_token'))) {
            $em->remove($pays);
            $em->flush();
            $this->addFlash('success', 'Pays supprimé !');
        }
        return $this->redirectToRoute('admin_destination_index', ['tab' => 'pays']);
    }

    // --- VILLE ACTIONS ---
    #[Route('/ville/new', name: 'admin_ville_new')]
    #[Route('/ville/{id}/edit', name: 'admin_ville_edit')]
    public function villeForm(Request $request, EntityManagerInterface $em, ?Ville $ville = null): Response
    {
        $ville = $ville ?? new Ville();
        $form = $this->createForm(VilleType::class, $ville);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            // Guarantee we get the image from POST data even if unmapped
            $postData = $request->request->all('ville');
            $img = $postData['image'] ?? '';

            if ($img !== '') {
                $ville->setImage($img);
            }
            
            $em->persist($ville);
            $em->flush();
            return $this->redirectToRoute('admin_destination_index', ['tab' => 'ville']);
        }

        if ($ville->getId()) {
            return $this->render('admin/ville_edit.html.twig', [
                'form' => $form->createView(),
                'ville' => $ville
            ]);
        }
        return $this->render('admin/ville_new.html.twig', [
            'form' => $form->createView()
        ]);
    }

    #[Route('/ville/{id}/delete', name: 'admin_ville_delete', methods: ['POST'])]
    public function villeDelete(Request $request, Ville $ville, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $ville->getId(), $request->request->get('_token'))) {
            $em->remove($ville);
            $em->flush();
            $this->addFlash('success', 'Ville supprimée !');
        }
        return $this->redirectToRoute('admin_destination_index', ['tab' => 'ville']);
    }

    // --- ATTRACTION ACTIONS ---
    #[Route('/attraction/new', name: 'admin_attraction_new')]
    #[Route('/attraction/{id}/edit', name: 'admin_attraction_edit')]
    public function attractionForm(Request $request, EntityManagerInterface $em, ?Attraction $attraction = null): Response
    {
        $attraction = $attraction ?? new Attraction();
        $form = $this->createForm(AttractionType::class, $attraction);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $em->persist($attraction);
            $em->flush();
            $this->addFlash('success', 'Attraction enregistrée !');
            return $this->redirectToRoute('admin_destination_index', ['tab' => 'attraction']);
        }

        if ($attraction->getId()) {
            return $this->render('admin/attraction_edit.html.twig', [
                'form' => $form->createView(),
                'attraction' => $attraction
            ]);
        }
        return $this->render('admin/attraction_new.html.twig', [
            'form' => $form->createView()
        ]);
    }

    #[Route('/attraction/{id}/delete', name: 'admin_attraction_delete', methods: ['POST'])]
    public function attractionDelete(Request $request, Attraction $attraction, EntityManagerInterface $em): Response
    {
        if ($this->isCsrfTokenValid('delete' . $attraction->getId(), $request->request->get('_token'))) {
            $em->remove($attraction);
            $em->flush();
            $this->addFlash('success', 'Attraction supprimée !');
        }
        return $this->redirectToRoute('admin_destination_index', ['tab' => 'attraction']);
    }

    // --- AJAX API ENDPOINTS ---

    #[Route('/api/countries/search', name: 'admin_api_countries_search', methods: ['GET'])]
    public function searchCountries(Request $request): Response
    {
        $query = $request->query->get('q', '');
        
        if (strlen($query) < 2) {
            return $this->json(['countries' => []]);
        }

        $countryApi = new \App\Service\CountryApiService();
        $countries = $countryApi->searchCountries($query);
        
        return $this->json(['countries' => $countries]);
    }

    #[Route('/api/cities/search', name: 'admin_api_cities_search', methods: ['GET'])]
    public function searchCities(Request $request): Response
    {
        $country = $request->query->get('country', '');
        $query = $request->query->get('q', '');
        
        if (empty($country) || strlen($query) < 2) {
            return $this->json(['cities' => []]);
        }

        $cityApi = new \App\Service\CityApiService();
        $cities = $cityApi->searchCities($country, $query);
        
        return $this->json(['cities' => $cities]);
    }

    #[Route('/api/cities/country', name: 'admin_api_cities_by_country', methods: ['GET'])]
    public function getCitiesByCountry(Request $request): Response
    {
        $country = $request->query->get('country', '');
        
        if (empty($country)) {
            return $this->json(['cities' => []]);
        }

        $cityApi = new \App\Service\CityApiService();
        $cities = $cityApi->getCitiesByCountry($country);
        
        return $this->json(['cities' => $cities]);
    }

    #[Route('/api/ai/generate-description', name: 'admin_api_ai_description', methods: ['POST'])]
    public function generateAiDescription(Request $request, \App\Service\DestinationGeminiService $aiService): Response
    {
        $data = json_decode($request->getContent(), true);
        $country = $data['country'] ?? '';
        
        if (empty($country)) {
            return $this->json(['error' => 'Veuillez d\'abord sélectionner ou entrer un nom de pays.'], 400);
        }
        try {
            $description = $aiService->generateForCountry($country);
            return $this->json(['description' => $description]);
        } catch (\Exception $e) {
            return $this->json(['error' => $e->getMessage()], 500);
        }
    }

    #[Route('/api/ai/suggest-ville-profile', name: 'admin_api_ai_suggest_ville', methods: ['POST'])]
    public function suggestVilleProfile(Request $request, \App\Service\DestinationGeminiService $aiService): Response
    {
        $data = json_decode($request->getContent(), true);
        $ville = $data['ville'] ?? '';
        
        if (empty($ville)) {
            return $this->json(['error' => 'Veuillez entrer un nom de ville.'], 400);
        }
        
        try {
            $suggestion = $aiService->suggestCityProfile($ville);
            if (isset($suggestion['error'])) {
                return $this->json(['error' => $suggestion['error']], 500);
            }
            return $this->json($suggestion);
        } catch (\Exception $e) {
            return $this->json(['error' => $e->getMessage()], 500);
        }
    }

    #[Route('/api/generate-image', name: 'admin_api_generate_image', methods: ['POST'])]
    public function generateImage(Request $request, \App\Service\UnsplashService $unsplashService, EntityManagerInterface $em, \Symfony\Component\DependencyInjection\ParameterBag\ParameterBagInterface $params): Response
    {
        $data = json_decode($request->getContent(), true);
        $type = $data['type'] ?? ''; // 'pays' or 'ville'
        $id = $data['id'] ?? null;
        $name = $data['name'] ?? '';

        if (empty($name)) {
            return $this->json(['error' => 'Veuillez entrer un nom pour la recherche.'], 400);
        }

        // Use Unsplash to find and download an image
        $filename = $unsplashService->fetchAndSaveImage($name, $name);

        if (!$filename) {
            return $this->json(['error' => 'Erreur lors de la génération de l\'image (Vérifiez votre clé API Unsplash).'], 500);
        }

        // If we have an ID, save it to the database immediately
        if ($id) {
            $entity = ($type === 'pays') 
                ? $em->getRepository(Pays::class)->find($id) 
                : $em->getRepository(Ville::class)->find($id);

            if ($entity) {
                // DELETE OLD IMAGE FROM DISK
                $oldImage = $entity->getImage();
                if ($oldImage) {
                    $projectDir = $params->get('kernel.project_dir');
                    $oldFilePath = $projectDir . '/public/uploads/destinations/' . $oldImage;
                    if (file_exists($oldFilePath)) {
                        unlink($oldFilePath);
                    }
                }

                $entity->setImage($filename);
                $em->flush();
            }
        }

        return $this->json([
            'filename' => $filename,
            'url' => '/uploads/destinations/' . $filename
        ]);
    }
}
