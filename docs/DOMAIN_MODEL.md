# CBT-Pulse Grid Planned Domain Model

The following records describe the planned core domain. They are architectural placeholders only; no persistence entities or business behavior are defined at this stage.

- **Institution** — an organization that owns and administers CBT resources and examinations.
- **User** — a person who accesses the platform as an administrator, author, invigilator, or candidate.
- **Role** — a named collection of permissions assigned to users.
- **Course** — an institutional subject or unit used to organize questions and examinations.
- **Question** — an assessable prompt held in the question bank.
- **QuestionOption** — a selectable response option belonging to a question.
- **Exam** — a configured assessment with timing, availability, and delivery rules.
- **ExamAssignment** — the allocation of an exam to a candidate or candidate group.
- **Attempt** — a candidate's active or completed sitting of an assigned exam.
- **AttemptQuestion** — the question instance and ordering presented within an attempt.
- **Answer** — a candidate response recorded for an attempt question.
- **ProctorEvent** — a monitoring or anti-cheat event observed during an attempt.
- **Result** — the scoring and grading outcome produced for an attempt.
- **AuditLog** — an immutable, institution-scoped record of a security-sensitive or operational action, including actor context, outcome, timestamp, request correlation, and sanitized metadata.
