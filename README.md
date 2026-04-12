# ⚖️ ALIS – AI Legal Intelligence System

ALIS is an AI‑powered legal document analysis platform that helps legal and deal‑making professionals draft, review, and analyse agreements. It extracts clauses, checks compliance against South African legislation, flags risks, and provides intelligent drafting suggestions—all running locally with Ollama.

## ✨ Features

- 🔐 **User authentication** – Register and login with bcrypt password hashing.
- 📄 **Document upload** – Upload `.txt` legal agreements for analysis.
- 🧠 **AI‑powered analysis** – Uses Ollama (local LLM) to extract clauses and classify deal type.
- ⚠️ **Compliance checking** – Flags risks based on South African laws (CPA, POPIA, Companies Act, Labour Relations Act, National Credit Act).
- 💡 **Intelligent suggestions** – Provides corrective guidance for non‑compliant clauses.
- 🛡️ **Admin panel** – Manage users, documents, law rules, and view audit logs.
- 🗄️ **PostgreSQL database** – Secure storage with duplicate document detection (SHA‑256).

## 🛠️ Tech Stack

| Layer       | Technology                       |
|-------------|----------------------------------|
| Frontend    | JSP, HTML, CSS, JavaScript       |
| Backend     | Java Servlets, Jakarta EE 9+     |
| AI Engine   | Ollama (local LLM)               |
| Database    | PostgreSQL                       |
| Build Tool  | Maven                            |
| Server      | Apache Tomcat 10+                |

## 📁 Project Structure

alis-web_v2/
├── src/main/java/za/co/alis/
│   ├── controller/          # Servlets (user & admin)
│   ├── dao/                 # Database access objects
│   ├── model/               # POJOs (User, Document, Clause, etc.)
│   └── util/                # Password hashing, Ollama client
├── src/main/webapp/
│   ├── WEB-INF/
│   │   ├── views/           # JSP views (dashboard, analysis, admin)
│   │   └── web.xml          # Deployment descriptor
│   ├── css/                 # Stylesheets
│   ├── js/                  # JavaScript (chat interface)
│   └── *.jsp                # Login, register, upload pages
├── pom.xml                  # Maven dependencies
└── README.md

## 🚀 Getting Started

### Prerequisites

- **Java JDK 17** or higher
- **Apache Maven** 3.6+
- **Apache Tomcat 10+** (Jakarta EE 9+)
- **PostgreSQL** 14+
- **Ollama** installed and running locally ([ollama.com](https://ollama.com))

### 1. Clone the Repository

git clone https://github.com/your-username/alis-legal-intelligence.git
cd alis-legal-intelligence

### 2. Set Up the Database

Create a PostgreSQL database (e.g., ALIS_WEBAPPDB).  
Run the SQL script provided in ALIS_DB_SCRIPT_V7.sql to create all tables, sequences, and seed data.

psql -U postgres -d ALIS_WEBAPPDB -f ALIS_DB_SCRIPT_V7.sql

### 3. Configure Database Connection

Copy DBConnection.template.java to DBConnection.java and fill in your PostgreSQL credentials.

### 4. Pull the Ollama Model

ollama pull deepseek-r1:1.5b   # or llama3

### 5. Build the WAR

mvn clean package

### 6. Deploy to Tomcat

Copy target/alis.war to Tomcat's webapps folder and start Tomcat.

### 7. Access the Application

- User interface: http://localhost:8080/alis
- Admin panel: http://localhost:8080/alis/admin/login.jsp

## 📸 Screenshots

| Login | Dashboard | Analysis Results |
|-------|-----------|------------------|
| ![Login](screenshots/login.png) | ![Dashboard](screenshots/dashboard.png) | ![Analysis](screenshots/analysis.png) |

## 🧪 Testing

Upload a .txt file containing a legal clause (e.g., an NDA excerpt). The AI will extract clauses, assign risk levels, and display compliance suggestions.

## 🤝 Contributing

This project was developed as part of a university group assignment. Contributions, issues, and feature requests are welcome.

## 📄 License

This project is licensed under the MIT License.

## 🙏 Acknowledgements

- [Ollama](https://ollama.com) for local LLM inference.
- [Apache Tomcat](https://tomcat.apache.org) and [Jakarta EE](https://jakarta.ee).
- Group members of **Group D** for collaborative development.
