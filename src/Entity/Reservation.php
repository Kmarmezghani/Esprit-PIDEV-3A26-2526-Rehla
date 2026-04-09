<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Ville;
use Doctrine\Common\Collections\Collection;
use App\Entity\Ticket;

#[ORM\Entity]
class Reservation
{

    #[ORM\Id]
    #[ORM\Column(type: "integer")]
    private int $id;

    #[ORM\Column(name: "dateReservation", type: "date")]
    private \DateTimeInterface $dateReservation;

    #[ORM\Column(name: "dateDebut",type: "date")]
    private \DateTimeInterface $dateDebut;

    #[ORM\Column(name: "dateFin", type: "date")]
    private \DateTimeInterface $dateFin;

    #[ORM\Column(type: "string", length: 50)]
    private string $statut;

    #[ORM\Column(name: "coutTotal", type: "float")]
    private float $coutTotal;

        #[ORM\ManyToOne(targetEntity: Personne::class, inversedBy: "reservations")]
    #[ORM\JoinColumn(name: 'personne_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Personne $personne_id;

        #[ORM\ManyToOne(targetEntity: Ville::class, inversedBy: "reservations")]
    #[ORM\JoinColumn(name: 'destination_id', referencedColumnName: 'id', onDelete: 'CASCADE')]
    private Ville $destination;

    #[ORM\Column(type: "integer")]
    private int $nb_tickets;

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
}
