<?php

namespace App\Form;

use App\Entity\Activite;
use App\Entity\Ville;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Positive;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;

class ActiviteType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', null)
            ->add('description', null)
            ->add('prix', null)
            ->add('typeActivite', null)
            ->add('date_debut', DateTimeType::class, [
                'widget' => 'single_text',
                'constraints' => [
                    new NotBlank(['message' => 'La date de début est obligatoire']),
                ],
            ])
            ->add('date_fin', DateTimeType::class, [
                'widget' => 'single_text',
                'constraints' => [
                    new NotBlank(['message' => 'La date de fin est obligatoire']),
                    new Assert\Callback([$this, 'validateDates']),
                ],
            ])
            ->add('destination', EntityType::class, [
                'class' => Ville::class,
                'choice_label' => 'nom',
                'constraints' => [
                    new NotBlank(['message' => 'La destination est obligatoire']),
                ],
            ])
            ->add('image', FileType::class, [
                'label' => 'Image',
                'mapped' => false,
                'required' => !$options['is_edit'],
                'constraints' => $options['is_edit'] ? [] : [
                    new NotBlank(['message' => 'Veuillez fournir une image']),
                    new File([
                        'maxSize' => '2M',
                        'mimeTypes' => ['image/jpeg', 'image/png', 'image/jpg'],
                        'mimeTypesMessage' => 'Veuillez uploader une image valide (jpg, jpeg, png)',
                    ]),
                ],
            ]);

        if ($options['show_max_places']) {
            $builder->add('max_places', null, [
                'label' => 'Places maximum',
                'constraints' => [
                    new NotBlank(['message' => 'Le nombre maximum de places est obligatoire']),
                    new Assert\PositiveOrZero(['message' => 'Le nombre de places doit être positif ou nul']),
                ],
            ]);
        }

        if ($options['is_edit']) {
            $builder->add('status', ChoiceType::class, [
                'choices' => [
                    'Disponible' => 'DISPONIBLE',
                    'Indisponible' => 'INDISPONIBLE',
                ],
                'label' => 'Statut',
            ]);
        }
    }

    public function validateDates($dateFin, ExecutionContextInterface $context): void
    {
        $form = $context->getRoot();
        $dateDebut = $form['date_debut']->getData();

        if ($dateDebut && $dateFin && $dateFin < $dateDebut) {
            $context->buildViolation('La date de fin doit être supérieure ou égale à la date de début')
                ->addViolation();
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Activite::class,
            'is_edit' => false,
            'show_max_places' => false,
        ]);
    }
}