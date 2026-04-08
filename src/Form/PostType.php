<?php

namespace App\Form;

use App\Entity\Post;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\FileType;
use Symfony\Component\Validator\Constraints\NotBlank;
use Symfony\Component\Validator\Constraints\Length;
use Symfony\Component\OptionsResolver\OptionsResolver;

class PostType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options)
    {
        $builder
            ->add('contenu', TextType::class, [
                'required' => true,
                'constraints' => [
                    new NotBlank(['message' => 'Le contenu de la publication est obligatoire']),
                    new Length([
                        'min' => 3,
                        'minMessage' => 'Minimum 3 caractères'
                    ])
                ],
                'attr' => [
                    'placeholder' => 'Écrivez quelque chose de fort...',
                    'class' => 'form-control rounded-pill px-4'
                ]
            ])

            ->add('image', FileType::class, [
                'required' => false,
                'mapped' => false,
                'attr' => ['style' => 'display:none']
            ]);
    }

    public function configureOptions(OptionsResolver $resolver)
    {
        $resolver->setDefaults([
            'data_class' => Post::class,
        ]);
    }
}