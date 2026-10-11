
import { useEffect, useState } from "react";
import axios from "axios";
import "./App.css";

const API = "http://localhost:8080/api";

function App() {
  const [students, setStudents] = useState([]);
  const [projects, setProjects] = useState([]);
  const [message, setMessage] = useState("");
  const [activeTab, setActiveTab] = useState("students");
  const [selectedProject, setSelectedProject] = useState("");
  const [projectTeams, setProjectTeams] = useState({});
  const [loadingTeam, setLoadingTeam] = useState(false);
  const [generatingId, setGeneratingId] = useState(null);

  const [student, setStudent] = useState({
    name: "",
    email: "",
    skills: "",
  });

  const [project, setProject] = useState({
    title: "",
    requiredSkills: "",
    teamSize: 4,
  });

  async function loadData() {
    try {
      const [studentResponse, projectResponse] = await Promise.all([
        axios.get(`${API}/students`),
        axios.get(`${API}/projects`),
      ]);

      setStudents(studentResponse.data);
      setProjects(projectResponse.data);

      if (projectResponse.data.length > 0) {
        setSelectedProject((current) =>
          current || String(projectResponse.data[0].id)
        );
      }
    } catch {
      setMessage("Cannot connect to backend. Check that Java is running.");
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function addStudent(e) {
    e.preventDefault();

    try {
      await axios.post(
        `${API}/students`,
        new URLSearchParams(student)
      );

      setStudent({ name: "", email: "", skills: "" });
      setMessage("Student registered successfully!");
      await loadData();
    } catch (err) {
      setMessage(
        err.response?.data?.error || "Student registration failed."
      );
    }
  }

  async function addProject(e) {
    e.preventDefault();

    try {
      const response = await axios.post(
        `${API}/projects`,
        new URLSearchParams({
          ...project,
          teamSize: String(project.teamSize),
        })
      );

      setProject({ title: "", requiredSkills: "", teamSize: 4 });
      setSelectedProject(String(response.data.id));
      setMessage("Project created successfully!");
      await loadData();
    } catch (err) {
      setMessage(
        err.response?.data?.error || "Project creation failed."
      );
    }
  }

  async function fetchProjectTeam(projectId) {
    if (!projectId) return;

    setLoadingTeam(true);

    try {
      const response = await axios.get(
        `${API}/projects/${projectId}/team`
      );

      setProjectTeams((previous) => ({
        ...previous,
        [projectId]: response.data,
      }));
    } catch (err) {
      setMessage(
        err.response?.data?.error || "Could not load this project's team."
      );
      setProjectTeams((previous) => {
        const updated = { ...previous };
        delete updated[projectId];
        return updated;
      });
    } finally {
      setLoadingTeam(false);
    }
  }

  useEffect(() => {
    if (activeTab === "teams" && selectedProject) {
      fetchProjectTeam(selectedProject);
    }
  }, [activeTab, selectedProject]);

  async function generateTeam(projectId) {
    setGeneratingId(projectId);
    setMessage("");

    try {
      const response = await axios.post(
        `${API}/projects/${projectId}/generate`
      );

      setProjectTeams((previous) => ({
        ...previous,
        [projectId]: response.data,
      }));

      setSelectedProject(String(projectId));
      setMessage(response.data.message || "Team generated successfully!");
      setActiveTab("teams");
    } catch (err) {
      setMessage(
        err.response?.data?.error || "Team generation failed."
      );
    } finally {
      setGeneratingId(null);
    }
  }

  function changeTab(tab) {
    setActiveTab(tab);
    setMessage("");
  }

  const currentTeam = projectTeams[selectedProject];
  const currentProject = projects.find(
    (p) => String(p.id) === String(selectedProject)
  );

  return (
    <main className="container">
      <header className="hero">
        <div className="brand">TF</div>
        <div>
          <h1>TeamForge</h1>
          <p>Build better teams. Build better projects.</p>
        </div>
      </header>

      <nav className="navbar" aria-label="Main navigation">
        <button
          type="button"
          className={`nav-button ${activeTab === "students" ? "active" : ""}`}
          onClick={() => changeTab("students")}
        >
          Students
          <span className="nav-count">{students.length}</span>
        </button>

        <button
          type="button"
          className={`nav-button ${activeTab === "projects" ? "active" : ""}`}
          onClick={() => changeTab("projects")}
        >
          Projects
          <span className="nav-count">{projects.length}</span>
        </button>

        <button
          type="button"
          className={`nav-button ${activeTab === "teams" ? "active" : ""}`}
          onClick={() => changeTab("teams")}
        >
          Project Teams
        </button>
      </nav>

      {message && (
        <div className="message" role="status">
          {message}
          <button
            type="button"
            className="dismiss"
            onClick={() => setMessage("")}
            aria-label="Dismiss message"
          >
            ×
          </button>
        </div>
      )}

      <section className="stats">
        <div className="stat">
          <span>Registered students</span>
          <strong>{students.length}</strong>
        </div>

        <div className="stat">
          <span>Available projects</span>
          <strong>{projects.length}</strong>
        </div>

        <div className="stat">
          <span>Projects with saved teams</span>
          <strong>{Object.values(projectTeams).filter(
            (team) => team.members?.length > 0
          ).length}</strong>
        </div>
      </section>

      {activeTab === "students" && (
        <>
          <section className="forms">
            <form className="panel" onSubmit={addStudent}>
              <h2>Register student</h2>

              <label>Full name</label>
              <input
                placeholder="e.g. Arun Kumar"
                required
                value={student.name}
                onChange={(e) =>
                  setStudent({ ...student, name: e.target.value })
                }
              />

              <label>Email</label>
              <input
                type="email"
                placeholder="arun@example.com"
                required
                value={student.email}
                onChange={(e) =>
                  setStudent({ ...student, email: e.target.value })
                }
              />

              <label>Skills</label>
              <input
                placeholder="Java, React, MySQL"
                required
                value={student.skills}
                onChange={(e) =>
                  setStudent({ ...student, skills: e.target.value })
                }
              />

              <button type="submit">Register student</button>
            </form>

            <div className="panel">
              <h2>Student directory</h2>
              <p>All registered students and their technical skills.</p>
              <div className="directory-summary">
                <strong>{students.length}</strong>
                <span>students registered</span>
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="section-heading">
              <div>
                <h2>All Students</h2>
                <p>Student details retrieved from MySQL.</p>
              </div>
              <button type="button" className="secondary-button"
                onClick={loadData}>Refresh</button>
            </div>

            {students.length === 0 ? (
              <p className="empty">No students registered yet.</p>
            ) : (
              <div className="student-grid">
                {students.map((s) => (
                  <article className="student-card" key={s.id}>
                    <div className="avatar">
                      {s.name?.trim().charAt(0).toUpperCase() || "S"}
                    </div>
                    <div className="student-details">
                      <strong>{s.name}</strong>
                      <p>{s.email}</p>
                      <span className="skill-list">{s.skills}</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}

      {activeTab === "projects" && (
        <>
          <section className="forms">
            <form className="panel" onSubmit={addProject}>
              <h2>Create project</h2>

              <label>Project title</label>
              <input
                placeholder="e.g. CampusHub"
                required
                value={project.title}
                onChange={(e) =>
                  setProject({ ...project, title: e.target.value })
                }
              />

              <label>Required skills</label>
              <input
                placeholder="Java, React, MySQL"
                required
                value={project.requiredSkills}
                onChange={(e) =>
                  setProject({
                    ...project,
                    requiredSkills: e.target.value,
                  })
                }
              />

              <label>Team size</label>
              <input
                type="number"
                min="2"
                max="20"
                required
                value={project.teamSize}
                onChange={(e) =>
                  setProject({ ...project, teamSize: e.target.value })
                }
              />

              <button type="submit">Create project</button>
            </form>

            <div className="panel">
              <h2>Project overview</h2>
              <p>Explore projects and generate skill-matched teams.</p>
              <div className="directory-summary">
                <strong>{projects.length}</strong>
                <span>projects available</span>
              </div>
            </div>
          </section>

          <section className="panel">
            <div className="section-heading">
              <div>
                <h2>All Projects</h2>
                <p>Project details retrieved from MySQL.</p>
              </div>
              <button type="button" className="secondary-button"
                onClick={loadData}>Refresh</button>
            </div>

            {projects.length === 0 ? (
              <p className="empty">No projects created yet.</p>
            ) : (
              <div className="project-grid">
                {projects.map((p) => (
                  <article className="project-card" key={p.id}>
                    <div className="project-icon">P</div>
                    <h3>{p.title}</h3>
                    <p>Team size: {p.teamSize}</p>
                    <span className="skill-list">{p.requiredSkills}</span>

                    <button
                      type="button"
                      className="generate-button"
                      disabled={generatingId !== null}
                      onClick={() => generateTeam(p.id)}
                    >
                      {generatingId === p.id
                        ? "Generating..."
                        : "Generate Team"}
                    </button>

                    <button
                      type="button"
                      className="secondary-button"
                      onClick={() => {
                        setSelectedProject(String(p.id));
                        setActiveTab("teams");
                      }}
                    >
                      View Project Team
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        </>
      )}

      {activeTab === "teams" && (
        <section className="panel teams-panel">
          <div className="section-heading">
            <div>
              <h2>Project Teams</h2>
              <p>View saved team assignments for each project.</p>
            </div>
            <button
              type="button"
              className="secondary-button"
              disabled={!selectedProject || loadingTeam}
              onClick={() => fetchProjectTeam(selectedProject)}
            >
              {loadingTeam ? "Loading..." : "Refresh Team"}
            </button>
          </div>

          {projects.length === 0 ? (
            <p className="empty">Create a project before viewing teams.</p>
          ) : (
            <>
              <label htmlFor="project-select">Select project</label>
              <select
                id="project-select"
                className="project-select"
                value={selectedProject}
                onChange={(e) => setSelectedProject(e.target.value)}
              >
                {projects.map((p) => (
                  <option key={p.id} value={String(p.id)}>
                    {p.title}
                  </option>
                ))}
              </select>

              {currentProject && (
                <div className="selected-project">
                  <div>
                    <span className="eyebrow">SELECTED PROJECT</span>
                    <h3>{currentProject.title}</h3>
                    <p>Required skills: {currentProject.requiredSkills}</p>
                    <p>Target team size: {currentProject.teamSize}</p>
                  </div>

                  <button
                    type="button"
                    className="generate-button"
                    disabled={generatingId !== null}
                    onClick={() => generateTeam(currentProject.id)}
                  >
                    {generatingId === currentProject.id
                      ? "Generating..."
                      : "Generate / Regenerate Team"}
                  </button>
                </div>
              )}

              {loadingTeam ? (
                <p className="empty">Loading saved team members...</p>
              ) : currentTeam ? (
                <>
                  <div className="team-summary">
                    <strong>{currentTeam.members?.length || 0}</strong>
                    <span>
                      / {currentTeam.requestedTeamSize} assigned members
                    </span>
                  </div>

                  {(currentTeam.members?.length || 0) === 0 ? (
                    <p className="empty">
                      No team has been saved for this project yet.
                      Click Generate Team to assign students.
                    </p>
                  ) : (
                    <div className="student-grid">
                      {currentTeam.members.map((member) => (
                        <article className="student-card" key={member.id}>
                          <div className="avatar">
                            {member.name?.trim().charAt(0).toUpperCase() || "S"}
                          </div>

                          <div className="student-details">
                            <strong>{member.name}</strong>
                            <p>{member.email}</p>
                            <span className="skill-list">{member.skills}</span>

                            {member.matchPercentage !== undefined && (
                              <div className="match-row">
                                <span>Skill match</span>
                                <strong>{member.matchPercentage}%</strong>
                              </div>
                            )}

                            {member.matchPercentage !== undefined && (
                              <div className="match-track">
                                <div
                                  className="match-fill"
                                  style={{
                                    width: `${Math.max(0, Math.min(
                                      100,
                                      Number(member.matchPercentage) || 0
                                    ))}%`,
                                  }}
                                />
                              </div>
                            )}
                          </div>
                        </article>
                      ))}
                    </div>
                  )}

                  {(currentTeam.members?.length || 0) <
                    currentTeam.requestedTeamSize && (
                    <p className="team-warning">
                      This team is incomplete. Register more students with
                      matching skills, then regenerate the team.
                    </p>
                  )}
                </>
              ) : (
                <p className="empty">
                  Select a project to load its saved team.
                </p>
              )}
            </>
          )}
        </section>
      )}

      <footer>TeamForge · Simple. Smart. Collaborative.</footer>
    </main>
  );
}

export default App;
