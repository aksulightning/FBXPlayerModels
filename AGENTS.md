# AGENTS.md

## Scope

Agents may work only inside the codespace named `com.aksulightning.fbxplayermodels`.

Do not touch repositories. Do not create branches, commits, pull requests, issues, labels, comments, or any other not asked changes.

## Goals

When working in the allowed codespace, agents should:

- Make requested features complete.
- Improve the code when the improvement directly supports the requested work.
- Keep changes focused and avoid touching unrelated code.
- Prefer durable, maintainable implementations over temporary fixes.
- The modloader is Fabric.
- Working on versions: 1.21.1, 26.3 (full and client-only Fabric variants).

## Avoid

Agents should avoid:

- Quick fixes that only hide the real problem.
- Leaving TODOs instead of finishing the requested work.
- Starting features that will remain unfinished.
- Producing broken code or changes that do not compile.
- Refactoring or rewriting code that was not part of the request.

## Required Workflow

For every coding task:

1. Use codebase com.aksulightning.fbxplayermodels.
2. Create, make changes, remove the code.
3. Test only with: bash ./gradlew :fabric-1.21.1:compileJava :fabric-1.21.1:compileClientJava :fabric-26.3:compileJava :fabric-26.3:compileClientJava :fabric-1.21.1-client:compileJava :fabric-1.21.1-client:compileClientJava :fabric-26.3-client:compileJava :fabric-26.3-client:compileClientJava
4. Document the changes made to the Code_Guide.md file.

Do not run unrelated checks, test suites, formatters, or build tasks unless explicitly asked.
