# Rehla – Travel & Leisure Management Platform

## 🌍 Overview

Rehla is a comprehensive desktop application built with Java/JavaFX designed to centralize and simplify travel and leisure activities planning. The platform provides users with an integrated environment to explore destinations, discover activities, interact through social features, and manage reservations seamlessly.

**Academic Context:**
Developed as part of PIDEV – 3rd Year Engineering Program at Esprit School of Engineering (Academic Year 2025–2026)

**Project Goal:** Solve fragmentation in existing travel platforms by offering a unified, intuitive, and feature-rich desktop solution.

## 🚀 Key Features

### 👥 User Management
- Multi-role authentication (User, Guide, Admin)
- Comprehensive profile management (update, delete, deactivate)
- Role-based access control and permissions
- Session management and security

### 📝 Social Features
- Create, edit, and delete posts with basic content management
- Like and favorite posts system
- Comment on posts with moderation
- Trending posts based on popularity ranking
- Basic post validation and content management
- Post sharing to social media platforms (Ayrshare integration)
- Chat/messaging system with WebSocket support
- AI-powered chatbot for travel assistance and recommendations

### 🎯 Activities Management
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

### 🌍 Destinations Management
- Browse destinations (countries, cities, attractions) with pagination
- Real-time search and filtering by country name
- Admin manages destinations and attractions (full CRUD operations)
- Image management with automatic file handling
- Visit count tracking for countries and cities
- Geographic clustering for similar city recommendations
- Top destinations ranking by composite score (reservations, visits, attractions)

### 📅 Reservations Management
- Complete booking system for activities and destinations
- View and manage personal reservations with status tracking
- Generate printable tickets with booking details
- Admin reservation and ticket management interface
- Integrated secure payment during reservation with invoice generation
- Destination weather forecast displayed
- Automatic currency conversion based on the user's preferred currency

### 💬 Posts Management
- Add comments with text validation and regex patterns
- Like/unlike posts with tracking and counting
- Admin moderation for comments and posts
- Comment management with minimum length validation
- Post likes tracking and display with sorting capabilities
- Admin validation workflow (approve/reject) with notifications

### ⭐ Favorites System
- Add/remove posts to personal favorites
- View favorites list
- Basic favorites management with database storage

### 📊 Admin Dashboard
- Comprehensive user and guide management
- Content moderation (posts, comments, activities)
- Global statistics and analytics
- System monitoring and reporting

### 🤖 AI-Powered Features
- **DestinationGeminiService**: AI-generated country descriptions for tourism marketing
- **AiDescriptionService**: Professional activity descriptions with marketing tone
- **MistralRecommendationService**: AI-powered activity recommendations
- **ChatbotService**: AI-powered travel assistance and recommendations using Gemini models
- **UserRiskAnalysisService**: AI-powered user behavior analysis and risk assessment
- **WeatherActivityTipsService**: AI-generated activity tips based on weather and reviews
- **Currency Exchange Service**: Real-time TND to EUR/USD exchange rates for international pricing
- **Multi-language Support**: French content with English typeTourisme terms

## 🛠 Tech Stack

### Frontend Technologies
- **JavaFX 21.0.2** - Modern desktop application framework
- **FXML** - Declarative UI markup language
- **CSS3** - Styling and theming
- **Java 17** - Core programming language

### Backend Technologies
- **Java 17** - Object-oriented programming
- **Maven** - Build automation and dependency management
- **MySQL Database** - Relational data storage
- **JDBC** - Database connectivity
- **File System Handling** - Secure uploads management
- **Security System** - Authentication & authorization
- **PDF Generation** - iText 7 for ticket generation
- **QR Code Generation** - ZXing library
- **Email Services** - Jakarta Mail integration
- **WebSocket Support** - Custom socket server for real-time messaging
- **Payment Integration** - Stripe payment processing
- **SMS Integration** - Twilio for OTP and notifications
- **Face Recognition** - Webcam capture integration
- **Environment Configuration** - Dotenv for secure configuration

## 🏗 Architecture

### Layered MVC Architecture
```
┌─────────────────────────────────────────┐
│              Presentation Layer         │
│  ┌─────────────────────────────┐        │
│  │    Views (FXML + CSS)       │        │
│  └─────────────────────────────┘        │
│              Controllers                │
│  ┌─────────────────────────────┐        │
│  │   Business Logic Layer      │        │
│  └─────────────────────────────┘        │
│              Services                   │
│  ┌─────────────────────────────┐        │
│  │      Data Access Layer      │        │
│  └─────────────────────────────┘        │
│           Models                      │
│  ┌─────────────────────────────┐        │
│  │      Data Models            │        │
│  └─────────────────────────────┘        │
│             Entities                    │
└─────────────────────────────────────────┘
```

### Core Modules
- **User Management Module** - Authentication and profiles
- **Travel Content Module** - Destinations, attractions, activities
- **Social Module** - Posts, comments, likes
- **Reservation Module** - Booking and ticketing
- **Admin Dashboard Module** - Management and analytics
- **Socket Server Module** - Real-time messaging

