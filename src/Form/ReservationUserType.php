<?php

namespace App\Form;

use App\Entity\Reservation;
use App\Entity\Ville;
use App\Entity\Ticket;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
class ReservationUserType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('dateDebut', DateType::class, [
                'widget' => 'single_text',
                'label' => 'Date Début',
            ])
            ->add('dateFin', DateType::class, [
                'widget' => 'single_text',
                'label' => 'Date Fin',
            ])
            ->add('statut', ChoiceType::class, [
        'choices' => [
            'Confirmée' => 'confirmée',
            'réservée' => 'réservée',
            'Annulée' => 'annulée',
        ],
        'label' => 'Statut',

        // ✅ ONLY control edit/new here
        'disabled' => !$options['is_edit'],

        'attr' => [
            'class' => 'form-select'
        ],
    ])
            ->add('destination', EntityType::class, [
                'class' => Ville::class,
                'choice_label' => 'nom',
                'placeholder' => 'Sélectionner une destination',
                'label' => 'Destination',
            ])
            ->add('tickets', EntityType::class, [
    'class' => Ticket::class,
    'choice_label' => function (Ticket $ticket) {
        return $ticket->getType().' - '.$ticket->getPrix().' TND';
    },
    'multiple' => true,
    'expanded' => true,
    'by_reference' => false,
    'label' => false,

    // ⭐ tickets disponibles seulement
    'query_builder' => function($repo){
        return $repo->createQueryBuilder('t')
            ->where('t.reservation_id IS NULL');
    }
]);

    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Reservation::class,
            'validation_groups' => ['Default', 'user'],
            'is_edit' => false,
        ]);
    }
}