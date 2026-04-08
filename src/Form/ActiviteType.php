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
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Validator\Constraints\File;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Positive;
use Symfony\Component\Validator\Constraints as Assert;

class ActiviteType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', null, [
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le nom est obligatoire']),
                    new Assert\Length(['max' => 150, 'maxMessage' => 'Maximum {{ limit }} caractères']),
                ],
            ])
            ->add('description', null, [
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La description est obligatoire']),
                ],
            ])
            ->add('prix', null, [
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le prix est obligatoire']),
                    new Assert\Positive(['message' => 'Le prix doit être positif']),
                ],
            ])
            ->add('typeActivite', null, [
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le type d\'activité est obligatoire']),
                ],
            ])
            ->add('date_debut', DateTimeType::class, [
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de début est obligatoire']),
                ],
            ])
            ->add('date_fin', DateTimeType::class, [
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de fin est obligatoire']),
                    new Assert\Callback([$this, 'validateDates']),
                ],
            ])
            ->add('destination', EntityType::class, [
                'class' => Ville::class,
                'choice_label' => 'nom',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La destination est obligatoire']),
                ],
            ])
            ->add('image', FileType::class, [
                'label' => 'Image',
                'mapped' => false,
                'required' => true, // image obligatoire
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Veuillez fournir une image']),
                    new File([
                        'maxSize' => '2M',
                        'mimeTypes' => [
                            'image/jpeg',
                            'image/png',
                            'image/jpg',
                        ],
                        'mimeTypesMessage' => 'Veuillez uploader une image valide (jpg, jpeg, png)',
                    ]),
                ],
            ]);
    }

    public function validateDates($dateFin, \Symfony\Component\Validator\Context\ExecutionContextInterface $context)
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
        ]);
    }
}