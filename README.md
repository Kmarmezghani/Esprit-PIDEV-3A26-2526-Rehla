# Rehla – Travel & Leisure Management Platform

## 🌍 Overview

Rehla is a comprehensive full-stack web application designed to centralize and simplify travel and leisure activities planning. The platform provides users with an integrated environment to explore destinations, discover activities, interact through social features, and manage reservations seamlessly.

**Academic Context:**  
Developed as part of **PIDEV – 3rd Year Engineering Program at Esprit School of Engineering** (Academic Year 2025–2026)

**Project Goal:** Solve fragmentation in existing travel platforms by offering a unified, intuitive, and feature-rich solution.

---

## 🚀 Key Features

### **👥 User Management**
- Multi-role authentication (User, Guide, Admin)
- Comprehensive profile management (update, delete, deactivate)
- Role-based access control and permissions
- Session management and security

### **📝 Social Features**
- Create, edit, and delete posts with basic content management
- Like and favorite posts system
- Comment on posts with moderation
- Trending posts based on popularity ranking
- Basic post validation and content management
- Post sharing to social media platforms (Ayrshare integration)
- Chat/messaging system with WebSocket support
- AI-powered chatbot for travel assistance and recommendations

### **🎯 Activities Management**
- Browse tourist activities with pagination
- Search and filtering by destination, date range, and maximum price
- Activity participation system with booking integration
- Guide activity creation and management
- Full CRUD operations for activity management
- Activity rating system (average ratings from reviews)
- Review system with user feedback and moderation
- Activity status management (available, unavailable, flash sales)
- Email confirmations for successful bookings
- Waitlist management for fully booked activities
- Cancellation workflow with email notifications

### **🌍 Destinations Management**
- Browse destinations (countries, cities, attractions) with pagination
- Real-time search and filtering by country name
- Admin manages destinations and attractions (full CRUD operations)
- Image management with automatic file handling
- Visit count tracking for countries and cities
- Geographic clustering for similar city recommendations
- Top destinations ranking by composite score (reservations, visits, attractions)

### **📅 Reservations Management**
- Complete booking system for activities and destinations
- View and manage personal reservations with status tracking
- Generate printable tickets with booking details
- Admin reservation and ticket management interface

### **💬 Posts Management**
- Add comments with text validation and regex patterns
- Like/unlike posts with tracking and counting
- Admin moderation for comments and posts
- Comment management with minimum length validation
- Post likes tracking and display with sorting capabilities
- Admin validation workflow (approve/reject) with notifications

### **⭐ Favorites System**
- Add/remove posts to personal favorites
- View favorites list
- Basic favorites management with database storage

### **📊 Admin Dashboard**
- Comprehensive user and guide management
- Content moderation (posts, comments, activities)
- Global statistics and analytics
- System monitoring and reporting

### **🤖 AI-Powered Features**
- **DestinationGeminiService:** AI-generated country descriptions for tourism marketing
- **AiDescriptionService:** Professional activity descriptions with marketing tone
- **MistralRecommendationService:** AI-powered activity recommendations
- **ChatbotService:** AI-powered travel assistance and recommendations using Gemini models
- **UserRiskAnalysisService:** AI-powered user behavior analysis and risk assessment
- **WeatherActivityTipsService:** AI-generated activity tips based on weather and reviews
- **Currency Exchange Service:** Real-time TND to EUR/USD exchange rates for international pricing
- **Multi-language Support:** French content with English typeTourisme terms

---

## 🛠 Tech Stack

### **Frontend Technologies**
- **HTML5 / CSS3** - Modern web standards
- **JavaScript** - Interactive functionality
- **Bootstrap** - Responsive design framework
- **Twig** - Symfony templating engine

### **Backend Technologies**
- **Symfony Framework** (PHP 8.1+) - Robust MVC architecture
- **Doctrine ORM** - Database abstraction layer
- **MySQL Database** - Relational data storage
- **File System Handling** - Secure uploads management
- **Security System** - Authentication & authorization
- **PDF Generation** - DomPDF for ticket generation
- **QR Code Generation** - Endroid QR code bundle
- **Email Services** - Google Mailer integration
- **Pagination** - KNP Paginator bundle
- **Image Processing** - Liip Imagine bundle
- **WebSocket Support** - Ratchet for real-time messaging
- **Cloud Storage** - Cloudinary for file uploads
- **Payment Integration** - Stripe payment processing

---

## 🏗 Architecture

### **Layered MVC Architecture**
```
┌─────────────────────────────────────────┐
│              Presentation Layer         │
│  ┌─────────────────────────────┐        │
│  │    Views (Twig Templates)   │        │
│  └─────────────────────────────┘        │
│              Controllers                │
│  ┌─────────────────────────────┐        │
│  │   Business Logic Layer      │        │
│  └─────────────────────────────┘        │
│              Services                   │
│  ┌─────────────────────────────┐        │
│  │      Data Access Layer      │        │
│  └─────────────────────────────┘        │
│           Repositories                  │
│  ┌─────────────────────────────┐        │
│  │      Data Models            │        │
│  └─────────────────────────────┘        │
│             Entities                    │
└─────────────────────────────────────────┘
```

### **Core Modules**
1. **User Management Module** - Authentication and profiles
2. **Travel Content Module** - Destinations, attractions, activities
3. **Social Module** - Posts, comments, likes
4. **Reservation Module** - Booking and ticketing
5. **Admin Dashboard Module** - Management and analytics
---

## 📁 Project Structure

