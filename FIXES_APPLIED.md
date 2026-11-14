# Fixes applied to this project copy

1. Preserved the username exactly as typed in the Swing login form. This is important because MySQL's `-- ` comment marker requires whitespace after the two hyphens; trimming the input removed that required trailing whitespace.
2. Changed the vulnerable-mode query shown in LoginResult to use a redacted display SQL string rather than the raw executed SQL, preventing the entered password from appearing in the visualizer/result model.
3. Removed the local `db.properties` file from this deliverable because it contained a database password. Use `db.properties.example` to create a local `db.properties` on your own machine.

A smoke test against the bundled H2 database passed: vulnerable mode returned the three synthetic lab rows for the tautology payload, secure mode rejected it without a syntax error, a valid BCrypt login succeeded, and the displayed query did not contain the submitted password. The changed source files compile against the project's packaged dependencies. The archive's previous Surefire reports show 21 tests passed (0 failures/errors); the full Maven test/package command could not be rerun in this environment because the Maven wrapper could not download Maven. Re-run `./mvnw clean test package` on your Mac after opening the project.
