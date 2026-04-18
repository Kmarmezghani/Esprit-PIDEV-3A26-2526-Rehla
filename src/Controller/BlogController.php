<?php

namespace App\Controller;

use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;
use App\Entity\Post;
use App\Entity\Personne;
use App\Entity\Commentaire;
use Doctrine\ORM\EntityManagerInterface;
use App\Form\PostType;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\File\Exception\FileException;
use Symfony\Component\HttpFoundation\JsonResponse;
use App\Entity\Likes;
use App\Service\ImageUploader;
use App\Service\FlaskClient\ToxicityChecker;
use App\Entity\Notification;
use App\Service\FlaskClient\GeoLocalisationService;
use App\Entity\Favoris;
use App\Entity\Favoris_post;
use App\Controller\FavorisController;
use App\Service\CloudinaryService;
use App\Service\AyrshareService;
use App\Service\Messagerie\ConversationService;
use App\Entity\Message;
use App\Entity\Conversation;

final class BlogController extends AbstractController

{

#[Route('/blog', name: 'blog')]
public function index(Request $request, EntityManagerInterface $em, ImageUploader $uploader, ToxicityChecker $toxicityChecker): Response
{
    $personne = $this->getConnectedUser($request, $em);

    $post = new Post();
    $form = $this->createForm(PostType::class, $post);
    $form->handleRequest($request);

   $status = $this->handlePostCreation(
    $form,
    $post,
    $personne,
    $uploader,
    $em,
    $toxicityChecker
);

if ($status === 'refused') {
    $this->addFlash('error', '❌ Contenu refusé (toxique)');
    return $this->redirectToRoute('blog');
}

if ($status === 'warning') {
    $this->addFlash('warning', '⚠️ Contenu sensible publié (admin notifié)');
    return $this->redirectToRoute('blog');
}

if ($status === 'ok') {
    $this->addFlash('success', '✅ Publication ajoutée');
    return $this->redirectToRoute('blog');
}

    $posts = $this->getFilteredPosts($request, $em);
    $users = $em->getRepository(Personne::class)->findAll();
    $groups = $personne->getGroupes();

        $groups = $em->createQueryBuilder()
        ->select('g')
        ->from(\App\Entity\Groupe::class, 'g')
        ->join('g.membres', 'm')
        ->where('m = :user')
        ->setParameter('user', $personne)
        ->getQuery()
        ->getResult();

        $conversations = $em->createQueryBuilder()
            ->select('c')
            ->from(Conversation::class, 'c')
            ->where('c.user1_id = :me OR c.user2_id = :me')
            ->setParameter('me', $personne)
            ->getQuery()
            ->getResult();

        $chatList = [];
        $usedUserIds = [];
        $totalUnread = 0;


    foreach ($conversations as $conv) {

        $lastMessage = $em->createQueryBuilder()
            ->select('m')
            ->from(Message::class, 'm')
            ->where('m.conversation = :conv')
            ->setParameter('conv', $conv)
            ->orderBy('m.sent_at', 'DESC')
            ->setMaxResults(1)
            ->getQuery()
            ->getOneOrNullResult();


            $unreadExists = $em->createQueryBuilder()
            ->select('m.id')
            ->from(Message::class, 'm')
            ->where('m.conversation = :conv')
            ->andWhere('m.is_read = false')
            ->andWhere('m.sender_id != :me')
            ->setParameter('conv', $conv)
            ->setParameter('me', $personne)
            ->setMaxResults(1) 
            ->getQuery()
            ->getOneOrNullResult();

        $hasUnread = $unreadExists ? 1 : 0;


        $totalUnread += $hasUnread;



        $otherUser = ($conv->getUser1_id()->getId() === $personne->getId())
            ? $conv->getUser2_id()
            : $conv->getUser1_id();

        $usedUserIds[] = $otherUser->getId();

        $chatList[] = [
            'type' => 'conversation',
            'user' => $otherUser,
            'conversationId' => $conv->getId(),
            'lastMessage' => $lastMessage?->getImage() ? '📷 Photo' : $lastMessage?->getContenu(),
            'lastMessageTime' => $lastMessage?->getSent_at(),
            'lastSenderId' => $lastMessage?->getSender_id()?->getId(),
            'unread' => $hasUnread === 1
        ];
    }

       foreach ($users as $user) {

    if ($user->getId() === $personne->getId()) continue;

    if (in_array($user->getId(), $usedUserIds)) continue;

    $chatList[] = [
        'type' => 'user',
        'user' => $user,
        'conversationId' => null,
        'lastMessage' => null,
        'lastMessageTime' => null,
        'unread' => false 
    ];
}



 usort($chatList, function ($a, $b) {

    if ($a['type'] !== $b['type']) {
        return $a['type'] === 'conversation' ? -1 : 1;
    }

    // si null safe
    $timeA = $a['lastMessageTime'] ? $a['lastMessageTime']->getTimestamp() : 0;
    $timeB = $b['lastMessageTime'] ? $b['lastMessageTime']->getTimestamp() : 0;

    return $timeB <=> $timeA;
});

        return $this->render('blog/blog.html.twig', [
            'posts' => $posts,
            'form' => $form->createView(),
            'chatList' => $chatList,
            'currentUserId' => $personne?->getId(),
            'unreadTotal' => $totalUnread,
            'users' => $users,
            'groups' => $groups
        ]);

}


private function getConnectedUser(Request $request, EntityManagerInterface $em): ?Personne
{
    $userId = $request->getSession()->get('user_id');
    return $em->getRepository(Personne::class)->find($userId);
}


/*--------------------------------------------------groups-------------------------------------------------------------*/
#[Route('/chat/group/start/{id}', name: 'chat_group_start')]
public function startGroupChat($id, EntityManagerInterface $em, Request $request)
{
    $personne = $this->getConnectedUser($request, $em);

    if (!$personne) {
        return $this->json(['error' => 'User not connected'], 401);
    }

    $groupe = $em->getRepository(\App\Entity\Groupe::class)->find($id);

    if (!$groupe) {
        return $this->json(['error' => 'Group not found'], 404);
    }

    // 🔥 chercher conversation existante
    $conv = $em->getRepository(Conversation::class)->findOneBy([
        'groupe' => $groupe
    ]);

    // 🔥 sinon créer
    if (!$conv) {
        $conv = new Conversation();
        $conv->setGroupe($groupe);
        $conv->setCreated_at(new \DateTime());

        $conv->setUser1_id(null);
        $conv->setUser2_id(null);

        $em->persist($conv);
        $em->flush();
    }

    return $this->json([
        'conversationId' => $conv->getId()
    ]);
}
#[Route('/groupe/create', name: 'groupe_create', methods: ['POST'])]
public function createGroup(Request $request, EntityManagerInterface $em): JsonResponse
{
    try {

        $personne = $this->getConnectedUser($request, $em);

        if (!$personne) {
            return $this->json(['error' => 'Not connected'], 401);
        }

        $name = $request->request->get('name');

        $members = json_decode($request->request->get('members'), true) ?? [];

        $imageFile = $request->files->get('image');

        $groupe = new \App\Entity\Groupe();
        $groupe->setNom($name);
        $groupe->setCreated_at(new \DateTime());

        $groupe->addMembre($personne);

        foreach ($members as $id) {
            $user = $em->getRepository(\App\Entity\Personne::class)->find($id);
            if ($user) {
                $groupe->addMembre($user);
            }
        }
        if ($imageFile) {
            $newFilename = uniqid().'.'.$imageFile->guessExtension();

            $imageFile->move(
                $this->getParameter('kernel.project_dir') . '/public/uploads/groups',
                $newFilename
            );

            $groupe->setImage('uploads/groups/' . $newFilename);
        }

        $em->persist($groupe);
        $em->flush();

        return $this->json([
            'status' => 'ok',
            'image' => $groupe->getImage()
        ]);

    } catch (\Throwable $e) {

        return $this->json([
            'error' => $e->getMessage(),
            'line' => $e->getLine()
        ], 500);
    }
}



/*--------------------------------------------messagerie-----------------------------------------------------------*/ 

#[Route('/chat/start/{id}', name: 'chat_start')]
public function startChat(Personne $receiver, ConversationService $service, EntityManagerInterface $em, Request $request)
{
    $personne = $this->getConnectedUser($request, $em);

    if (!$personne) {
        return $this->json(['error' => 'User not connected'], 401);
    }

    $conv = $service->getOrCreateConversation($personne, $receiver, $em);

    $messages = $em->createQueryBuilder()
    ->select('m')
    ->from(Message::class, 'm')
    ->where('m.conversation = :conv')
    ->andWhere('m.sender_id != :me')
    ->andWhere('m.is_read = 0')
    ->setParameter('conv', $conv)
    ->setParameter('me', $personne)
    ->getQuery()
    ->getResult();

        foreach ($messages as $msg) {
            $msg->setIs_read(true);
            $em->persist($msg);
        }

        $em->flush();


    return $this->json([
        'conversationId' => $conv->getId()
    ]);
}


#[Route('/chat/send', name: 'chat_send', methods:['POST'])]
public function send(Request $request, EntityManagerInterface $em)
{
    $sender = $this->getConnectedUser($request, $em);

    if (!$sender) {
        return $this->json(['error' => 'User not connected']);
    }

    $conversationId = $request->request->get('conversationId');
    $messageText = $request->request->get('message');
    $imageFile = $request->files->get('image');

    $conv = $em->getRepository(Conversation::class)->find($conversationId);

    if (!$conv) {
        return $this->json(['error' => 'Conversation not found']);
    }

    $msg = new Message();
    $msg->setContenu($messageText);
    $msg->setSent_at(new \DateTime());
    $msg->setIs_read(false);
    $msg->setSender_id($sender);
    $msg->setConversation($conv);

    // 📸 UPLOAD IMAGE
    if ($imageFile) {
        $newFilename = uniqid().'.'.$imageFile->guessExtension();

        try {
            $imageFile->move(
                $this->getParameter('kernel.project_dir') . '/public/uploads/chat',
                $newFilename
            );

            $msg->setImage('uploads/chat/' . $newFilename);
        } catch (FileException $e) {
            return $this->json(['error' => 'Upload failed']);
        }
    }

    $em->persist($msg);
    $em->flush();

    return $this->json(['status' => 'ok']);
}



#[Route('/chat/render-message', name: 'chat_render_message', methods: ['POST'])]
public function renderMessage(Request $request): Response
{
    $content = $request->getContent();

    $data = json_decode($content, true);

    if (!is_array($data)) {
        return $this->json([
            'error' => 'Invalid JSON',
            'raw' => $content
        ], 400);
    }

            return $this->render('chat/_message.html.twig', [
            'message' => $data['message'] ?? '',
            'image' => $data['image'] ?? null,
            'type' => $data['type'] ?? 'received',
            'time' => $data['time'] ?? '',
            'sender_name' => $data['sender_name'] ?? null,
            'sender_photo' => $data['sender_photo'] ?? null
            
]);


}

#[Route('/chat/messages/{id}', name: 'chat_messages', methods: ['GET'])]
public function getMessages($id, EntityManagerInterface $em): JsonResponse
{
    $conv = $em->getRepository(Conversation::class)->find($id);

    if (!$conv) {
        return $this->json([]);
    }

    $messages = $em->getRepository(Message::class)
        ->createQueryBuilder('m')
        ->where('m.conversation = :conv')
        ->setParameter('conv', $conv) 
        ->orderBy('m.sent_at', 'ASC')
        ->getQuery()
        ->getResult();

    $data = [];

    foreach ($messages as $msg) {
        $data[] = [
    'contenu' => $msg->getContenu(),
    'image' => $msg->getImage(),
    'sender_id' => $msg->getSender_id()?->getId(),
    'sender_name' => $msg->getSender_id()?->getNom(),
    'sent_at' => $msg->getSent_at()->format('H:i'),
    'sender_photo' => $msg->getSender_id()?->getProfile_photo(),
];
    }

    return $this->json($data);
}











/*------------------------------------------------------posting--------------------------------------------------------*/

private function handlePostCreation( $form, Post $post, $personne, ImageUploader $uploader, EntityManagerInterface $em, ToxicityChecker $toxicityChecker ): ?string
{
    if (!$form->isSubmitted() || !$form->isValid()) {
        return null;
    }

    $contenu = $post->getContenu();

    $result = $toxicityChecker->check($contenu);
    $score = $result['score'];

    if ($score > 0.7) {
        return 'refused';
    }

    $status = ($score > 0.1) ? 'warning' : 'ok';


    $imageFile = $form->get('image')->getData();
    $imagePath = $uploader->upload($imageFile, $this->getParameter('images_directory'));

    if ($imagePath) {
        $post->setImage($imagePath);
    }

    $post->setDatePublication(new \DateTime());
    $post->setPopularite(0);
    $post->setPersonne_id($personne);

    $em->persist($post);
    $em->flush();
    if ($status === 'warning') {

    // récupérer tous les admins
    $admins = $em->getRepository(Personne::class)
                 ->findBy(['role' => 'ADMIN']); 

    foreach ($admins as $admin) {

        $notification = new Notification();

        $message = $personne->getNom() . ' ' . $personne->getPrenom()
            . ' a publié un post jugée suspect (score: ' . round($score, 2) . ') | Post ID: ' . $post->getId();

        $notification->setMessage($message);
        $notification->setType('POST');
        $notification->setPost_id($post); 
        $notification->setSender_id($personne);
        $notification->setReceiver_id($admin);
        $notification->setIs_read(false);
        $notification->setCreated_at(new \DateTime());
        $notification->setIs_sent_sms(false);

        $em->persist($notification);
    }

    $em->flush();
}

    return $status;
}

#[Route('/check-toxicity', name: 'check_toxicity', methods: ['POST'])]
public function checkToxicity(Request $request, ToxicityChecker $toxicityChecker): JsonResponse
{
    $data = json_decode($request->getContent(), true);

    $text = $data['text'] ?? '';

    if (!$text) {
        return new JsonResponse(['error' => 'empty text'], 400);
    }

    try {
        $result = $toxicityChecker->check($text);

        return new JsonResponse([
            'score' => $result['score'],
            'label' => $result['label'] ?? null
        ]);

    } catch (\Exception $e) {
        return new JsonResponse([
            'error' => 'Flask error'
        ], 500);
    }
}
private function getFilteredPosts(Request $request, EntityManagerInterface $em)
{
    $search = $request->query->get('search');
    $likes = $request->query->get('likes');
    $date = $request->query->get('date');
    $dateExact = $request->query->get('date_exact');
    $sort = $request->query->get('sort');
    $order = $request->query->get('order');

    $repo = $em->getRepository(Post::class);

    if ($search) {
        return $repo->searchByContent($search);
    }

    if ($likes) {
        return $repo->findByLikes($likes);
    }

    if ($dateExact) {
        return $repo->findByExactDate($dateExact);
    }

    if ($date) {
        return $repo->findByDateFilter($date);
    }

    if ($sort && $order) {
        return $repo->findSorted($sort, $order);
    }

    return $repo->findLatestPosts();
}

#[Route('/post/delete/{id}', name: 'post_delete', methods: ['POST'])]
public function delete(Post $post, EntityManagerInterface $em): Response
{
    $em->remove($post);
    $em->flush();

    return $this->redirectToRoute('blog');
}
#[Route('/post/edit/{id}', name: 'post_edit', methods: ['POST'])]
public function edit(Request $request, Post $post, EntityManagerInterface $em): Response
{
    $post->setContenu($request->request->get('contenu'));

    $imageFile = $request->files->get('image');
    if ($imageFile) {
        $newFilename = uniqid().'.'.$imageFile->guessExtension();
        $imageFile->move($this->getParameter('images_directory'), $newFilename);
        $post->setImage('uploads/'.$newFilename);
    }

    $em->flush();

    return $this->redirectToRoute('blog');
}


#[Route('/comment/add/{id}', name: 'comment_add', methods: ['POST'])]
public function addComment(
    Request $request, 
    Post $post, 
    EntityManagerInterface $em,
    ToxicityChecker $toxicityChecker
) {
    $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);

