<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class ProfileType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom',
                'attr'  => ['class' => 'form-control'],
                'constraints' => [new Assert\NotBlank(message: 'Le nom est requis.')],
            ])
            ->add('prenom', TextType::class, [
                'label' => 'Prénom',
                'attr'  => ['class' => 'form-control'],
                'constraints' => [new Assert\NotBlank(message: 'Le prénom est requis.')],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Adresse e-mail',
                'attr'  => ['class' => 'form-control'],
                'constraints' => [
                    new Assert\NotBlank(message: "L'email est requis."),
                    new Assert\Email(message: 'Email invalide.'),
                ],
            ])
            ->add('telephone', TextType::class, [
                'label'    => 'Téléphone',
                'required' => false,
                'attr'     => ['class' => 'form-control'],
            ])
            ->add('profilePhotoFile', FileType::class, [
                'label'    => 'Photo de profil',
                'mapped'   => false,
                'required' => false,
                'constraints' => [
                    new Assert\File([
                        'maxSize'          => '2M',
                        'mimeTypes'        => ['image/jpeg', 'image/png', 'image/gif'],
                        'mimeTypesMessage' => 'Formats acceptés : JPG, PNG, GIF',
                    ]),
                ],
                'attr' => ['class' => 'form-control', 'accept' => 'image/*'],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([]);
    }
}
