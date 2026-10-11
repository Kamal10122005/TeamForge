# TeamForge 🚀

**Smart Student Team Formation and Project Management**

TeamForge is a full-stack web application that helps organize students into project teams based on their technical skills and project requirements. It matches student skills against required project skills and generates teams using a simple skill-matching algorithm.

## ✨ Features

- **Student Management** — Register students with their names, email addresses, and technical skills.
- **Project Management** — Create projects with required skills and a target team size.
- **Automated Team Formation** — Rank eligible students according to skill matches and generate project teams.
- **Skill Match Evaluation** — Calculate and display skill-match percentages.
- **Team Viewing** — View generated teams and their assigned members.
- **Persistent Storage** — Store student, project, and team data in MySQL.
- **Full-Stack Interface** — Interact with the application through a React-based web interface.

## 🛠️ Tech Stack

| Layer | Technologies |
|---|---|
| Frontend | React, Vite, JavaScript, CSS |
| Backend | Core Java, Java HTTP Server |
| Database | MySQL |
| Database Connectivity | JDBC, MySQL Connector/J |
| API Communication | REST-style HTTP endpoints, JSON |
| Development Tools | VS Code, Git, GitHub |

## 🏗️ Project Structure

```text
TeamForge/
├── backend/
│   ├── src/
│   │   └── Main.java
│   └── lib/
│       └── mysql-connector-j-26.7.0.jar
├── frontend/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
└── README.md
```

## ⚙️ Prerequisites

Install the following before running the project:

- Java Development Kit (JDK) 21 or a compatible version
- Node.js and npm
- MySQL Server
- Git

## 🗄️ Database Setup

1. Start your MySQL server.
2. Open MySQL Workbench or another MySQL client.
3. Execute the following SQL script:

```sql
CREATE DATABASE IF NOT EXISTS teamforge;
USE teamforge;

CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    skills VARCHAR(500) NOT NULL
);

CREATE TABLE IF NOT EXISTS projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    required_skills VARCHAR(500) NOT NULL,
    team_size INT NOT NULL
);

CREATE TABLE IF NOT EXISTS teams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    student_id INT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id),
    FOREIGN KEY (student_id) REFERENCES students(id),
    UNIQUE (project_id, student_id)
);
```

## 🔐 Database Configuration

The backend connects to the `teamforge` database on your local MySQL server.

Configure the database password through an environment variable instead of committing credentials to GitHub.

In Windows PowerShell, set the variable for your current terminal session:

```powershell
$env:TEAMFORGE_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
```

Replace `YOUR_MYSQL_PASSWORD` with your own MySQL password.

Make sure the Java backend reads this variable and uses the appropriate database username and JDBC URL. Never commit your actual password to the repository.

## ▶️ Running the Application

### 1. Start the Backend

Open a PowerShell terminal and navigate to the backend directory:

```powershell
cd backend
```

Compile the Java source:

```powershell
javac -cp "lib\mysql-connector-j-26.7.0.jar" -d . src\Main.java
```

Run the backend:

```powershell
java -cp ".;lib\mysql-connector-j-26.7.0.jar" Main
```

Keep this terminal open while using the application.

The backend is configured to run at:

`http://localhost:8080`

### 2. Start the Frontend

Open a second terminal in VS Code and navigate to the frontend directory:

```powershell
cd frontend
```

Install the dependencies:

```powershell
npm install
```

Start the development server:

```powershell
npm run dev
```

Open the local URL printed by Vite, normally:

`http://localhost:5173`

## 🔌 API Endpoints

The backend exposes the following endpoints:

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/health` | Check backend health |
| GET | `/api/students` | Retrieve students |
| POST | `/api/students` | Register a student |
| GET | `/api/projects` | Retrieve projects |
| POST | `/api/projects` | Create a project |
| POST | `/api/projects/{id}/generate` | Generate a project team |
| GET | `/api/projects/{id}/team` | Retrieve a project's generated team |

The frontend communicates with the backend at `http://localhost:8080/api`.

## 🧠 Team Formation Logic

TeamForge uses a skill-matching approach to form project teams:

1. A project specifies its required technical skills and target team size.
2. The backend compares each student's listed skills against the project's requirements.
3. Students are ranked by the number of matching skills.
4. Students with higher match scores are prioritized, with student ID used to break ties.
5. The selected students are saved as the project's team members in MySQL.

The generated team is persisted in the database and can be retrieved through the application.

**Note:** Skill matching depends on the skills entered by students and the project's requirements. A match score indicates skill overlap, not a guarantee of project success.

## 🔒 Security Notes

- Keep database credentials out of source control.
- Use environment variables for local secrets.
- Do not expose database credentials in frontend code.
- Configure appropriate authentication and authorization before deploying the application for public use.

## 🚧 Future Enhancements

- More advanced skill-based team optimization.
- Student availability and workload balancing.
- Team roles and complementary skill coverage.
- Authentication and role-based access.
- Deployment and production database configuration.

## 👨‍💻 Author

Developed as a full-stack project to explore Java backend development, React, MySQL, JDBC, API integration, and automated student team formation.

## 📄 License

No license has been specified yet. Add a license if you intend to permit others to reuse or distribute this project.

TO Start the Frontend:

cd "C:\Users\Salai Kamalavassan.S\Desktop\TeamForge\frontend"
npm run dev

To Start Backend:

cd "C:\Users\Salai Kamalavassan.S\Desktop\TeamForge\backend"

$env:TEAMFORGE_DB_PASSWORD = "root"

javac -cp "lib\mysql-connector-j-26.7.0.jar" -d . src\Main.java
java -cp ".;lib\mysql-connector-j-26.7.0.jar" Main