    $contenu = trim($request->request->get('contenu'));

    if (!$contenu) {
        return $this->redirectToRoute('blog');
    }

    // 🔥 Analyse toxicité
    $result = $toxicityChecker->check($contenu);
    $score = $result['score'];

    // ❌ REFUS
    if ($score > 0.7) {
        $this->addFlash('error', '❌ Commentaire refusé (toxique)');
        return $this->redirectToRoute('blog');
    }

    $status = ($score > 0.1) ? 'warning' : 'ok';

    // ✅ Création commentaire
    $comment = new Commentaire();
    $comment->setContenu($contenu);
    $comment->setDateCommentaire(new \DateTime());
    $comment->setPost_id($post);
    $comment->setPersonne_id($personne);

    $em->persist($comment);
    $em->flush();

    // ⚠️ Notification admin si warning
    if ($status === 'warning') {

        $admins = $em->getRepository(Personne::class)
                     ->findBy(['role' => 'ADMIN']);

        foreach ($admins as $admin) {

            $notification = new Notification();

            $message = $personne->getNom() . ' ' . $personne->getPrenom()
                . ' a ajouté  un commentaire jugée suspect (score: ' . round($score, 2) . ')'
                . ' | Comment ID: ' . $comment->getId();

            $notification->setMessage($message);
            $notification->setType('COMMENT');
            $notification->setComment_id($comment); 
            $notification->setPost_id($post);
            $notification->setSender_id($personne);
            $notification->setReceiver_id($admin);
            $notification->setIs_read(false);
            $notification->setCreated_at(new \DateTime());
            $notification->setIs_sent_sms(false);

            $em->persist($notification);
        }

        $em->flush();
    }