```
Rehla/
├── src/
│   ├── Controller/          # HTTP request handlers
│   ├── Entity/            # Data models
│   ├── Repository/         # Database queries
│   ├── Service/           # Business logic
│   └── Form/              # Form handling
├── templates/             # Twig templates
├── public/
│   ├── uploads/           # File storage
│   └── assets/            # Static assets
├── config/               # Configuration files
├── migrations/            # Database schema
└── tests/               # Unit tests
```

---

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/Kmarmezghani/Esprit-PIDEV-3A26-2526-Rehla
```

---

### 2. Install dependencies
```bash
composer install
```

---

### 3. Configure environment
Update `.env` file:

DATABASE_URL="mysql://username:password@127.0.0.1:3306/rehla"

---

### 4. Create database
```bash
php bin/console doctrine:database:create
php bin/console doctrine:migrations:migrate
```

---

### 5. Run the server
```bash
php -S localhost:8000 -c C:\xampp\php\php.ini -t public
```

---

## 🔧 Configuration

### **Database Configuration**
```env
# DATABASE_URL format for MySQL
DATABASE_URL="mysql://root:@127.0.0.1:3306/rehla"
```

### **File Upload Configuration**
```yaml
# config/services.yaml
parameters:
    activities_directory: '%kernel.project_dir%/public/uploads/activities'
    images_directory: '%kernel.project_dir%/public/uploads'
```

---

## 📊 Performance Optimizations

### **Database Optimizations**
- **N+1 Query Resolution:** Reduced from 12 to 7 queries per page
- **Query Optimization:** Implemented JOIN FETCH strategies
- **Indexing:** Proper database indexes for search queries
- **Connection Pooling:** Efficient database connection management

### **Frontend Optimizations**
- **Image Compression:** Automatic image optimization
- **Lazy Loading:** On-demand content loading
- **Caching:** Browser and server-side caching
- **CDN Ready:** Asset optimization for production

### **Performance Metrics**
- **Query Optimization:** Implemented efficient database queries
- **Image Processing:** Optimized file handling and compression
- **Caching Strategy:** Browser and server-side caching implemented

---

## 🔒 Security Features

### **Authentication & Authorization**
- **Role-based Access Control:** User, Guide, Admin roles
- **Session Management:** Secure session handling
- **Password Encryption:** Bcrypt hashing

### **Data Protection**
- **Input Validation:** Form validation with constraints
- **SQL Injection Prevention:** Parameterized queries
- **CSRF Protection:** Token-based form protection
- **User Authorization:** Reservation ownership verification

---

## 🧪 Testing

### **Run Unit Tests**
```bash
php bin/phpunit
```

### **Run Static Analysis**
```bash
php vendor/bin/phpstan analyse src/
```

### **Test Coverage**
- **Unit Tests:** Service layer validation (ActiviteManager, AttractionValidator, PostManager, ReservationManager, UserManager)
- **Service Tests:** Business logic validation and edge cases
- **Manager Tests:** Activity, attraction, post, reservation, and user management validation

---

## 📚 API Documentation

### **Authentication API**
```
POST /api/login                    # User login authentication
GET  /api/check-email              # Check email availability
POST /api/check-password-pwned     # Check if password is compromised
```

### **User Management API**
```
GET  /api/users                    # List users (admin only, requires API key)
```

### **AI & Recommendations API**
```
POST /api/recommandation-ia        # AI-powered activity recommendations
POST /api/recommandation-ia/creer  # Create reservation from AI recommendation
```

### **External Services API**
```
GET  /api/exchange-rates           # Get currency exchange rates (TND to EUR/USD)
GET  /api/tickets-by-destination   # Get tickets by destination
POST /api/circuit/generate         # Generate optimized travel circuits
```

### **Admin Management API**
```
GET  /api/countries/search         # Search countries (admin)
GET  /api/cities/search            # Search cities (admin)
GET  /api/cities/country           # Get cities by country (admin)
POST /api/ai/generate-description # Generate AI descriptions (admin)
POST /api/ai/suggest-ville-profile # AI city profile suggestions (admin)
POST /api/generate-image           # Generate images from Unsplash (admin)
POST /api/clustering               # K-means clustering analysis (admin)
```

### **Weather API**
```
GET  /meteo/{ville}/{date}         # Get weather forecast for city and date
```

---

## 🚀 Deployment

### **Production Setup**
1. **Environment Variables:** Set production `.env` values
2. **Database:** Configure production database
3. **Web Server:** Configure Apache/Nginx
4. **Start Application:** Use PHP built-in server or web server

---

## 🤝 Contributing

### **Development Workflow**
1. Fork the repository
2. Create feature branch: `git checkout -b branchefinalSymfony`
3. Make changes and commit
4. Push to fork: `git push origin branchefinalSymfony`
5. Create Pull Request

---

## 👥 Contributors

### **Development Team**
- **Amamou Yosr** 
- **Bouriga Zeineb** 
- **Mezghani Kmar** 
- **El Ayech Mohamed Rayen** 
- **Trabelsi Anissa** 


### **Academic Supervision**
- **Esprit School of Engineering – Tunisia**
- **PIDEV – 3rd Year Engineering Program**
- **Academic Year:** 2025–2026

---

## 🙏 Acknowledgments

We would like to thank:

- **Esprit School of Engineering** for academic supervision and guidance
- **Teaching staff of PIDEV module** for their continuous support
- **All contributors** involved in development and testing
- **Open-source community** for tools and libraries
- **Symfony Community** for excellent documentation and support

---

