<?php

namespace App\Form;

use App\Entity\Attraction;
use App\Entity\Ville;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\TimeType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Positive;
use Symfony\Component\Validator\Constraints\Regex;

class AttractionType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom de l\'attraction',
                'constraints' => [
                    new NotBlank(['message' => 'Le nom est obligatoire']),
                    new Regex([
                        'pattern' => '/^[\p{L}0-9\s\-]+$/u',
                        'message' => 'Le nom ne doit contenir que des lettres, chiffres, espaces et tirets (pas de caractères spéciaux comme ; ? !)',
                    ]),
                ],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
                'required' => false,
            ])
            ->add('type', ChoiceType::class, [
                'label' => 'Type d\'attraction',
                'choices' => [
                    'Historical' => 'Historical',
                    'Nature' => 'Nature',
                    'Entertainment' => 'Entertainment',
                    'Religious' => 'Religious',
                    'Adventurous' => 'Adventurous',
                ],
                'placeholder' => 'Choisissez un type',
            ])
            ->add('prix', NumberType::class, [
                'label' => 'Prix',
                'required' => false,
                'constraints' => [
                    new Positive(['message' => 'Le prix doit être positif']),
                ],
            ])
            ->add('ville_id', EntityType::class, [
                'class' => Ville::class,
                'choice_label' => 'nom',
                'label' => 'Ville',
            ])
            ->add('heure_ouverture', TimeType::class, [
                'label' => 'Heure d\'ouverture',
                'widget' => 'single_text',
                'required' => false,
            ])
            ->add('heure_fermeture', TimeType::class, [
                'label' => 'Heure de fermeture',
                'widget' => 'single_text',
                'required' => false,
            ])
            ->add('est_ferme', CheckboxType::class, [
                'label' => 'Est fermé ?',
                'required' => false,
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Attraction::class,
        ]);
    }
}
