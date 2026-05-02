<?php

namespace App\Form;

use App\Entity\Pays;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\HiddenType;
use Symfony\Component\Form\FormEvent;
use Symfony\Component\Form\FormEvents;
use App\Service\CountryApiService;

class PaysType extends AbstractType
{
    private CountryApiService $countryApiService;

    public function __construct()
    {
        $this->countryApiService = new CountryApiService();
    }

    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        // Fetch all countries from API with error handling
        try {
            $countries = $this->countryApiService->getAllCountries();
        } catch (\Exception $e) {
            $countries = [];
            error_log('Country API Error: ' . $e->getMessage());
        }
        
        $countryChoices = [];
        if (!empty($countries)) {
            foreach ($countries as $country) {
                if (!empty($country['name'])) {
                    $countryChoices[$country['name']] = $country['name'];
                }
            }
            // Sort alphabetically
            ksort($countryChoices);
        }
        
        // Fallback if API fails
        if (empty($countryChoices)) {
            $countryChoices = ['-- API Error - Please retry --' => ''];
        }

        $builder
            ->add('nom', ChoiceType::class, [
                'label' => 'Nom du Pays',
                'choices' => $countryChoices,
                'placeholder' => 'Sélectionnez un pays',
                'attr' => ['class' => 'form-control'],
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description',
            ])
            ->add('visit_count', IntegerType::class, [
                'label' => 'Nombre de visites',
                'required' => false,
                'attr' => ['min' => 0],
            ])
            ->add('image', HiddenType::class, [
                'required' => false,
                'mapped' => false,
            ]);

        // Sync hidden (edit). Create stays empty until POST / JS.
        $builder->addEventListener(FormEvents::POST_SET_DATA, function (FormEvent $event): void {
            $data = $event->getData();
            if (!$data instanceof Pays) {
                return;
            }
            $form = $event->getForm();
            if (!$form->has('image')) {
                return;
            }
            $form->get('image')->setData($data->getImage() ?? '');
        });

        // After children are submitted, copy unmapped image onto the entity (covers create + modifier).
        $builder->addEventListener(FormEvents::SUBMIT, function (FormEvent $event): void {
            $form = $event->getForm();
            if (!$form->isRoot()) {
                return;
            }
            $data = $event->getData();
            if (!$data instanceof Pays) {
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
            'data_class' => Pays::class,
        ]);
    }
}
