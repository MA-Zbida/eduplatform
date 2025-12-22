package com.example.demo.config;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Course;
import com.example.demo.entity.CourseStatus;
import com.example.demo.entity.Enrollment;
import com.example.demo.entity.EnrollmentStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RAGService;

/**
 * Data Loader - Initializes sample data for demonstration
 * This runs automatically on application startup
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final RAGService ragService;

    public DataLoader(UserRepository userRepository, CourseRepository courseRepository,
                      EnrollmentRepository enrollmentRepository, PasswordEncoder passwordEncoder,
                      RAGService ragService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.ragService = ragService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("===========================================");
        log.info("   EDUCATIONAL PLATFORM - DATA LOADER");
        log.info("===========================================");

        if (userRepository.count() > 0) {
            log.info("Data already exists, skipping initialization");
            return;
        }

        // Create Administrator
        User admin = createAdmin();
        
        // Create Sample Students
        List<User> students = createStudents();
        
        // Create Sample Courses
        List<Course> courses = createCourses(admin);
        
        // Create Enrollments
        createEnrollments(students, courses);

        log.info("===========================================");
        log.info("   DATA INITIALIZATION COMPLETE");
        log.info("===========================================");
        log.info("");
        log.info("   DEMO CREDENTIALS:");
        log.info("   -----------------");
        log.info("   Admin:   admin / admin123");
        log.info("   Student: student1 / student123");
        log.info("   Student: student2 / student123");
        log.info("   Student: student3 / student123");
        log.info("");
        log.info("===========================================");
    }

    private User createAdmin() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@eduplatform.com");
        admin.setFullName("System Administrator");
        admin.setRole(Role.ADMINISTRATOR);
        admin.setEnabled(true);
        admin.setCreatedAt(LocalDateTime.now());
        
        userRepository.save(admin);
        log.info("Created administrator: admin");
        
        return admin;
    }

    private List<User> createStudents() {
        User student1 = new User();
        student1.setUsername("student1");
        student1.setPassword(passwordEncoder.encode("student123"));
        student1.setEmail("student1@eduplatform.com");
        student1.setFullName("Alice Johnson");
        student1.setRole(Role.STUDENT);
        student1.setEnabled(true);
        student1.setCreatedAt(LocalDateTime.now());

        User student2 = new User();
        student2.setUsername("student2");
        student2.setPassword(passwordEncoder.encode("student123"));
        student2.setEmail("student2@eduplatform.com");
        student2.setFullName("Bob Smith");
        student2.setRole(Role.STUDENT);
        student2.setEnabled(true);
        student2.setCreatedAt(LocalDateTime.now());

        User student3 = new User();
        student3.setUsername("student3");
        student3.setPassword(passwordEncoder.encode("student123"));
        student3.setEmail("student3@eduplatform.com");
        student3.setFullName("Carol Davis");
        student3.setRole(Role.STUDENT);
        student3.setEnabled(true);
        student3.setCreatedAt(LocalDateTime.now());

        List<User> students = userRepository.saveAll(List.of(student1, student2, student3));
        log.info("Created {} sample students", students.size());
        
        return students;
    }

    private List<Course> createCourses(User admin) {
        Course javaCourse = new Course();
        javaCourse.setTitle("Introduction to Java Programming");
        javaCourse.setDescription("Learn the fundamentals of Java programming language, including object-oriented concepts, data structures, and algorithms.");
        javaCourse.setContent(getJavaCourseContent());
        javaCourse.setStatus(CourseStatus.PUBLISHED);
        javaCourse.setIndexed(true);
        javaCourse.setCreatedAt(LocalDateTime.now());
        javaCourse.setPublishedAt(LocalDateTime.now());
        javaCourse.setCreatedBy(admin);

        Course springCourse = new Course();
        springCourse.setTitle("Spring Boot Essentials");
        springCourse.setDescription("Master Spring Boot framework for building modern web applications with Java.");
        springCourse.setContent(getSpringBootContent());
        springCourse.setStatus(CourseStatus.PUBLISHED);
        springCourse.setIndexed(true);
        springCourse.setCreatedAt(LocalDateTime.now());
        springCourse.setPublishedAt(LocalDateTime.now());
        springCourse.setCreatedBy(admin);

        Course aiCourse = new Course();
        aiCourse.setTitle("Introduction to Artificial Intelligence");
        aiCourse.setDescription("Explore the fundamentals of AI, machine learning, and their applications in modern technology.");
        aiCourse.setContent(getAICourseContent());
        aiCourse.setStatus(CourseStatus.PUBLISHED);
        aiCourse.setIndexed(true);
        aiCourse.setCreatedAt(LocalDateTime.now());
        aiCourse.setPublishedAt(LocalDateTime.now());
        aiCourse.setCreatedBy(admin);

        Course draftCourse = new Course();
        draftCourse.setTitle("Advanced Database Design (Draft)");
        draftCourse.setDescription("Coming soon - Advanced topics in database design and optimization.");
        draftCourse.setContent("This course is currently under development...");
        draftCourse.setStatus(CourseStatus.DRAFT);
        draftCourse.setIndexed(false);
        draftCourse.setCreatedAt(LocalDateTime.now());
        draftCourse.setCreatedBy(admin);

        List<Course> courses = courseRepository.saveAll(List.of(javaCourse, springCourse, aiCourse, draftCourse));
        log.info("Created {} sample courses ({} published)", courses.size(), courses.stream().filter(c -> c.getStatus() == CourseStatus.PUBLISHED).count());
        
        // Index published courses for RAG
        for (Course course : courses) {
            if (course.getStatus() == CourseStatus.PUBLISHED && course.isIndexed()) {
                ragService.indexCourse(course);
                log.info("Indexed course for RAG: {}", course.getTitle());
            }
        }
        
        return courses;
    }

    private void createEnrollments(List<User> students, List<Course> courses) {
        // Get published courses
        List<Course> publishedCourses = courses.stream()
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .toList();

        // Enroll student1 in all published courses
        for (Course course : publishedCourses) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(students.get(0));
            enrollment.setCourse(course);
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            enrollment.setProgressPercentage(0);
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }

        // Enroll student2 in Java course only
        Enrollment enrollment2 = new Enrollment();
        enrollment2.setStudent(students.get(1));
        enrollment2.setCourse(publishedCourses.get(0)); // Java course
        enrollment2.setStatus(EnrollmentStatus.IN_PROGRESS);
        enrollment2.setProgressPercentage(25);
        enrollment2.setEnrolledAt(LocalDateTime.now().minusDays(5));
        enrollmentRepository.save(enrollment2);

        log.info("Created sample enrollments");
    }

    private String getJavaCourseContent() {
        return """
            # Introduction to Java Programming
            
            ## Chapter 1: Getting Started with Java
            
            Java is a high-level, class-based, object-oriented programming language that is designed to have as few implementation dependencies as possible. It was developed by James Gosling at Sun Microsystems and released in 1995.
            
            ### Key Features of Java:
            - Platform Independence: Java code can run on any platform that has a JVM (Java Virtual Machine)
            - Object-Oriented: Everything in Java is an object with data and behavior
            - Robust and Secure: Java has strong memory management and security features
            - Multithreaded: Java supports concurrent execution of multiple threads
            
            ## Chapter 2: Variables and Data Types
            
            In Java, variables are containers for storing data values. Java is a statically-typed language, which means all variables must be declared before use.
            
            ### Primitive Data Types:
            1. byte - 8-bit signed integer (-128 to 127)
            2. short - 16-bit signed integer
            3. int - 32-bit signed integer (most commonly used)
            4. long - 64-bit signed integer
            5. float - 32-bit floating point
            6. double - 64-bit floating point (most commonly used for decimals)
            7. boolean - true or false values
            8. char - single 16-bit Unicode character
            
            ### Variable Declaration Example:
            int age = 25;
            String name = "Alice";
            double salary = 50000.50;
            boolean isEmployed = true;
            
            ## Chapter 3: Control Flow Statements
            
            Control flow statements allow you to control the order in which statements are executed in your program.
            
            ### If-Else Statements:
            The if statement executes a block of code if a specified condition is true. The else block executes if the condition is false.
            
            ### Switch Statements:
            Switch statements provide an alternative to multiple if-else statements when you need to choose between many options based on a single value.
            
            ### Loops:
            - for loop: Used when you know exactly how many times to iterate
            - while loop: Continues as long as a condition is true
            - do-while loop: Executes at least once, then continues while condition is true
            - for-each loop: Iterates through elements of arrays or collections
            
            ## Chapter 4: Object-Oriented Programming
            
            Object-Oriented Programming (OOP) is a programming paradigm based on the concept of objects, which contain data and code.
            
            ### Four Pillars of OOP:
            
            1. Encapsulation: Bundling data and methods that operate on that data within a single unit (class), and restricting direct access to some of the object's components.
            
            2. Inheritance: A mechanism where a new class inherits properties and behaviors from an existing class. The new class is called a subclass, and the existing class is called a superclass.
            
            3. Polymorphism: The ability of different classes to be treated as instances of the same class through a common interface. This includes method overloading and method overriding.
            
            4. Abstraction: Hiding complex implementation details and showing only the necessary features of an object.
            
            ## Chapter 5: Exception Handling
            
            Exception handling in Java uses try-catch-finally blocks to handle runtime errors gracefully.
            
            ### Types of Exceptions:
            - Checked Exceptions: Must be caught or declared (IOException, SQLException)
            - Unchecked Exceptions: Runtime exceptions (NullPointerException, ArrayIndexOutOfBoundsException)
            
            ### Best Practices:
            - Always catch specific exceptions rather than generic Exception
            - Use finally blocks for cleanup operations
            - Don't ignore exceptions silently
            - Use custom exceptions for domain-specific error handling
            """;
    }

    private String getSpringBootContent() {
        return """
            # Spring Boot Essentials
            
            ## Chapter 1: Introduction to Spring Boot
            
            Spring Boot is an open-source Java framework used to create microservices and production-ready applications with minimal configuration. It is built on top of the Spring Framework.
            
            ### Why Spring Boot?
            - Simplified Configuration: Auto-configuration reduces boilerplate code
            - Embedded Servers: No need for external application servers
            - Production-Ready Features: Health checks, metrics, and externalized configuration
            - Microservices Ready: Perfect for building microservice architectures
            
            ### Key Annotations:
            - @SpringBootApplication: Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan
            - @RestController: Marks a class as a REST controller
            - @RequestMapping: Maps HTTP requests to handler methods
            - @Autowired: Enables dependency injection
            
            ## Chapter 2: Dependency Injection
            
            Dependency Injection (DI) is a design pattern used to implement Inversion of Control (IoC). Spring Boot uses DI to manage object creation and their dependencies.
            
            ### Types of Injection:
            1. Constructor Injection: Dependencies passed through constructor (recommended)
            2. Setter Injection: Dependencies set through setter methods
            3. Field Injection: Dependencies injected directly into fields (not recommended)
            
            ### Bean Scopes:
            - Singleton (default): One instance per Spring container
            - Prototype: New instance for each request
            - Request: One instance per HTTP request
            - Session: One instance per HTTP session
            
            ## Chapter 3: Spring Data JPA
            
            Spring Data JPA simplifies database access by reducing boilerplate code for data access layers.
            
            ### Key Concepts:
            - Entity: A Java class mapped to a database table using @Entity
            - Repository: Interface extending JpaRepository for CRUD operations
            - Query Methods: Methods following naming conventions that Spring translates to SQL
            - JPQL: Java Persistence Query Language for custom queries
            
            ### Common Annotations:
            - @Entity: Marks a class as a JPA entity
            - @Id: Specifies the primary key
            - @GeneratedValue: Configures ID generation strategy
            - @Column: Customizes column mapping
            - @OneToMany, @ManyToOne: Define relationships
            
            ## Chapter 4: Spring Security
            
            Spring Security is a powerful authentication and authorization framework for Java applications.
            
            ### Core Concepts:
            - Authentication: Verifying user identity (who you are)
            - Authorization: Verifying permissions (what you can do)
            - Principal: Currently authenticated user
            - GrantedAuthority: Permissions granted to a principal
            
            ### Security Configuration:
            - WebSecurityConfigurerAdapter: Base class for custom security config (deprecated in Spring Security 6)
            - SecurityFilterChain: Modern approach to configure security
            - BCryptPasswordEncoder: Recommended password encoder
            
            ## Chapter 5: RESTful Web Services
            
            Spring Boot makes it easy to build RESTful web services using annotations.
            
            ### HTTP Methods:
            - GET: Retrieve resources
            - POST: Create new resources
            - PUT: Update existing resources
            - DELETE: Remove resources
            - PATCH: Partial updates
            
            ### Response Handling:
            - @ResponseBody: Serialize return value to response body
            - ResponseEntity: Full control over HTTP response
            - @ExceptionHandler: Handle exceptions globally or per controller
            - @ControllerAdvice: Global exception handling
            
            ## Chapter 6: Testing in Spring Boot
            
            Spring Boot provides excellent testing support with various annotations and utilities.
            
            ### Testing Annotations:
            - @SpringBootTest: Full application context for integration tests
            - @WebMvcTest: Test MVC controllers in isolation
            - @DataJpaTest: Test JPA repositories
            - @MockBean: Mock dependencies in tests
            
            ### Best Practices:
            - Write unit tests for business logic
            - Use integration tests for testing components together
            - Mock external dependencies
            - Test edge cases and error conditions
            """;
    }

    private String getAICourseContent() {
        return """
            # Introduction to Artificial Intelligence
            
            ## Chapter 1: What is Artificial Intelligence?
            
            Artificial Intelligence (AI) is the simulation of human intelligence processes by computer systems. These processes include learning, reasoning, problem-solving, perception, and language understanding.
            
            ### Types of AI:
            1. Narrow AI (Weak AI): Designed for specific tasks like voice assistants or recommendation systems
            2. General AI (Strong AI): Hypothetical AI with human-like general intelligence
            3. Super AI: Hypothetical AI surpassing human intelligence
            
            ### AI vs Machine Learning vs Deep Learning:
            - AI is the broader concept of machines performing tasks intelligently
            - Machine Learning is a subset of AI that learns from data
            - Deep Learning is a subset of ML using neural networks with many layers
            
            ## Chapter 2: Machine Learning Fundamentals
            
            Machine Learning is a method of data analysis that automates analytical model building. It uses algorithms that iteratively learn from data.
            
            ### Types of Machine Learning:
            
            1. Supervised Learning:
               - Training data includes input-output pairs
               - Examples: Classification, Regression
               - Algorithms: Linear Regression, Decision Trees, SVM, Neural Networks
            
            2. Unsupervised Learning:
               - Training data has no labeled responses
               - Examples: Clustering, Dimensionality Reduction
               - Algorithms: K-Means, Hierarchical Clustering, PCA
            
            3. Reinforcement Learning:
               - Agent learns through trial and error
               - Receives rewards or penalties for actions
               - Applications: Game AI, Robotics, Autonomous vehicles
            
            ## Chapter 3: Neural Networks and Deep Learning
            
            Neural networks are computing systems inspired by biological neural networks in animal brains.
            
            ### Components of Neural Networks:
            - Neurons (Nodes): Basic processing units
            - Weights: Connection strengths between neurons
            - Activation Functions: Introduce non-linearity (ReLU, Sigmoid, Tanh)
            - Layers: Input, Hidden, and Output layers
            
            ### Popular Architectures:
            - Convolutional Neural Networks (CNNs): Image processing
            - Recurrent Neural Networks (RNNs): Sequential data
            - Transformers: Natural language processing
            - GANs: Generative Adversarial Networks for content generation
            
            ## Chapter 4: Natural Language Processing
            
            NLP is a branch of AI that helps computers understand, interpret, and manipulate human language.
            
            ### Key NLP Tasks:
            - Tokenization: Breaking text into words or sentences
            - Part-of-Speech Tagging: Identifying grammatical parts
            - Named Entity Recognition: Identifying names, places, etc.
            - Sentiment Analysis: Determining emotional tone
            - Machine Translation: Translating between languages
            
            ### Modern NLP Technologies:
            - Word Embeddings: Word2Vec, GloVe
            - Transformers: BERT, GPT, T5
            - Large Language Models (LLMs): GPT-4, Claude, LLaMA
            
            ## Chapter 5: Applications of AI
            
            AI is transforming various industries and everyday life.
            
            ### Healthcare:
            - Disease diagnosis from medical images
            - Drug discovery and development
            - Personalized treatment plans
            
            ### Finance:
            - Fraud detection
            - Algorithmic trading
            - Credit scoring
            
            ### Transportation:
            - Autonomous vehicles
            - Traffic optimization
            - Route planning
            
            ### Education:
            - Personalized learning platforms
            - Automated grading
            - Intelligent tutoring systems
            
            ## Chapter 6: Ethics and Future of AI
            
            As AI becomes more powerful, ethical considerations become crucial.
            
            ### Key Ethical Concerns:
            - Bias and Fairness: AI systems can perpetuate existing biases
            - Privacy: AI requires large amounts of data
            - Transparency: Black-box nature of some AI systems
            - Job Displacement: Automation affecting employment
            - Autonomy: AI making decisions that affect humans
            
            ### Best Practices:
            - Develop AI responsibly with diverse teams
            - Ensure transparency and explainability
            - Implement robust testing and validation
            - Consider societal impact
            - Maintain human oversight
            """;
    }
}
