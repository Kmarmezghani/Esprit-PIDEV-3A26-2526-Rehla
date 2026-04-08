<?php

namespace App\Controller;

use App\Entity\Activite;
use App\Form\ActiviteType;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\String\Slugger\SluggerInterface;

final class AdminController extends AbstractController
{
    #[Route('/admin', name: 'app_admin')]
    public function index(): Response
    {
        return $this->render('admin/base_admin.html.twig', [
            'controller_name' => 'AdminController',
        ]);
    }

    #[Route('/admin/activite', name: 'activite_admin')]
    public function activiteList(EntityManagerInterface $em): Response
    {
        $activites = $em->getRepository(Activite::class)->findAll();

        return $this->render('admin/activite_admin.html.twig', [
            'activites' => $activites
        ]);
    }

    #[Route('/admin/activite/new', name: 'activite_new')]
    public function activiteNew(Request $request, EntityManagerInterface $em, SluggerInterface $slugger): Response
    {
        $activite = new Activite();

        $form = $this->createForm(ActiviteType::class, $activite, [
    'is_edit' => false,
    'show_max_places' => false,
]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {
            $imageFile = $form->get('image')->getData();

            if ($imageFile) {
               
                $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

                
                $destinationPath = $this->getParameter('activities_directory');

                try {
                    
                    $imageFile->move($destinationPath, $newFilename);
                } catch (\Exception $e) {
                    $this->addFlash('danger', 'Erreur lors de l\'upload de l\'image');
                }

               
                $physicalPath = $destinationPath . '\\' . $newFilename;
                $activite->setImage($physicalPath);
            }

            $em->persist($activite);
            $em->flush();

            $this->addFlash('success', 'Activité créée avec succès !');

            return $this->redirectToRoute('activite_admin');
        }

        return $this->render('admin/activite_new.html.twig', [
            'form' => $form->createView()
        ]);
    }
    #[Route('/admin/activite/{id}/edit', name: 'activite_edit')]
    public function edit(Request $request, Activite $activite, EntityManagerInterface $em, SluggerInterface $slugger): Response
    {
        $form = $this->createForm(ActiviteType::class, $activite, [
    'is_edit' => true,
    'show_max_places' => false,
]);
        $form->handleRequest($request);

        if ($form->isSubmitted() && $form->isValid()) {

            
            $imageFile = $form->get('image')->getData();

            if ($imageFile) {
                $originalFilename = pathinfo($imageFile->getClientOriginalName(), PATHINFO_FILENAME);
                $safeFilename = $slugger->slug($originalFilename);
                $newFilename = $safeFilename . '-' . uniqid() . '.' . $imageFile->guessExtension();

                $destinationPath = $this->getParameter('activities_directory');

                try {
                    $imageFile->move($destinationPath, $newFilename);
                } catch (\Exception $e) {
                    $this->addFlash('danger', 'Erreur lors de l\'upload de l\'image');
                }

                $physicalPath = $destinationPath . '\\' . $newFilename;
                $activite->setImage($physicalPath);
            }
            

            $em->flush(); 

            $this->addFlash('success', 'Activité modifiée avec succès !');

            return $this->redirectToRoute('activite_admin');
        }

        return $this->render('admin/activite_edit.html.twig', [
            'form' => $form->createView(),
            'activite' => $activite
        ]);
    }
    #[Route('/admin/activite/{id}/delete', name: 'activite_delete')]
    public function delete(Activite $activite, EntityManagerInterface $em): Response
    {
        $em->remove($activite);
        $em->flush();

        $this->addFlash('success', 'Activité supprimée avec succès !');

        return $this->redirectToRoute('activite_admin');
    }
}