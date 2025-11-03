# Resource and Task Manager

## Overview

This is a command-line based resource and task management system built in Java. It provides functionalities for managing users and resources (referred to as "scrolls") through a secure, proxy-protected interface. The system includes administration capabilities, event logging, and a filesystem-based storage mechanism.

## Features

- **User Management**: Securely create, manage, and authenticate users.
- **Scroll Management**: Create and manage scrolls, which act as the primary resource within the system.
- **Administration**: Provides a separate menu and controls for administrative users.
- **Security**: Implements password hashing for user credentials and uses a proxy pattern to control access to core functionalities.
- **Event Logging**: Logs important events and actions within the system for auditing and tracking purposes.
- **Filesystem Abstraction**: Utilizes a custom filesystem abstraction layer for data persistence.

## Project Structure

```
.
├── gradle/
│   └── wrapper/
├── src/
│   ├── main/
│   │   └── java/
│   │       ├── AdminManager.java
│   │       ├── AdminManagerProxy.java
│   │       ├── AdminMenu.java
│   │       ├── App.java
│   │       ├── EventLogger.java
│   │       ├── EventLogManager.java
│   │       ├── SandboxEnvironment.java
│   │       ├── Scroll.java
│   │       ├── ScrollManager.java
│   │       ├── ScrollManagerProxy.java
│   │       ├── User.java
│   │       ├── UserInterface.java
│   │       ├── UserManager.java
│   │       ├── UserManagerProxy.java
│   │       ├── fsam/
│   │       │   └── FilesystemMemory.java
│   │       └── security/
│   │           └── PasswordHasher.java
│   └── test/
│       └── java/
│           ├── AdminManagerProxyTest.java
│           ├── AdminManagerTest.java
│           ├── ... (and other test files)
├── build.gradle
├── Jenkinsfile
└── README.md
```

## Setup and Usage

### Prerequisites

- Java Development Kit (JDK)
- Gradle (the project includes a Gradle wrapper, so a local installation is not required)

### Building and Testing

To build the project, run tests, and generate a code coverage report, execute the following command:
```bash
./gradlew clean test
```

### Running the Application

There are two ways to run the application:

1.  **With Password Obscuring (Recommended)**: This method builds the project and runs the application with support for hiding password input in the terminal.
    ```bash
    ./gradlew build
    java -cp build/classes/java/main App
    ```

2.  **Via Gradle (No Password Obscuring)**: This is a simpler way to run the application, but it will not obscure password input.
    ```bash
    ./gradlew run --console plain --quiet
