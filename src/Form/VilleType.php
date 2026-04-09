<?php

namespace App\Form;

use App\Entity\Ville;
use App\Entity\Pays;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Validator\Constraints\NotBlank;

class VilleType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom de la Ville',
                'constraints' => [
                    new NotBlank(['message' => 'Le nom est obligatoire']),
                ],
            ])
            ->add('pays_id', EntityType::class, [
                'class' => Pays::class,
                'choice_label' => 'nom',
                'label' => 'Pays',
            ])
            ->add('visit_count', IntegerType::class, [
                'label' => 'Nombre de visites',
                'required' => false,
            ])
            ->add('latitude', NumberType::class, [
                'label' => 'Latitude',
                'required' => false,
            ])
            ->add('longitude', NumberType::class, [
                'label' => 'Longitude',
                'required' => false,
            ])
            ->add('typeTourisme', ChoiceType::class, [
                'label' => 'Type de Tourisme',
                'choices' => [
                    'Seaside' => 'Seaside',
                    'Desert' => 'Desert',
                    'Mountain' => 'Mountain',
                    'Urban' => 'Urban',
                    'Cultural' => 'Cultural',
                ],
                'placeholder' => 'Sélectionnez un type',
            ])
            ->add('saison', ChoiceType::class, [
                'label' => 'Saison idéale',
                'choices' => [
                    'Winter' => 'Winter',
                    'Spring' => 'Spring',
                    'Summer' => 'Summer',
                    'Autumn' => 'Autumn',
                    'All Year' => 'All Year',
                ],
                'placeholder' => 'Sélectionnez une saison',
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Ville::class,
        ]);
    }
}