    // 🔥 Flash message UX
    if ($status === 'warning') {
        $this->addFlash('warning', '⚠️ Commentaire sensible publié');
    } else {
        $this->addFlash('success', '✅ Commentaire ajouté');
    }

    return $this->redirectToRoute('blog');
}
#[Route('/comment/delete/{id}', name: 'comment_delete', methods: ['POST'])]
public function deleteComment(Commentaire $comment, EntityManagerInterface $em): Response
{
    $em->remove($comment);
    $em->flush();

    return $this->redirectToRoute('blog');
}


#[Route('/comment/edit/{id}', name: 'comment_edit', methods: ['POST'])]
public function editComment(Request $request, Commentaire $comment, EntityManagerInterface $em)
{
    $contenu = $request->request->get('contenu');

    if (!$contenu) {
        return new JsonResponse(['error' => 'vide'], 400);
    }

    $comment->setContenu($contenu);
    $em->flush();

    return new JsonResponse([
        'contenu' => $contenu
    ]);
}

#[Route('/post/like/{id}', name: 'post_like', methods: ['POST'])]
public function like(Post $post, EntityManagerInterface $em, Request $request): JsonResponse
{
    $userId = $request->getSession()->get('user_id');
    $personne = $em->getRepository(Personne::class)->find($userId);

    if (!$personne) {
        return new JsonResponse(['error' => 'user not connected'], 403);
    }

    $likeRepo = $em->getRepository(Likes::class);

    
    $existing = $likeRepo->findOneBy([
        'personne_id' => $personne,
        'post_id' => $post
    ]);

    if ($existing) {
        $em->remove($existing);
        $em->flush();

        return new JsonResponse([
            'liked' => false,
            'count' => $likeRepo->count(['post_id' => $post])
        ]);
    }

    $like = new Likes();
    $like->setPersonne_id($personne);
    $like->setPost_id($post);
    $like->setStatut(true);

    $em->persist($like);
    $em->flush();

    return new JsonResponse([
        'liked' => true,
        'count' => $likeRepo->count(['post_id' => $post])
    ]);
}


