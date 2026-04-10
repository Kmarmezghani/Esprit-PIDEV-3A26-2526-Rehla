<?php

namespace App\Form;

use App\Entity\Reservation;
use App\Entity\Ville;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ReservationType extends AbstractType
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
            ->add('destination', EntityType::class, [
                'class' => Ville::class,
                'choice_label' => 'nom',
                'placeholder' => 'Sélectionner une destination',
                'label' => 'Destination',
            ])
            ->add('statut', ChoiceType::class, [
                'choices'  => [
                    'Confirmée' => 'Confirmée',
                    'Réservée' => 'réservée',
                    'Annulée' => 'Annulée',
                ],
                'label' => 'Statut',
                'placeholder' => 'Sélectionner le statut',
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Reservation::class,
        ]);
    }
}