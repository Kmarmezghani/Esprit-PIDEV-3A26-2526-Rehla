<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\Collection;
use Doctrine\Common\Collections\ArrayCollection;
use App\Entity\Notification;

#[ORM\Entity]
class Personne
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: "integer")]
    private ?int $id = null;

    #[ORM\Column(type: "string", length: 100)]
    private string $nom;

    #[ORM\Column(type: "string", length: 100)]
    private string $prenom;

    #[ORM\Column(type: "string", length: 150)]
    private string $email;

    #[ORM\Column(name: "motDePasse", type: "string", length: 255)]
    private string $motDePasse;

    #[ORM\Column(name: "dateInscription", type: "date", nullable: true)]
    private ?\DateTimeInterface $dateInscription = null;

    #[ORM\Column(type: "string")]
    private string $role = 'CLIENT';

    #[ORM\Column(name: "statutCompte", type: "string", columnDefinition: "enum('ACTIF', 'INACTIF', 'SUSPENDU')")]
    private string $statutCompte = 'ACTIF';

    #[ORM\Column(type: "string", length: 20, nullable: true)]
    private ?string $telephone = null;

    #[ORM\Column(name: "heureNotif", type: "time", nullable: true)]
    private ?\DateTimeInterface $heureNotif = null;

    #[ORM\Column(name: "notifSmsActive", type: "boolean")]
    private bool $notifSmsActive = true;

    #[ORM\Column(name: "profile_photo", type: "string", length: 500, nullable: true)]
    private ?string $profile_photo = null;

    #[ORM\Column(name: "reset_token", type: "string", length: 100, nullable: true)]
    private ?string $resetToken = null;

    #[ORM\Column(name: "reset_token_expiry", type: "datetime", nullable: true)]
    private ?\DateTimeInterface $resetTokenExpiry = null;

    // ------------------- Relations -------------------

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Favoris::class)]
    private Collection $favoriss;

    #[ORM\OneToMany(mappedBy: "id", targetEntity: Guide::class)]
    private Collection $guides;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Post::class)]
    private Collection $posts;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Preference::class)]
    private Collection $preferences;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Avis::class)]
    private Collection $aviss;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Commentaire::class)]
    private Collection $commentaires;

    #[ORM\OneToMany(mappedBy: "user1_id", targetEntity: Conversation::class)]
    private Collection $conversationsUser1;

    #[ORM\OneToMany(mappedBy: "user2_id", targetEntity: Conversation::class)]
    private Collection $conversationsUser2;

    #[ORM\OneToMany(mappedBy: "sender_id", targetEntity: Message::class)]
    private Collection $messages;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Reservation::class)]
    private Collection $reservations;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Waitlist::class)]
    private Collection $waitlists;

    #[ORM\OneToMany(mappedBy: "personne_id", targetEntity: Likes::class)]
    private Collection $likess;

    #[ORM\OneToMany(mappedBy: "sender_id", targetEntity: Notification::class)]
    private Collection $sentNotifications;

    #[ORM\OneToMany(mappedBy: "receiver_id", targetEntity: Notification::class)]
    private Collection $receivedNotifications;

    #[ORM\ManyToMany(targetEntity: Groupe::class, mappedBy: "membres")]
    private Collection $groupes;


    // ------------------- Constructor -------------------
    public function __construct()
    {
        $this->favoriss = new ArrayCollection();
        $this->guides = new ArrayCollection();
        $this->posts = new ArrayCollection();
        $this->preferences = new ArrayCollection();
        $this->aviss = new ArrayCollection();
        $this->commentaires = new ArrayCollection();
        $this->conversationsUser1 = new ArrayCollection();
        $this->conversationsUser2 = new ArrayCollection();
        $this->messages = new ArrayCollection();
        $this->reservations = new ArrayCollection();
        $this->waitlists = new ArrayCollection();
        $this->likess = new ArrayCollection();
        $this->sentNotifications = new ArrayCollection();
        $this->receivedNotifications = new ArrayCollection();
        $this->groupes = new ArrayCollection();

    }
    

    // ------------------- Getters & Setters simples -------------------
    public function getId() { return $this->id; }
    public function setId($value) { $this->id = $value; }

    public function getNom() { return $this->nom; }
    public function setNom($value) { $this->nom = $value; }

    public function getPrenom() { return $this->prenom; }
    public function setPrenom($value) { $this->prenom = $value; }

    public function getEmail() { return $this->email; }
    public function setEmail($value) { $this->email = $value; }

    public function getMotDePasse() { return $this->motDePasse; }
    public function setMotDePasse($value) { $this->motDePasse = $value; }

    public function getDateInscription() { return $this->dateInscription; }
    public function setDateInscription($value) { $this->dateInscription = $value; }

    public function getRole() { return $this->role; }
    public function setRole($value) { $this->role = $value; }

    public function getStatutCompte() { return $this->statutCompte; }
    public function setStatutCompte($value) { $this->statutCompte = $value; }

    public function getTelephone() { return $this->telephone; }
    public function setTelephone($value) { $this->telephone = $value; }

    public function getHeureNotif() { return $this->heureNotif; }
    public function setHeureNotif($value) { $this->heureNotif = $value; }

    public function getNotifSmsActive() { return $this->notifSmsActive; }
    public function setNotifSmsActive($value) { $this->notifSmsActive = $value; }

    public function getProfile_photo() { return $this->profile_photo; }
    public function setProfile_photo($value) { $this->profile_photo = $value; }

    public function getResetToken(): ?string { return $this->resetToken; }
    public function setResetToken(?string $value): void { $this->resetToken = $value; }

    public function getResetTokenExpiry(): ?\DateTimeInterface { return $this->resetTokenExpiry; }
    public function setResetTokenExpiry(?\DateTimeInterface $value): void { $this->resetTokenExpiry = $value; }
    public function getGroupes(): Collection
{
    return $this->groupes;
}


    // ------------------- Relations Favoris -------------------
    public function getFavoriss(): Collection { return $this->favoriss; }
    public function addFavoris(Favoris $favoris): self
    {
        if (!$this->favoriss->contains($favoris)) {
            $this->favoriss[] = $favoris;
            $favoris->setPersonne_id($this);
        }
        return $this;
    }
    public function removeFavoris(Favoris $favoris): self
    {
        if ($this->favoriss->removeElement($favoris)) {
            if ($favoris->getPersonne_id() === $this) {
                $favoris->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Guides -------------------
    public function getGuides(): Collection { return $this->guides; }
    public function addGuide(Guide $guide): self
    {
        if (!$this->guides->contains($guide)) {
            $this->guides[] = $guide;
            $guide->setPersonne($this);
        }
        return $this;
    }
    public function removeGuide(Guide $guide): self
    {
        if ($this->guides->removeElement($guide)) {
            if ($guide->getPersonne() === $this) {
                $guide->setPersonne(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Posts -------------------
    public function getPosts(): Collection { return $this->posts; }
    public function addPost(Post $post): self
    {
        if (!$this->posts->contains($post)) {
            $this->posts[] = $post;
            $post->setPersonne_id($this);
        }
        return $this;
    }
    public function removePost(Post $post): self
    {
        if ($this->posts->removeElement($post)) {
            if ($post->getPersonne_id() === $this) {
                $post->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Preferences -------------------
    public function getPreferences(): Collection { return $this->preferences; }
    public function addPreference(Preference $preference): self
    {
        if (!$this->preferences->contains($preference)) {
            $this->preferences[] = $preference;
            $preference->setPersonne_id($this);
        }
        return $this;
    }
    public function removePreference(Preference $preference): self
    {
        if ($this->preferences->removeElement($preference)) {
            if ($preference->getPersonne_id() === $this) {
                $preference->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Avis -------------------
    public function getAviss(): Collection { return $this->aviss; }
    public function addAvis(Avis $avis): self
    {
        if (!$this->aviss->contains($avis)) {
            $this->aviss[] = $avis;
            $avis->setPersonne($this);
        }
        return $this;
    }
    public function removeAvis(Avis $avis): self
    {
        if ($this->aviss->removeElement($avis)) {
            if ($avis->getPersonne() === $this) {
                $avis->setPersonne(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Commentaires -------------------
    public function getCommentaires(): Collection { return $this->commentaires; }
    public function addCommentaire(Commentaire $commentaire): self
    {
        if (!$this->commentaires->contains($commentaire)) {
            $this->commentaires[] = $commentaire;
            $commentaire->setPersonne_id($this);
        }
        return $this;
    }
    public function removeCommentaire(Commentaire $commentaire): self
    {
        if ($this->commentaires->removeElement($commentaire)) {
            if ($commentaire->getPersonne_id() === $this) {
                $commentaire->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Conversations -------------------
public function getConversationsUser1(): Collection
{
    return $this->conversationsUser1;
}

public function addConversationUser1(Conversation $conversation): self
{
    if (!$this->conversationsUser1->contains($conversation)) {
        $this->conversationsUser1[] = $conversation;
        $conversation->setUser1_id($this);
    }
    return $this;
}

public function removeConversationUser1(Conversation $conversation): self
{
    if ($this->conversationsUser1->removeElement($conversation)) {
        // Vérifie si la conversation pointe toujours vers cette personne
        if ($conversation->getUser1_id() === $this) {
            $conversation->setUser1_id(null);
        }
    }
    return $this;
}

public function getConversationsUser2(): Collection
{
    return $this->conversationsUser2;
}

public function addConversationUser2(Conversation $conversation): self
{
    if (!$this->conversationsUser2->contains($conversation)) {
        $this->conversationsUser2[] = $conversation;
        $conversation->setUser2_id($this);
    }
    return $this;
}

public function removeConversationUser2(Conversation $conversation): self
{
    if ($this->conversationsUser2->removeElement($conversation)) {
        if ($conversation->getUser2_id() === $this) {
            $conversation->setUser2_id(null); // Maintenant possible grâce au type nullable
        }
    }
    return $this;
}

    // ------------------- Relations Messages -------------------
    public function getMessages(): Collection { return $this->messages; }
    public function addMessage(Message $message): self
    {
        if (!$this->messages->contains($message)) {
            $this->messages[] = $message;
            $message->setSender_id($this);
        }
        return $this;
    }
    public function removeMessage(Message $message): self
{
    if ($this->messages->removeElement($message)) {
        if ($message->getSender_id() === $this) {
            $message->setSender_id(null);
        }
    }
    return $this;

}

    // ------------------- Relations Reservations -------------------
    public function getReservations(): Collection { return $this->reservations; }
    public function addReservation(Reservation $reservation): self
    {
        if (!$this->reservations->contains($reservation)) {
            $this->reservations[] = $reservation;
            $reservation->setPersonne_id($this);
        }
        return $this;
    }
    public function removeReservation(Reservation $reservation): self
    {
        if ($this->reservations->removeElement($reservation)) {
            if ($reservation->getPersonne_id() === $this) {
                $reservation->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Waitlists -------------------
    public function getWaitlists(): Collection { return $this->waitlists; }
    public function addWaitlist(Waitlist $waitlist): self
    {
        if (!$this->waitlists->contains($waitlist)) {
            $this->waitlists[] = $waitlist;
            $waitlist->setPersonne_id($this);
        }
        return $this;
    }
    public function removeWaitlist(Waitlist $waitlist): self
    {
        if ($this->waitlists->removeElement($waitlist)) {
            if ($waitlist->getPersonne_id() === $this) {
                $waitlist->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Likes -------------------
    public function getLikess(): Collection { return $this->likess; }
    public function addLikes(Likes $likes): self
    {
        if (!$this->likess->contains($likes)) {
            $this->likess[] = $likes;
            $likes->setPersonne_id($this);
        }
        return $this;
    }
    public function removeLikes(Likes $likes): self
    {
        if ($this->likess->removeElement($likes)) {
            if ($likes->getPersonne_id() === $this) {
                $likes->setPersonne_id(null);
            }
        }
        return $this;
    }

    // ------------------- Relations Notifications -------------------
    public function getSentNotifications(): Collection { return $this->sentNotifications; }
    public function addSentNotification(Notification $notification): self
    {
        if (!$this->sentNotifications->contains($notification)) {
            $this->sentNotifications[] = $notification;
            $notification->setSender_id($this);
        }
        return $this;
    }
    public function removeSentNotification(Notification $notification): self
    {
        if ($this->sentNotifications->removeElement($notification)) {
            if ($notification->getSender_id() === $this) {
                $notification->setSender_id(null);
            }
        }
        return $this;
    }

    public function getReceivedNotifications(): Collection { return $this->receivedNotifications; }
    public function addReceivedNotification(Notification $notification): self
    {
        if (!$this->receivedNotifications->contains($notification)) {
            $this->receivedNotifications[] = $notification;
            $notification->setReceiver_id($this);
        }
        return $this;
    }
    public function removeReceivedNotification(Notification $notification): self
    {
        if ($this->receivedNotifications->removeElement($notification)) {
            if ($notification->getReceiver_id() === $this) {
                $notification->setReceiver_id(null);
            }
        }
        return $this;
    }
}