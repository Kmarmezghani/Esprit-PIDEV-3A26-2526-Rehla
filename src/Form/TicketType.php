<?php

namespace App\Form;

use App\Entity\Ticket;
use App\Entity\Ville;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\Extension\Core\Type\MoneyType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\FormEvent;
use Symfony\Component\Form\FormEvents;
use Symfony\Component\OptionsResolver\OptionsResolver;

class TicketType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('prix', MoneyType::class, [
                'currency' => 'TND',
                'label' => 'Prix'
            ])
            ->add('destination', EntityType::class, [
                'class' => Ville::class,
                'choice_label' => 'nom',
                'placeholder' => 'Sélectionner destination'
            ]);

        $builder->addEventListener(FormEvents::PRE_SET_DATA, function (FormEvent $event) {
            $ticket = $event->getData();
            $form = $event->getForm();

            $choices = [
                'Vol ✈️' => 'Vol',
                'Hotel 🏨' => 'Hotel',
                'Transport 🚗' => 'Transport',
            ];

            if ($ticket && $ticket->getId() !== null) {
                // Mode EDIT — ajoute Activité et statut
                $choices['Activité 🎯'] = 'Activité';

                $form->add('statut', ChoiceType::class, [
                    'choices' => [
                        'Disponible' => 'Disponible',
                        'Reservé' => 'Reservé',
                        'Annulé' => 'Annulé',
                    ],
                    'placeholder' => 'Sélectionner statut',
                    'required' => true,
                ]);
            }

            $form->add('type', ChoiceType::class, [
                'choices' => $choices,
                'placeholder' => 'Sélectionner type',
                'label' => 'Type de ticket'
            ]);
        });
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Ticket::class,
        ]);
    }
}