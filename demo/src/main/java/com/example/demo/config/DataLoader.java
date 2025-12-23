package com.example.demo.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.example.demo.entity.Module;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.ModuleRepository;
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
    private final ModuleRepository moduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RAGService ragService;

    public DataLoader(UserRepository userRepository, CourseRepository courseRepository,
                      EnrollmentRepository enrollmentRepository, ModuleRepository moduleRepository,
                      PasswordEncoder passwordEncoder, RAGService ragService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
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
        
        // Create Modules
        List<Module> modules = createModules(admin);
        
        // Create Sample Courses
        List<Course> courses = createCourses(admin, modules);
        
        // Create Enrollments
        createEnrollments(students, courses);

        log.info("===========================================");
        log.info("   DATA INITIALIZATION COMPLETE");
        log.info("===========================================");
        log.info("");
        log.info("   DEMO CREDENTIALS:");
        log.info("   -----------------");
        log.info("   Admin:   admin / admin123");
        log.info("   Students: student1-student10 / student123");
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
        List<User> students = new ArrayList<>();
        
        String[] firstNames = {"Alice", "Bob", "Carol", "David", "Emma", "Frank", "Grace", "Henry", "Ivy", "Jack"};
        String[] lastNames = {"Johnson", "Smith", "Davis", "Wilson", "Brown", "Taylor", "Anderson", "Thomas", "Moore", "Martin"};
        
        for (int i = 1; i <= 10; i++) {
            User student = new User();
            student.setUsername("student" + i);
            student.setPassword(passwordEncoder.encode("student123"));
            student.setEmail("student" + i + "@eduplatform.com");
            student.setFullName(firstNames[i-1] + " " + lastNames[i-1]);
            student.setRole(Role.STUDENT);
            student.setEnabled(true);
            student.setCreatedAt(LocalDateTime.now().minusDays(i));
            students.add(student);
        }

        List<User> savedStudents = userRepository.saveAll(students);
        log.info("Created {} sample students", savedStudents.size());
        
        return savedStudents;
    }

    private List<Module> createModules(User admin) {
        Module aiModule = new Module();
        aiModule.setName("Artificial Intelligence");
        aiModule.setDescription("Master the fundamentals of AI, from machine learning to deep learning, reinforcement learning, and natural language processing.");
        aiModule.setDisplayOrder(1);
        aiModule.setActive(true);
        aiModule.setCreatedBy(admin);
        aiModule.setCreatedAt(LocalDateTime.now());

        Module cpModule = new Module();
        cpModule.setName("Competitive Programming");
        cpModule.setDescription("Learn essential algorithms and data structures for competitive programming and technical interviews.");
        cpModule.setDisplayOrder(2);
        cpModule.setActive(true);
        cpModule.setCreatedBy(admin);
        cpModule.setCreatedAt(LocalDateTime.now());

        List<Module> modules = moduleRepository.saveAll(List.of(aiModule, cpModule));
        log.info("Created {} modules", modules.size());
        
        return modules;
    }

    private List<Course> createCourses(User admin, List<Module> modules) {
        Module aiModule = modules.get(0);
        Module cpModule = modules.get(1);
        
        List<Course> allCourses = new ArrayList<>();
        
        // === AI MODULE COURSES ===
        
        // Machine Learning Course
        Course mlCourse = new Course();
        mlCourse.setTitle("Machine Learning Fundamentals");
        mlCourse.setDescription("Learn the core concepts of machine learning including supervised learning, unsupervised learning, and model evaluation.");
        mlCourse.setContent(getMLContent());
        mlCourse.setStatus(CourseStatus.PUBLISHED);
        mlCourse.setIndexed(true);
        mlCourse.setCreatedAt(LocalDateTime.now());
        mlCourse.setPublishedAt(LocalDateTime.now());
        mlCourse.setCreatedBy(admin);
        mlCourse.setModule(aiModule);
        mlCourse.setDisplayOrder(1);
        allCourses.add(mlCourse);

        // Deep Learning Course
        Course dlCourse = new Course();
        dlCourse.setTitle("Deep Learning & Neural Networks");
        dlCourse.setDescription("Dive deep into neural networks, CNNs, RNNs, and modern deep learning architectures.");
        dlCourse.setContent(getDLContent());
        dlCourse.setStatus(CourseStatus.PUBLISHED);
        dlCourse.setIndexed(true);
        dlCourse.setCreatedAt(LocalDateTime.now());
        dlCourse.setPublishedAt(LocalDateTime.now());
        dlCourse.setCreatedBy(admin);
        dlCourse.setModule(aiModule);
        dlCourse.setDisplayOrder(2);
        allCourses.add(dlCourse);

        // Reinforcement Learning Course
        Course rlCourse = new Course();
        rlCourse.setTitle("Reinforcement Learning");
        rlCourse.setDescription("Master reinforcement learning concepts including Q-learning, policy gradients, and deep RL.");
        rlCourse.setContent(getRLContent());
        rlCourse.setStatus(CourseStatus.PUBLISHED);
        rlCourse.setIndexed(true);
        rlCourse.setCreatedAt(LocalDateTime.now());
        rlCourse.setPublishedAt(LocalDateTime.now());
        rlCourse.setCreatedBy(admin);
        rlCourse.setModule(aiModule);
        rlCourse.setDisplayOrder(3);
        allCourses.add(rlCourse);

        // NLP Course
        Course nlpCourse = new Course();
        nlpCourse.setTitle("Natural Language Processing");
        nlpCourse.setDescription("Explore NLP techniques from text processing to transformers and large language models.");
        nlpCourse.setContent(getNLPContent());
        nlpCourse.setStatus(CourseStatus.PUBLISHED);
        nlpCourse.setIndexed(true);
        nlpCourse.setCreatedAt(LocalDateTime.now());
        nlpCourse.setPublishedAt(LocalDateTime.now());
        nlpCourse.setCreatedBy(admin);
        nlpCourse.setModule(aiModule);
        nlpCourse.setDisplayOrder(4);
        allCourses.add(nlpCourse);

        // === COMPETITIVE PROGRAMMING MODULE COURSES ===

        // Sorting Algorithms Course
        Course sortingCourse = new Course();
        sortingCourse.setTitle("Sorting Algorithms");
        sortingCourse.setDescription("Master essential sorting algorithms from bubble sort to quicksort and their time complexities.");
        sortingCourse.setContent(getSortingContent());
        sortingCourse.setStatus(CourseStatus.PUBLISHED);
        sortingCourse.setIndexed(true);
        sortingCourse.setCreatedAt(LocalDateTime.now());
        sortingCourse.setPublishedAt(LocalDateTime.now());
        sortingCourse.setCreatedBy(admin);
        sortingCourse.setModule(cpModule);
        sortingCourse.setDisplayOrder(1);
        allCourses.add(sortingCourse);

        // Sliding Window Course
        Course slidingWindowCourse = new Course();
        slidingWindowCourse.setTitle("Sliding Window Technique");
        slidingWindowCourse.setDescription("Learn the sliding window pattern for solving array and string problems efficiently.");
        slidingWindowCourse.setContent(getSlidingWindowContent());
        slidingWindowCourse.setStatus(CourseStatus.PUBLISHED);
        slidingWindowCourse.setIndexed(true);
        slidingWindowCourse.setCreatedAt(LocalDateTime.now());
        slidingWindowCourse.setPublishedAt(LocalDateTime.now());
        slidingWindowCourse.setCreatedBy(admin);
        slidingWindowCourse.setModule(cpModule);
        slidingWindowCourse.setDisplayOrder(2);
        allCourses.add(slidingWindowCourse);

        // Dynamic Programming Course
        Course dpCourse = new Course();
        dpCourse.setTitle("Dynamic Programming");
        dpCourse.setDescription("Master dynamic programming from basic concepts to advanced optimization techniques.");
        dpCourse.setContent(getDPContent());
        dpCourse.setStatus(CourseStatus.PUBLISHED);
        dpCourse.setIndexed(true);
        dpCourse.setCreatedAt(LocalDateTime.now());
        dpCourse.setPublishedAt(LocalDateTime.now());
        dpCourse.setCreatedBy(admin);
        dpCourse.setModule(cpModule);
        dpCourse.setDisplayOrder(3);
        allCourses.add(dpCourse);

        // Graph Algorithms Course
        Course graphCourse = new Course();
        graphCourse.setTitle("Graph Algorithms");
        graphCourse.setDescription("Learn graph representations, traversals, shortest paths, and advanced graph algorithms.");
        graphCourse.setContent(getGraphContent());
        graphCourse.setStatus(CourseStatus.PUBLISHED);
        graphCourse.setIndexed(true);
        graphCourse.setCreatedAt(LocalDateTime.now());
        graphCourse.setPublishedAt(LocalDateTime.now());
        graphCourse.setCreatedBy(admin);
        graphCourse.setModule(cpModule);
        graphCourse.setDisplayOrder(4);
        allCourses.add(graphCourse);

        List<Course> courses = courseRepository.saveAll(allCourses);
        log.info("Created {} sample courses", courses.size());
        
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
        List<Course> publishedCourses = courses.stream()
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .toList();

        // Enroll first 5 students in AI courses
        List<Course> aiCourses = publishedCourses.stream()
                .filter(c -> c.getModule() != null && c.getModule().getName().contains("Artificial Intelligence"))
                .toList();
        
        for (int i = 0; i < 5 && i < students.size(); i++) {
            for (Course course : aiCourses) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(students.get(i));
                enrollment.setCourse(course);
                enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
                enrollment.setProgressPercentage(i * 20);
                enrollment.setEnrolledAt(LocalDateTime.now().minusDays(10 - i));
                enrollmentRepository.save(enrollment);
            }
        }

        // Enroll students 6-10 in CP courses
        List<Course> cpCourses = publishedCourses.stream()
                .filter(c -> c.getModule() != null && c.getModule().getName().contains("Competitive"))
                .toList();
        
        for (int i = 5; i < 10 && i < students.size(); i++) {
            for (Course course : cpCourses) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(students.get(i));
                enrollment.setCourse(course);
                enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
                enrollment.setProgressPercentage((i - 5) * 20);
                enrollment.setEnrolledAt(LocalDateTime.now().minusDays(15 - i));
                enrollmentRepository.save(enrollment);
            }
        }

        // Enroll first 3 students in some CP courses too (for variety)
        for (int i = 0; i < 3 && i < students.size() && cpCourses.size() > 0; i++) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(students.get(i));
            enrollment.setCourse(cpCourses.get(i % cpCourses.size()));
            enrollment.setStatus(EnrollmentStatus.IN_PROGRESS);
            enrollment.setProgressPercentage(50);
            enrollment.setEnrolledAt(LocalDateTime.now().minusDays(5));
            enrollmentRepository.save(enrollment);
        }

        log.info("Created sample enrollments");
    }

    // === AI COURSE CONTENTS ===

    private String getMLContent() {
        return """
            # Machine Learning Fundamentals
            
            ## Chapter 1: Introduction to Machine Learning
            
            Machine Learning (ML) is a subset of Artificial Intelligence that enables systems to learn and improve from experience without being explicitly programmed. It focuses on developing algorithms that can access data and use it to learn for themselves.
            
            ### What is Machine Learning?
            Machine learning is the science of getting computers to act without being explicitly programmed. Instead of writing code to solve problems directly, we train models on data to find patterns and make predictions.
            
            ### Types of Machine Learning:
            1. **Supervised Learning**: Learning from labeled data
            2. **Unsupervised Learning**: Finding patterns in unlabeled data
            3. **Reinforcement Learning**: Learning through trial and error
            4. **Semi-supervised Learning**: Using both labeled and unlabeled data
            
            ## Chapter 2: Supervised Learning
            
            Supervised learning is the most common type of machine learning. The algorithm learns from labeled training data to make predictions.
            
            ### Classification
            Classification predicts categorical labels. Examples include:
            - Email spam detection (spam/not spam)
            - Image classification (cat/dog/bird)
            - Disease diagnosis (positive/negative)
            
            **Common Classification Algorithms:**
            - Logistic Regression
            - Decision Trees
            - Random Forests
            - Support Vector Machines (SVM)
            - K-Nearest Neighbors (KNN)
            - Naive Bayes
            
            ### Regression
            Regression predicts continuous numerical values. Examples include:
            - House price prediction
            - Stock price forecasting
            - Temperature prediction
            
            **Common Regression Algorithms:**
            - Linear Regression
            - Polynomial Regression
            - Ridge and Lasso Regression
            - Gradient Boosting Regressors
            
            ## Chapter 3: Unsupervised Learning
            
            Unsupervised learning finds hidden patterns in data without labels.
            
            ### Clustering
            Grouping similar data points together:
            - **K-Means**: Partitions data into K clusters
            - **Hierarchical Clustering**: Creates a tree of clusters
            - **DBSCAN**: Density-based clustering
            
            ### Dimensionality Reduction
            Reducing the number of features while preserving information:
            - **PCA (Principal Component Analysis)**: Linear dimensionality reduction
            - **t-SNE**: Visualization of high-dimensional data
            - **UMAP**: Modern alternative to t-SNE
            
            ## Chapter 4: Model Evaluation
            
            ### Classification Metrics
            - **Accuracy**: Percentage of correct predictions
            - **Precision**: True positives / (True positives + False positives)
            - **Recall**: True positives / (True positives + False negatives)
            - **F1 Score**: Harmonic mean of precision and recall
            - **ROC-AUC**: Area under the ROC curve
            
            ### Regression Metrics
            - **MSE (Mean Squared Error)**: Average squared difference
            - **RMSE (Root Mean Squared Error)**: Square root of MSE
            - **MAE (Mean Absolute Error)**: Average absolute difference
            - **R² Score**: Proportion of variance explained
            
            ### Cross-Validation
            Cross-validation helps assess model generalization:
            - K-Fold Cross-Validation
            - Stratified K-Fold
            - Leave-One-Out Cross-Validation
            
            ## Chapter 5: Feature Engineering
            
            Feature engineering is the process of using domain knowledge to create features that make ML algorithms work better.
            
            ### Techniques:
            - **Feature Scaling**: Normalization and Standardization
            - **Encoding Categorical Variables**: One-hot encoding, Label encoding
            - **Handling Missing Values**: Imputation strategies
            - **Feature Selection**: Selecting the most relevant features
            - **Feature Creation**: Creating new features from existing ones
            """;
    }

    private String getDLContent() {
        return """
            # Deep Learning & Neural Networks
            
            ## Chapter 1: Introduction to Neural Networks
            
            Neural networks are computing systems inspired by biological neural networks in the human brain. They consist of interconnected nodes (neurons) that process information.
            
            ### The Perceptron
            The perceptron is the simplest neural network:
            - Takes multiple inputs
            - Applies weights to each input
            - Sums the weighted inputs
            - Applies an activation function
            - Produces an output
            
            ### Multi-Layer Perceptron (MLP)
            MLPs have multiple layers:
            - **Input Layer**: Receives the input features
            - **Hidden Layers**: Process the information
            - **Output Layer**: Produces the final prediction
            
            ## Chapter 2: Activation Functions
            
            Activation functions introduce non-linearity into neural networks.
            
            ### Common Activation Functions:
            - **Sigmoid**: outputs between 0 and 1
            - **Tanh**: outputs between -1 and 1
            - **ReLU**: max(0, x), most popular for hidden layers
            - **Leaky ReLU**: fixes dying ReLU problem
            - **Softmax**: Used for multi-class classification output
            
            ## Chapter 3: Training Neural Networks
            
            ### Forward Propagation
            Data flows through the network from input to output, computing predictions.
            
            ### Loss Functions
            - **MSE**: For regression tasks
            - **Cross-Entropy**: For classification tasks
            - **Binary Cross-Entropy**: For binary classification
            
            ### Backpropagation
            Backpropagation computes gradients of the loss with respect to weights using the chain rule, enabling the network to learn.
            
            ### Gradient Descent Optimizers
            - **SGD**: Stochastic Gradient Descent
            - **Momentum**: Accelerates SGD
            - **Adam**: Adaptive learning rates (most popular)
            - **RMSprop**: Root Mean Square Propagation
            
            ## Chapter 4: Convolutional Neural Networks (CNNs)
            
            CNNs are specialized for processing grid-like data such as images.
            
            ### Key Components:
            - **Convolutional Layers**: Extract features using filters
            - **Pooling Layers**: Reduce spatial dimensions
            - **Fully Connected Layers**: Final classification
            
            ### Famous CNN Architectures:
            - LeNet-5: Pioneer architecture
            - AlexNet: Won ImageNet 2012
            - VGGNet: Deep networks with small filters
            - ResNet: Skip connections for very deep networks
            - EfficientNet: Balanced scaling
            
            ## Chapter 5: Recurrent Neural Networks (RNNs)
            
            RNNs are designed for sequential data like text and time series.
            
            ### Vanilla RNN
            Has a hidden state that is updated at each time step, allowing memory of previous inputs.
            
            ### LSTM (Long Short-Term Memory)
            Solves the vanishing gradient problem with:
            - Forget gate
            - Input gate
            - Output gate
            - Cell state
            
            ### GRU (Gated Recurrent Unit)
            Simplified version of LSTM with:
            - Reset gate
            - Update gate
            
            ## Chapter 6: Modern Architectures
            
            ### Transformers
            The transformer architecture revolutionized NLP and beyond:
            - Self-attention mechanism
            - Parallel processing
            - Positional encoding
            
            ### GANs (Generative Adversarial Networks)
            Two networks competing:
            - Generator: Creates fake data
            - Discriminator: Distinguishes real from fake
            
            ### Autoencoders
            Learn compressed representations:
            - Encoder: Compresses input
            - Decoder: Reconstructs from compressed form
            - Applications: Denoising, anomaly detection
            """;
    }

    private String getRLContent() {
        return """
            # Reinforcement Learning
            
            ## Chapter 1: Introduction to Reinforcement Learning
            
            Reinforcement Learning (RL) is a type of machine learning where an agent learns to make decisions by interacting with an environment. The agent learns to achieve a goal by receiving rewards or penalties.
            
            ### Key Components:
            - **Agent**: The learner and decision maker
            - **Environment**: The world the agent interacts with
            - **State**: Current situation of the agent
            - **Action**: What the agent can do
            - **Reward**: Feedback from the environment
            - **Policy**: Strategy the agent uses
            
            ### The RL Loop:
            1. Agent observes the current state
            2. Agent selects an action based on its policy
            3. Environment transitions to a new state
            4. Agent receives a reward
            5. Agent updates its policy
            6. Repeat
            
            ## Chapter 2: Markov Decision Processes (MDPs)
            
            MDPs provide the mathematical framework for RL.
            
            ### Components of an MDP:
            - **S**: Set of states
            - **A**: Set of actions
            - **P(s'|s,a)**: Transition probabilities
            - **R(s,a,s')**: Reward function
            - **gamma**: Discount factor (0 to 1)
            
            ### Value Functions:
            - **V(s)**: State-value function - expected return from state s
            - **Q(s,a)**: Action-value function - expected return from state s taking action a
            
            ### Bellman Equations:
            Fundamental equations that express the relationship between value of a state and values of successor states.
            
            ## Chapter 3: Dynamic Programming
            
            DP methods require a complete model of the environment.
            
            ### Policy Evaluation
            Computing the value function for a given policy.
            
            ### Policy Improvement
            Finding a better policy given a value function.
            
            ### Policy Iteration
            Alternating between evaluation and improvement until convergence.
            
            ### Value Iteration
            Combining evaluation and improvement in a single update.
            
            ## Chapter 4: Model-Free Methods
            
            Learn without knowing the environment dynamics.
            
            ### Monte Carlo Methods
            - Learn from complete episodes
            - Average returns for visited states
            - First-visit vs Every-visit MC
            
            ### Temporal Difference (TD) Learning
            - Learn from incomplete episodes
            - Bootstrap from current estimates
            - TD(0): One-step TD
            
            ### Q-Learning
            Off-policy TD control:
            - Learn action-value function Q(s,a)
            - Guaranteed to converge to optimal Q*
            
            ### SARSA
            On-policy TD control:
            - Learns the value of the policy being followed
            
            ## Chapter 5: Deep Reinforcement Learning
            
            Combining deep learning with RL for complex problems.
            
            ### Deep Q-Networks (DQN)
            - Neural network approximates Q-function
            - Experience replay for stability
            - Target network for stable targets
            - Achieved human-level performance on Atari games
            
            ### Policy Gradient Methods
            - Directly optimize the policy
            - REINFORCE algorithm
            - Can handle continuous action spaces
            
            ### Actor-Critic Methods
            - Actor: Learns the policy
            - Critic: Learns the value function
            - Combines benefits of both approaches
            
            ### Advanced Algorithms:
            - **A3C/A2C**: Asynchronous/Advantage Actor-Critic
            - **PPO**: Proximal Policy Optimization (very popular)
            - **SAC**: Soft Actor-Critic (maximum entropy RL)
            - **DDPG**: Deep Deterministic Policy Gradient
            
            ## Chapter 6: Applications
            
            ### Games:
            - AlphaGo: Defeated world champion in Go
            - OpenAI Five: Dota 2
            - AlphaStar: StarCraft II
            
            ### Robotics:
            - Robot manipulation
            - Locomotion
            - Navigation
            
            ### Real-World Applications:
            - Autonomous driving
            - Resource management
            - Recommendation systems
            - Trading strategies
            """;
    }

    private String getNLPContent() {
        return """
            # Natural Language Processing
            
            ## Chapter 1: Introduction to NLP
            
            Natural Language Processing (NLP) is a field of AI that focuses on the interaction between computers and human language. It enables machines to read, understand, and derive meaning from text.
            
            ### Why NLP is Challenging:
            - Ambiguity in language
            - Context-dependent meaning
            - Sarcasm and idioms
            - Multiple languages and dialects
            - Constantly evolving language
            
            ### NLP Applications:
            - Machine Translation
            - Sentiment Analysis
            - Chatbots and Virtual Assistants
            - Text Summarization
            - Named Entity Recognition
            - Question Answering
            
            ## Chapter 2: Text Preprocessing
            
            Preparing text data for NLP models.
            
            ### Tokenization
            Breaking text into tokens (words, subwords, or characters):
            - Word tokenization
            - Sentence tokenization
            - Subword tokenization (BPE, WordPiece)
            
            ### Text Cleaning:
            - Lowercasing
            - Removing punctuation
            - Removing stop words
            - Handling contractions
            - Removing special characters
            
            ### Normalization:
            - Stemming: Reducing words to their root (running to run)
            - Lemmatization: Reducing to dictionary form (better to good)
            
            ## Chapter 3: Text Representation
            
            ### Bag of Words (BoW)
            - Represents text as word frequency vectors
            - Ignores word order
            - Simple but effective baseline
            
            ### TF-IDF
            Term Frequency-Inverse Document Frequency:
            - Weighs words by importance
            - Reduces impact of common words
            
            ### Word Embeddings
            Dense vector representations of words:
            - **Word2Vec**: Skip-gram and CBOW models
            - **GloVe**: Global Vectors for Word Representation
            - **FastText**: Handles out-of-vocabulary words
            
            Properties:
            - Capture semantic relationships
            - king - man + woman = queen
            - Typically 100-300 dimensions
            
            ## Chapter 4: Sequence Models
            
            ### RNNs for NLP
            Process text sequentially:
            - Good for variable-length sequences
            - Capture context from previous words
            - Suffer from vanishing gradients
            
            ### LSTMs and GRUs
            - Better at capturing long-range dependencies
            - Commonly used for:
              - Language modeling
              - Machine translation
              - Text classification
            
            ### Bidirectional Models
            Process text in both directions:
            - BiLSTM captures context from both sides
            - Better understanding of word meaning
            
            ## Chapter 5: Attention and Transformers
            
            ### Attention Mechanism
            Allows models to focus on relevant parts of input:
            - Query, Key, Value mechanism
            - Attention weights indicate importance
            - Solves the bottleneck problem in seq2seq
            
            ### The Transformer Architecture
            Revolutionary architecture that powers modern NLP:
            - Self-attention: Relate different positions in sequence
            - Multi-head attention: Multiple attention patterns
            - Positional encoding: Inject position information
            - Feed-forward networks
            - Layer normalization
            
            ### Key Advantages:
            - Parallelizable (unlike RNNs)
            - Captures long-range dependencies
            - Scales well with data and compute
            
            ## Chapter 6: Large Language Models
            
            ### BERT (Bidirectional Encoder Representations from Transformers)
            - Pre-trained on masked language modeling
            - Bidirectional context
            - Fine-tune for downstream tasks
            - Variants: RoBERTa, ALBERT, DistilBERT
            
            ### GPT (Generative Pre-trained Transformer)
            - Autoregressive language model
            - Trained to predict next token
            - GPT-2, GPT-3, GPT-4
            - Emergent capabilities at scale
            
            ### T5 (Text-to-Text Transfer Transformer)
            - Frames all NLP tasks as text-to-text
            - Unified approach to NLP
            
            ### Modern LLMs:
            - ChatGPT: Conversational AI
            - Claude: Anthropic's assistant
            - LLaMA: Meta's open model
            - PaLM/Gemini: Google's models
            
            ### Prompt Engineering:
            - Zero-shot prompting
            - Few-shot learning
            - Chain-of-thought reasoning
            - Instruction tuning
            """;
    }

    // === COMPETITIVE PROGRAMMING COURSE CONTENTS ===

    private String getSortingContent() {
        return """
            # Sorting Algorithms
            
            ## Chapter 1: Introduction to Sorting
            
            Sorting is the process of arranging elements in a specific order (ascending or descending). It's one of the most fundamental operations in computer science.
            
            ### Why Sorting Matters:
            - Enables efficient searching (binary search)
            - Data organization and presentation
            - Foundation for many algorithms
            - Common in interviews and competitions
            
            ### Comparison-Based vs Non-Comparison Sorting:
            - Comparison-based: Elements compared pairwise (O(n log n) lower bound)
            - Non-comparison: Use element properties (can achieve O(n))
            
            ## Chapter 2: Basic Sorting Algorithms
            
            ### Bubble Sort
            Repeatedly swaps adjacent elements if in wrong order.
            - Time: O(n squared) average and worst
            - Space: O(1)
            - Stable: Yes
            - Best for: Educational purposes, nearly sorted data
            
            ### Selection Sort
            Finds minimum element and places it at beginning.
            - Time: O(n squared) always
            - Space: O(1)
            - Stable: No (but can be made stable)
            - Best for: Small arrays, minimizing swaps
            
            ### Insertion Sort
            Builds sorted array one element at a time.
            - Time: O(n squared) average, O(n) best case
            - Space: O(1)
            - Stable: Yes
            - Best for: Small arrays, nearly sorted data, online sorting
            
            ## Chapter 3: Efficient Sorting Algorithms
            
            ### Merge Sort
            Divide and conquer algorithm that divides, sorts, and merges.
            - Time: O(n log n) always
            - Space: O(n)
            - Stable: Yes
            - Best for: Linked lists, external sorting, stable sort needed
            
            ### Quick Sort
            Partition-based divide and conquer algorithm.
            - Time: O(n log n) average, O(n squared) worst
            - Space: O(log n) for recursion
            - Stable: No
            - Best for: General purpose, in-memory sorting
            
            ### Pivot Selection Strategies:
            - First/Last element (simple but vulnerable)
            - Random element (expected O(n log n))
            - Median of three (good practical choice)
            
            ## Chapter 4: Heap Sort
            
            Uses a binary heap data structure.
            - Time: O(n log n) always
            - Space: O(1)
            - Stable: No
            - Best for: Guaranteed O(n log n), memory-constrained
            
            ### Building a Max Heap:
            - Heapify: O(log n) operation
            - Build heap: O(n) operation
            
            ## Chapter 5: Linear Time Sorting
            
            ### Counting Sort
            Counts occurrences of each element.
            - Time: O(n + k) where k is range
            - Space: O(k)
            - Stable: Yes
            - Best for: Small range of integers
            
            ### Radix Sort
            Sorts by individual digits/characters.
            - Time: O(d * (n + k)) where d is digits
            - Space: O(n + k)
            - Stable: Yes
            - Best for: Fixed-length integers, strings
            
            ### Bucket Sort
            Distributes elements into buckets, sorts each.
            - Time: O(n) average, O(n squared) worst
            - Space: O(n)
            - Stable: Depends on bucket sort used
            - Best for: Uniformly distributed data
            
            ## Chapter 6: Practical Considerations
            
            ### Choosing the Right Algorithm:
            - Small n (less than 50): Insertion sort
            - General purpose: Quick sort / Intro sort
            - Stability needed: Merge sort
            - Memory constrained: Heap sort
            - Known distribution: Bucket/Radix sort
            
            ### Hybrid Approaches:
            - **Timsort**: Merge sort + Insertion sort (Python, Java)
            - **Introsort**: Quick sort + Heap sort + Insertion sort (C++ STL)
            
            ### Competition Tips:
            - Use built-in sort when possible
            - Know when custom comparators are needed
            - Consider stability requirements
            - Watch for integer overflow in comparisons
            """;
    }

    private String getSlidingWindowContent() {
        return """
            # Sliding Window Technique
            
            ## Chapter 1: Introduction to Sliding Window
            
            The sliding window technique is a method for solving problems involving arrays or strings where you need to find or calculate something among all contiguous subarrays (or substrings) of a given size.
            
            ### When to Use Sliding Window:
            - Problems involving contiguous sequences
            - Finding max/min/sum in subarrays
            - Longest/shortest substring problems
            - Window of k elements
            
            ### Types of Sliding Windows:
            1. **Fixed Size Window**: Window size is constant
            2. **Variable Size Window**: Window expands/contracts based on conditions
            
            ## Chapter 2: Fixed Size Sliding Window
            
            The window size remains constant as it slides through the array.
            
            ### Example Problems:
            - Maximum sum of k consecutive elements
            - Average of all subarrays of size k
            - First negative number in every window of size k
            
            ### Time Complexity: O(n)
            Instead of O(n*k) with brute force
            
            ## Chapter 3: Variable Size Sliding Window
            
            The window expands or contracts based on problem conditions.
            
            ### General Pattern:
            - Initialize left pointer at 0
            - Iterate right pointer from 0 to n-1
            - Expand window by including element at right
            - While window is invalid, contract by moving left
            - Update answer if window is valid
            
            ## Chapter 4: Common Patterns
            
            ### Pattern 1: Maximum/Minimum Sum Subarray
            Find subarray with sum constraints.
            - Minimum size subarray with sum >= S
            - Maximum sum subarray of size at most K
            
            ### Pattern 2: Substring with Conditions
            Find substrings matching certain criteria.
            - Longest substring with K distinct characters
            - Smallest window containing all characters
            - Anagram substrings
            
            ### Pattern 3: K Elements Problems
            Problems involving exactly K of something.
            - Subarrays with exactly K different integers
            - Substrings with exactly K ones
            
            Trick: atMost(K) - atMost(K-1) = exactly(K)
            
            ## Chapter 5: Advanced Techniques
            
            ### Using Hash Maps
            Track frequency of elements in window for efficient lookups.
            
            ### Two Pointers Variant
            Sometimes called two pointers when not literally a window:
            - Container with most water
            - Three sum problems
            
            ### Deque for Monotonic Window
            For finding max/min in sliding window:
            - Maintain monotonic decreasing deque for max
            - Maintain monotonic increasing deque for min
            - O(n) time complexity
            
            ## Chapter 6: Classic Problems
            
            ### Problem 1: Maximum Sum Subarray of Size K
            Fixed window, track running sum.
            
            ### Problem 2: Longest Substring Without Repeating Characters
            Variable window, use set to track characters.
            
            ### Problem 3: Minimum Window Substring
            Find smallest window containing all characters of pattern.
            - Use two hashmaps to compare frequencies
            - Expand right to include, contract left to minimize
            
            ### Problem 4: Sliding Window Maximum
            Find maximum in each window of size k.
            - Use monotonic deque
            - Store indices, not values
            
            ### Problem 5: Subarray Product Less Than K
            Count subarrays with product less than k.
            - Variable window
            - Each valid window contributes (right - left + 1) subarrays
            
            ### Competition Tips:
            - Draw the window on paper
            - Identify expand and contract conditions
            - Handle edge cases (empty window, single element)
            - Consider what data structure to track window state
            """;
    }

    private String getDPContent() {
        return """
            # Dynamic Programming
            
            ## Chapter 1: Introduction to Dynamic Programming
            
            Dynamic Programming (DP) is an algorithmic paradigm that solves complex problems by breaking them down into simpler subproblems. It stores the results of subproblems to avoid redundant computations.
            
            ### When to Use DP:
            - **Optimal Substructure**: Optimal solution can be constructed from optimal solutions of subproblems
            - **Overlapping Subproblems**: Same subproblems are solved multiple times
            
            ### DP vs Divide and Conquer:
            - Divide and Conquer: Subproblems are independent (Merge Sort)
            - DP: Subproblems overlap (Fibonacci)
            
            ## Chapter 2: Approaches to DP
            
            ### Top-Down (Memoization)
            Start from the main problem, recursively solve subproblems, cache results.
            - Natural recursive thinking
            - May not solve all subproblems
            - Uses function call stack
            
            ### Bottom-Up (Tabulation)
            Start from base cases, build up to the solution.
            - Iterative approach
            - Solves all subproblems
            - Often more space-efficient
            
            ## Chapter 3: Classic DP Problems
            
            ### Fibonacci Numbers
            Base case: F(0) = 0, F(1) = 1
            Recurrence: F(n) = F(n-1) + F(n-2)
            
            ### Climbing Stairs
            Ways to climb n stairs taking 1 or 2 steps at a time.
            dp[i] = dp[i-1] + dp[i-2]
            
            ### Coin Change
            Minimum coins to make amount.
            dp[i] = min(dp[i], dp[i-coin] + 1) for each coin
            
            ### Longest Increasing Subsequence (LIS)
            Length of longest strictly increasing subsequence.
            O(n squared) basic, O(n log n) with binary search
            
            ## Chapter 4: 2D Dynamic Programming
            
            ### Grid Problems
            
            **Unique Paths**
            Count paths from top-left to bottom-right.
            dp[i][j] = dp[i-1][j] + dp[i][j-1]
            
            **Minimum Path Sum**
            Find path with minimum sum.
            dp[i][j] = grid[i][j] + min(dp[i-1][j], dp[i][j-1])
            
            ### String Problems
            
            **Longest Common Subsequence (LCS)**
            Compare two strings character by character.
            
            **Edit Distance**
            Minimum operations to convert s1 to s2.
            Operations: insert, delete, replace
            
            **Longest Palindromic Subsequence**
            LCS of string with its reverse.
            
            ## Chapter 5: DP Patterns
            
            ### Pattern 1: Linear DP
            - State depends on previous states
            - Examples: Fibonacci, House Robber, Maximum Subarray
            
            ### Pattern 2: Interval DP
            - Problems on ranges/intervals
            - Examples: Matrix Chain Multiplication, Burst Balloons
            - Usually O(n cubed)
            
            ### Pattern 3: Knapsack
            - Select items with constraints
            - 0/1 Knapsack: Each item used once
            - Unbounded Knapsack: Items can be reused
            - Subset Sum: Special case
            
            ### Pattern 4: DP on Trees
            - DFS-based DP
            - State at node depends on children
            - Examples: Tree Diameter, Maximum Path Sum
            
            ### Pattern 5: Bitmask DP
            - State represented as bitmask
            - For problems with small n (usually 20 or less)
            - Examples: Traveling Salesman, Assignment Problem
            
            ## Chapter 6: Optimization Techniques
            
            ### Space Optimization
            Often can reduce O(n) space to O(1) or O(n squared) to O(n).
            
            ### State Reduction
            Sometimes states can be combined or eliminated.
            
            ### Competition Tips:
            - Identify the state clearly
            - Write the recurrence relation first
            - Handle base cases carefully
            - Consider space optimization
            - Watch for off-by-one errors
            - Practice recognizing DP problems
            """;
    }

    private String getGraphContent() {
        return """
            # Graph Algorithms
            
            ## Chapter 1: Graph Fundamentals
            
            A graph G = (V, E) consists of vertices (nodes) V and edges E connecting them.
            
            ### Types of Graphs:
            - **Directed vs Undirected**: Edges have direction or not
            - **Weighted vs Unweighted**: Edges have costs or not
            - **Cyclic vs Acyclic**: Contains cycles or not
            - **Connected vs Disconnected**: All vertices reachable or not
            
            ### Graph Representations:
            
            **Adjacency Matrix**
            - 2D array where matrix[i][j] = 1 if edge exists
            - Space: O(V squared)
            - Edge lookup: O(1)
            - Best for: Dense graphs
            
            **Adjacency List**
            - Array of lists, each list contains neighbors
            - Space: O(V + E)
            - Edge lookup: O(degree)
            - Best for: Sparse graphs (most common)
            
            ## Chapter 2: Graph Traversals
            
            ### Breadth-First Search (BFS)
            Explores level by level using a queue.
            
            Applications:
            - Shortest path in unweighted graph
            - Level order traversal
            - Finding connected components
            - Bipartite check
            
            ### Depth-First Search (DFS)
            Explores as deep as possible using recursion/stack.
            
            Applications:
            - Cycle detection
            - Topological sorting
            - Finding connected components
            - Path finding
            
            ## Chapter 3: Shortest Path Algorithms
            
            ### Dijkstra's Algorithm
            Single-source shortest path for non-negative weights.
            Time: O((V + E) log V) with min-heap
            
            ### Bellman-Ford Algorithm
            Handles negative weights, detects negative cycles.
            Time: O(VE)
            
            ### Floyd-Warshall Algorithm
            All-pairs shortest paths.
            Time: O(V cubed)
            
            ## Chapter 4: Minimum Spanning Trees
            
            ### Kruskal's Algorithm
            Greedy approach using Union-Find.
            - Sort edges by weight
            - Add edges that don't create cycles
            Time: O(E log E)
            
            ### Prim's Algorithm
            Grows MST from starting vertex.
            Time: O((V + E) log V)
            
            ## Chapter 5: Topological Sort
            
            Linear ordering of vertices in a DAG such that for every edge (u, v), u comes before v.
            
            ### Kahn's Algorithm (BFS-based)
            - Track in-degrees
            - Process vertices with in-degree 0
            - Remove edges and update in-degrees
            
            ### DFS-based Approach
            Post-order DFS gives reverse topological order.
            
            ## Chapter 6: Advanced Topics
            
            ### Strongly Connected Components (Kosaraju's Algorithm)
            1. DFS and record finish times
            2. Transpose the graph
            3. DFS in order of decreasing finish time
            
            ### Articulation Points and Bridges
            Vertices/edges whose removal disconnects the graph.
            Uses DFS with discovery and low times.
            
            ### Maximum Flow (Ford-Fulkerson)
            Find maximum flow from source to sink.
            
            ### Bipartite Matching
            Maximum matching in bipartite graphs.
            
            ### Competition Tips:
            - Know when to use BFS vs DFS
            - Dijkstra for non-negative, Bellman-Ford for negative weights
            - Union-Find for connectivity queries
            - Watch for 0-indexed vs 1-indexed vertices
            - Consider edge cases: disconnected graphs, self-loops
            """;
    }
}
