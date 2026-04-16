<?php
namespace App\Form;

use App\Entity\Reservation;
use App\Entity\Ville;
use App\Entity\Ticket;
use Doctrine\ORM\EntityManagerInterface;
use Doctrine\ORM\EntityRepository;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\Form\FormEvent;
use Symfony\Component\Form\FormEvents;
use Symfony\Component\Form\FormInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class ReservationUserType extends AbstractType
{
    public function __construct(private EntityManagerInterface $em) {}

    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $isEdit = $options['is_edit'];
        $em     = $this->em;

        $builder
            ->add('dateDebut', DateType::class, [
                'widget' => 'single_text',
                'label'  => 'Date Début',
            ])
            ->add('dateFin', DateType::class, [
                'widget' => 'single_text',
                'label'  => 'Date Fin',
            ])
            ->add('statut', ChoiceType::class, [
                'choices'  => [
                    'Confirmée' => 'confirmée',
                    'réservée'  => 'réservée',
                    'Annulée'   => 'annulée',
                ],
                'label'    => 'Statut',
                'disabled' => !$isEdit,
                'attr'     => ['class' => 'form-select'],
            ])
            ->add('destination', EntityType::class, [
                'class'        => Ville::class,
                'choice_label' => 'nom',
                'placeholder'  => 'Sélectionner une destination',
                'label'        => 'Destination',
                'attr'         => ['id' => 'destination_select'],
            ]);

        $addTicketsField = function (FormInterface $form, ?Ville $destination, ?Reservation $reservation = null) use ($isEdit) {
    $form->add('tickets', EntityType::class, [
        'class'        => Ticket::class,
        'choice_label' => function (Ticket $ticket) {
            return $ticket->getType() . ' - ' . $ticket->getPrix() . ' TND';
        },
        'multiple'     => true,
        'expanded'     => true,
        'by_reference' => false,
        'label'        => false,
        'query_builder' => function (EntityRepository $repo) use ($destination, $reservation, $isEdit) {
            $qb = $repo->createQueryBuilder('t');

            // Aucune destination sélectionnée et pas en edit → liste vide
            // (les tickets seront injectés via AJAX côté JS)
            if ($destination === null && !$isEdit) {
                return $qb->where('1 = 0'); // ← retourne 0 résultats intentionnellement
            }

            $qb->where('t.reservation_id IS NULL');

            if ($isEdit && $reservation?->getId()) {
                $qb->orWhere('t.reservation_id = :resa')
                   ->setParameter('resa', $reservation->getId());
            }

            if ($destination !== null) {
                $qb->andWhere('t.destination = :dest')
                   ->setParameter('dest', $destination);
            }

            return $qb;
        },
    ]);
};

        // Chargement initial
        $builder->addEventListener(FormEvents::PRE_SET_DATA, function (FormEvent $event) use ($addTicketsField) {
    $reservation = $event->getData();
    $destination = ($reservation instanceof Reservation) ? $reservation->getDestination() : null;

    // En création (pas d'id), on ne pré-charge rien — l'AJAX s'en charge
    $addTicketsField($event->getForm(), $destination, $reservation);
});

        // Après soumission : reconstruire avec la destination choisie
        $builder->addEventListener(FormEvents::PRE_SUBMIT, function (FormEvent $event) use ($addTicketsField, $em) {
            $data        = $event->getData();
            $form        = $event->getForm();
            $reservation = $form->getData();

            $destination = null;
            if (!empty($data['destination'])) {
                $destination = $em->getRepository(Ville::class)->find($data['destination']);
            }

            $addTicketsField($form, $destination, $reservation);
        });
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class'        => Reservation::class,
            'validation_groups' => ['Default', 'user'],
            'is_edit'           => false,
        ]);
    }
}