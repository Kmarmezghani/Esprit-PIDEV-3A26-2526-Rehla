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
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\FormEvent;
use Symfony\Component\Form\FormEvents;

class VilleType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom de la Ville',
                // We use TextType here to completely avoid Symfony's strict ChoiceType validation 
                // for dynamic options. Twig will render it as a <select> and JS will populate it.
                'attr' => [
                    'class' => 'form-control ville-nom-select',
                    'placeholder' => 'Sélectionnez d\'abord un pays',
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
                'attr' => ['min' => 0],
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
            ])
            ->add('image', HiddenType::class, [
                'required' => false,
                'mapped' => false,
            ]);

        $builder->addEventListener(FormEvents::POST_SET_DATA, function (FormEvent $event): void {
            $data = $event->getData();
            if (!$data instanceof Ville) {
                return;
            }
            $form = $event->getForm();
            if (!$form->has('image')) {
                return;
            }
            $form->get('image')->setData($data->getImage() ?? '');
        });

        $builder->addEventListener(FormEvents::SUBMIT, function (FormEvent $event): void {
            $form = $event->getForm();
            if (!$form->isRoot()) {
                return;
            }
            $data = $event->getData();
            if (!$data instanceof Ville) {
                return;
            }
            if (!$form->has('image')) {
                return;
            }
            $img = trim((string) ($form->get('image')->getData() ?? ''));
            if ($img !== '') {
                $data->setImage($img);
            }
        });
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Ville::class,
        ]);
    }
}
