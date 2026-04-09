<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\OptionsResolver\OptionsResolver;

class PreferenceType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('budgetMin', NumberType::class, [
                'label'    => 'Budget minimum (€)',
                'required' => false,
                'attr'     => ['class' => 'form-control', 'placeholder' => '0', 'min' => 0],
            ])
            ->add('budgetMax', NumberType::class, [
                'label'    => 'Budget maximum (€)',
                'required' => false,
                'attr'     => ['class' => 'form-control', 'placeholder' => '5000', 'min' => 0],
            ])
            ->add('typesVoyage', ChoiceType::class, [
                'label'    => "Types de voyage",
                'choices'  => [
                    'Aventure'     => 'Aventure',
                    'Culture'      => 'Culture',
                    'Romantique'   => 'Romantique',
                    'Gastronomie'  => 'Gastronomie',
                    'Randonnée'    => 'Randonnée',
                    'Loisirs'      => 'Loisirs',
                    'Sport'        => 'Sport',
                    'Nature'       => 'Nature',
                    'Détente'      => 'Détente',
                    'Historique'   => 'Historique',
                ],
                'multiple' => true,
                'expanded' => true,
                'mapped'   => false,
                'required' => false,
            ])
            ->add('centresInteret', ChoiceType::class, [
                'label'    => "Centres d'intérêt",
                'choices'  => [
                    'Musées'           => 'Museum',
                    'Randonnée'        => 'Hiking',
                    'Plages'           => 'Beach',
                    'Désert'           => 'desert',
                    'Architecture'     => 'Architecture',
                    'Histoire'         => 'history',
                    'Cuisine locale'   => 'Food',
                    'Shopping'         => 'Shopping',
                    'Photographie'     => 'Photography',
                    'Sports nautiques' => 'Water Sports',
                ],
                'multiple' => true,
                'expanded' => true,
                'mapped'   => false,
                'required' => false,
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults(['data_class' => null]);
    }
}