## 📁 Project Structure

```
Rehla/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── Controllers/      # FXML controllers
│   │   │   ├── models/          # Data models
│   │   │   ├── services/        # Business logic
│   │   │   ├── util/            # Utility classes
│   │   │   ├── SocketServer/    # WebSocket server
│   │   │   └── rehla/           # Main application
│   │   └── resources/
│   │       ├── Frontoffice/      # Frontend FXML files
│   │       ├── Backoffice/       # Backend FXML files
│   │       ├── css/             # Stylesheets
│   │       └── images/          # Static images
│   └── test/                    # Unit tests
├── uploads/                    # File storage
├── pom.xml                     # Maven configuration
└── .env                        # Environment variables
```

## Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/Kmarmezghani/Esprit-PIDEV-3A26-2526-Rehla
```

### 2. Install dependencies
```bash
mvn clean install
```

### 3. Configure environment
Database configuration is hardcoded in `src/main/java/util/DBConnection.java`:
```java
private final String url = "jdbc:mysql://localhost:3306/rehla";
private final String user = "root";
private final String password = "";
```

For external API integrations (Stripe, Twilio), create `.env` file in project root:
```
STRIPE_SECRET_KEY=your_stripe_secret_key
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
```

### 4. Create database
```bash
# Create MySQL database
mysql -u root -e "CREATE DATABASE rehla;"
```

### 5. Run the application
```bash
mvn javafx:run
```

## 🔧 Configuration

### Database Configuration
```bash
# JDBC URL format for MySQL
DB_URL=jdbc:mysql://localhost:3306/rehla
DB_USER=root
DB_PASSWORD=
```

### File Upload Configuration
```java
// File paths are configured in util classes
String uploadsDirectory = "uploads/";
String activitiesDirectory = "uploads/activities/";
String imagesDirectory = "uploads/images/";
```

## 📊 Performance Optimizations

### Database Optimizations
- **Query Optimization**: Efficient JDBC query implementation
- **Indexing**: Proper database indexes for search queries
- **Connection Management**: Efficient database connection handling
- **Batch Operations**: Optimized bulk data operations

### Frontend Optimizations
- **Image Compression**: Automatic image optimization
- **Lazy Loading**: On-demand content loading in JavaFX
- **CSS Optimization**: Efficient styling and theming
- **FXML Caching**: UI component caching for better performance

### Performance Metrics
- **Query Optimization**: Efficient JDBC queries with prepared statements
- **Image Processing**: Optimized file handling and compression
- **Memory Management**: Efficient JavaFX scene graph management

## 🔒 Security Features

### Authentication & Authorization
- **Role-based Access Control**: User, Guide, Admin roles
- **Session Management**: Secure session handling in JavaFX
- **Password Encryption**: Bcrypt hashing
- **Face Recognition**: Webcam-based authentication

### Data Protection
- **Input Validation**: Form validation with JavaFX constraints
- **SQL Injection Prevention**: Prepared statements with JDBC
- **Environment Variables**: Secure configuration with Dotenv
- **User Authorization**: Reservation ownership verification

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Run Static Analysis
```bash
mvn clean compile
```

### Test Coverage
- **Unit Tests**: Service layer validation (Controllers, Services, Models)
- **Service Tests**: Business logic validation and edge cases
- **Integration Tests**: Database connectivity and external API integration

## 📚 API Documentation

### External Services Integration
- **Stripe API** - Payment processing and invoice generation
- **Twilio API** - SMS notifications and OTP verification
- **Weather API** - Weather forecast integration for destinations
- **Currency Exchange API** - Real-time TND to EUR/USD conversion
- **AI Services** - Integration with various AI providers for recommendations and descriptions

### Service Classes
- **DestinationGeminiService** - AI-generated country descriptions
- **AiDescriptionService** - Professional activity descriptions
- **MistralRecommendationService** - AI-powered activity recommendations
- **ChatbotService** - AI-powered travel assistance
- **UserRiskAnalysisService** - User behavior analysis
- **WeatherActivityTipsService** - Weather-based activity recommendations
- **Currency Exchange Service** - Real-time exchange rates


## 🤝 Contributing

### Development Workflow
1. Fork the repository
2. Create feature branch: `git checkout -b branchefinalTest`
3. Make changes and commit
4. Push to fork: `git push origin branchefinalTest`
5. Create Pull Request

## 👥 Contributors

### Development Team
- Amamou Yosr
- Bouriga Zeineb
- Mezghani Kmar
- El Ayech Mohamed Rayen
- Trabelsi Anissa

### Academic Supervision
- Esprit School of Engineering – Tunisia
- PIDEV – 3rd Year Engineering Program
- Academic Year: 2025–2026

## 🙏 Acknowledgments

We would like to thank:

- Esprit School of Engineering for academic supervision and guidance
- Teaching staff of PIDEV module for their continuous support
- All contributors involved in development and testing
- Open-source community for tools and libraries
- Symfony Community for excellent documentation and support

---

**Rehla – Your Travel Companion** © 2026. All rights reserved.
