<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\PasswordType;
use Symfony\Component\Form\Extension\Core\Type\RepeatedType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class RegisterType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom',
                'attr'  => ['placeholder' => 'Votre nom', 'class' => 'form-control'],
                'constraints' => [new Assert\NotBlank(message: 'Le nom est requis.')],
            ])
            ->add('prenom', TextType::class, [
                'label' => 'Prénom',
                'attr'  => ['placeholder' => 'Votre prénom', 'class' => 'form-control'],
                'constraints' => [new Assert\NotBlank(message: 'Le prénom est requis.')],
            ])
            ->add('email', EmailType::class, [
                'label' => 'Adresse e-mail',
                'attr'  => ['placeholder' => 'votre@email.com', 'class' => 'form-control'],
                'constraints' => [
                    new Assert\NotBlank(message: "L'email est requis."),
                    new Assert\Email(message: 'Email invalide.'),
                ],
            ])
            ->add('telephone', TextType::class, [
                'label'    => 'Téléphone (optionnel)',
                'required' => false,
                'attr'     => ['placeholder' => '+216 XX XXX XXX', 'class' => 'form-control'],
            ])
            ->add('motDePasse', RepeatedType::class, [
                'type'          => PasswordType::class,
                'first_options' => [
                    'label' => 'Mot de passe',
                    'attr'  => ['placeholder' => '••••••••', 'class' => 'form-control'],
                    'constraints' => [
                        new Assert\NotBlank(message: 'Le mot de passe est requis.'),
                        new Assert\Length(min: 4, minMessage: 'Minimum 4 caractères.'),
                    ],
                ],
                'second_options' => [
                    'label' => 'Confirmer le mot de passe',
                    'attr'  => ['placeholder' => '••••••••', 'class' => 'form-control'],
                ],
                'invalid_message' => 'Les mots de passe ne correspondent pas.',
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([]);
    }
}
