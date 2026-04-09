<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Ville;
use Doctrine\Common\Collections\Collection;
use App\Entity\Ticket;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;
use Doctrine\Common\Collections\ArrayCollection;


#[ORM\Entity]
class Reservation
{

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(name: "dateReservation", type: "date")]
#[Assert\NotNull(message: "La date de réservation est obligatoire")]
private \DateTimeInterface $dateReservation;

#[ORM\Column(name: "dateDebut", type: "date")]
#[Assert\NotNull(message: "La date de début est obligatoire")]
private \DateTimeInterface $dateDebut;

#[ORM\Column(name: "dateFin", type: "date")]
#[Assert\NotNull(message: "La date de fin est obligatoire")]
private \DateTimeInterface $dateFin;

#[ORM\Column(type: "string", length: 50)]
#[Assert\NotBlank(message: "Le statut est obligatoire")]
private ?string $statut = null;

#[ORM\Column(name: "coutTotal", type: "float")]
private float $coutTotal;  // ← remove @Assert\Positive and @Assert\NotNull

#[ORM\Column(type: "integer")]
private int $nb_tickets;

#[ORM\ManyToOne(targetEntity: Ville::class)]
#[Assert\NotNull(message: "La destination est obligatoire")]
private Ville $destination;
#[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "reservations")] 
#[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')] 
private ?Personne $personne_id = null;

    public function getId()
    {
        return $this->id;
    }

    public function setId($value)
    {
        $this->id = $value;
    }

    public function getDateReservation()
    {
        return $this->dateReservation;
    }

    public function setDateReservation($value)
    {
        $this->dateReservation = $value;
    }

    public function getDateDebut()
    {
        return $this->dateDebut;
    }

    public function setDateDebut($value)
    {
        $this->dateDebut = $value;
    }

    public function getDateFin()
    {
        return $this->dateFin;
    }

    public function setDateFin($value)
    {
        $this->dateFin = $value;
    }

    public function getStatut()
    {
        return $this->statut;
    }

    public function setStatut($value)
    {
        $this->statut = $value;
    }

    public function getCoutTotal()
    {
        return $this->coutTotal;
    }

    public function setCoutTotal($value)
    {
        $this->coutTotal = $value;
    }

    public function getPersonne_id()
    {
        return $this->personne_id;
    }

    public function setPersonne_id($value)
    {
        $this->personne_id = $value;
    }

    public function getDestination(): ?Ville
{
    return $this->destination;
}

public function setDestination(?Ville $destination): self
{
    $this->destination = $destination;
    return $this;
}

    public function getNb_tickets()
    {
        return $this->nb_tickets;
    }

    public function setNb_tickets($value)
    {
        $this->nb_tickets = $value;
    }

    #[ORM\OneToMany(mappedBy: "reservation_id", targetEntity: Ticket::class)]
    private Collection $tickets;

        public function getTickets(): Collection
        {
            return $this->tickets;
        }
    
        public function addTicket(Ticket $ticket): self
        {
            if (!$this->tickets->contains($ticket)) {
                $this->tickets[] = $ticket;
                $ticket->setReservation_id($this);
            }
    
            return $this;
        }
    
        public function removeTicket(Ticket $ticket): self
        {
            if ($this->tickets->removeElement($ticket)) {
                // set the owning side to null (unless already changed)
                if ($ticket->getReservation_id() === $this) {
                    $ticket->setReservation_id(null);
                }
            }
    
            return $this;
        }

        #[Assert\Callback]
public function validateDates(ExecutionContextInterface $context): void
{
    if (!$this->dateDebut || !$this->dateFin) {
        return;
    }

    $today = new \DateTimeImmutable('today');

    $dateDebut = \DateTimeImmutable::createFromInterface($this->dateDebut);
    $dateFin = \DateTimeImmutable::createFromInterface($this->dateFin);

    // ❌ date début dans le passé
    if ($dateDebut < $today) {
        $context->buildViolation('La date de début ne peut pas être avant aujourd’hui.')
            ->atPath('dateDebut')
            ->addViolation();
    }

    // ❌ date fin avant date début
    if ($dateFin < $dateDebut) {
        $context->buildViolation('La date de fin doit être après la date de début.')
            ->atPath('dateFin')
            ->addViolation();
    }
}
public function __construct()
{
    $this->tickets = new ArrayCollection();
    $this->dateReservation = new \DateTime();
    $this->statut = 'réservée';
    $this->coutTotal = 0;
    $this->nb_tickets = 0;
}
}