#[Route('/localiser-image', name: 'localiser_image', methods: ['POST'])]
    public function localiserImage(
        Request $request,
        GeoLocalisationService $geoService
    ): JsonResponse {

        $file = $request->files->get('image');

        if (!$file) {
            return $this->json([
                'error' => 'Aucune image envoyée'
            ], 400);
        }

        try {
            $result = $geoService->localize($file->getPathname());

            return $this->json($result);

        } catch (\Exception $e) {
            return $this->json([
                'error' => 'Erreur serveur : ' . $e->getMessage()
            ], 500);
        }
    }

    #[Route('/post/{id}/share', name: 'post_share', methods: ['POST'])]
public function share(
    $id,
    EntityManagerInterface $em,
    CloudinaryService $cloudinary,
    AyrshareService $ayrshare
): JsonResponse {

    try {

        $post = $em->getRepository(Post::class)->find($id);

        if (!$post) {
            return new JsonResponse(['status' => 'error', 'message' => 'Post not found'], 404);
        }

        $mediaUrls = [];

        if ($post->getImage()) {

            $imagePath = $this->getParameter('kernel.project_dir') . '/public/' . $post->getImage();

            if (!file_exists($imagePath)) {
                throw new \Exception("Image introuvable: " . $imagePath);
            }

            $url = $cloudinary->uploadImage($imagePath);
            $mediaUrls[] = $url;
        }

        $paramsMedia = $mediaUrls;

        $result = $ayrshare->sharePost(
            $post->getContenu(),
            ['facebook'],
            $paramsMedia
        );

        return new JsonResponse([
            'status' => 'success',
            'data' => $result
        ]);

    } catch (\Exception $e) {

        return new JsonResponse([
            'status' => 'error',
            'message' => $e->getMessage()
        ], 500);
    }
}





}
