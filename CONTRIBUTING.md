# Contributing to the Java Service Golden Path

Thank you for your interest in improving our golden path! This template serves all Java service teams, so your contributions have wide impact.

## Types of Contributions

### 1. Bug Fixes
Found something broken? Please fix it!

### 2. Pattern Additions
Have a pattern that's proven in production? Share it!

### 3. Documentation Improvements
Clearer docs help everyone onboard faster.

### 4. Dependency Updates
Keeping dependencies current helps security and compatibility.

## How to Contribute

### Small Changes (Docs, Bug Fixes)

1. Fork the repository
2. Create a branch: `git checkout -b fix/description`
3. Make your changes
4. Test locally (see below)
5. Submit a PR

### Larger Changes (New Features, Patterns)

1. **Open an issue first** - Discuss with the platform team
2. Get alignment on approach
3. Create an ADR for significant decisions
4. Implement with tests
5. Submit a PR

## Testing Your Changes

### Local Testing

```bash
# Clone the repo
git clone https://github.com/your-org/java-service-template
cd java-service-template

# Test template generation
# Use Backstage CLI or manual Jinja2 rendering

# Test generated service
cd skeleton
./mvnw clean verify
```

### What to Test

- [ ] Template generates without errors
- [ ] Generated service builds: `./mvnw clean package`
- [ ] Tests pass: `./mvnw test`
- [ ] Quality checks pass: `./mvnw checkstyle:check spotbugs:check`
- [ ] Docker builds: `docker build -t test .`
- [ ] Health checks work: `curl localhost:8081/health`

## Contribution Guidelines

### Code Style

- Follow existing patterns in the codebase
- Java: Google Java Style (enforced by Checkstyle)
- YAML: 2-space indentation
- Markdown: One sentence per line (easier diffs)

### Commit Messages

Use conventional commits:

```
feat: add distributed tracing support
fix: correct health check endpoint path
docs: update getting started guide
chore: update Spring Boot to 3.4.2
```

### Documentation

Every feature needs:
- Code comments for complex logic
- Usage example in relevant guide
- ADR for significant decisions

### Backward Compatibility

- Don't break existing services
- Deprecate before removing
- Provide migration path for breaking changes

## What Makes a Good Addition?

Ask yourself:

1. **Is it proven?** Has this worked in production?
2. **Is it common?** Will multiple teams use this?
3. **Is it simple?** Can you explain it in a sentence?
4. **Is it optional?** Can teams opt-out if needed?
5. **Is it documented?** Can someone use it without asking you?

## Pull Request Checklist

- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] ADR added (if significant change)
- [ ] No breaking changes (or migration path provided)
- [ ] Changelog updated
- [ ] PR description explains the "why"

## Review Process

1. **Automated checks** - CI must pass
2. **Platform team review** - At least one approval
3. **Documentation review** - For significant changes
4. **Merge** - Squash and merge to main

## Getting Help

- **Slack**: #platform-help
- **Office Hours**: Thursdays 2-3pm
- **Issue discussions**: Comment on the issue

## Recognition

Contributors are recognized in:
- Release notes
- Template changelog
- Team announcements

Thank you for making our golden path better for everyone!
