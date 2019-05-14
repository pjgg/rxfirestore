# Git Commit Guidelines
We have very precise rules over how our git commit messages can be formatted. This leads to more readable messages that are easy to follow when looking through the project history. But also, we use the git commit messages to generate the change log (TO DO).
 
Each commit message consists of a header, and a body.

## Header 

The header is mandatory. It has a special format that includes a **type**, a **scope**, a **subject** and an optional **issue_id**:
 
```
<type>(<scope>): (<issue_id>) <subject>
<BLANK LINE>
<body>
```
For example:
 
- feat(apiRest): TMPV-666 add new endpoint
- doc(apiRest): XXX-665 add swagger doc api
- fix(batch): XXX-667 fix cron expression

### Type

**Allowed `<type>`:**
 
- feature
- hotfix (bug fix with the environment)
- fix (bug fix)
- docs (documentation)
- style (formatting, missing semicolons, …)
- refactor
- test (when adding missing tests)
- chore (maintain)

**Next branch types will generate a k8s_deployment on CI:**
 
-feature
-hotfix
-feat
-userstory
-ft
-us
-hf

##### _Any line of the commit message cannot be longer 50 characters. This allows the message to be easier to read in various git tools._

## Scope

Allowed `<scope>`:
- serviceNameOne
- ServiceNameTwo

## Subject

A short phrase explaining the commit changes, should be capitalized and using imperative present tense.
 
## Body

Just as in the subject, use the imperative, present tense. The body should include the motivation for the change and contrast this with previous behavior. It's an optional deeper description.
 
Pushing a change
Before you submit your changes consider the following:
 
- Search for an open or closed Issue that relates to your stuff. You don't want to duplicate effort.
- Develop your stuff in a new branch
- Build your changes locally to ensure all tests pass and everything works as expected
- If everything works as expected, **and your branch is in sync with master** then open a **Pull Request**
- If we suggest changes then:
- Make the required updates.
- Rebuild to ensure tests are still passing and everything still works.
- Push your branch to the remote repository.
- If needed, sync your branch with master (we recommend using merge instead of rebase)

## Opening a Pull Request
You can open a PR against any branch.
 
Master branch is the standard branch to work with, where feature branches will be merged.
 
Use following  **naming for** PR titles: `<type>(<section>): <ISSUE-ID> <SUBJECT>`

#### Example:
- **feat(checkout-v2): TMPV-1234 my awesome stuff**
 
Inspired by [https://gist.github.com/stephenparish/9941e89d80e2bc58a153](https://gist.github.com/stephenparish/9941e89d80e2bc58a153)
 
**Allowed `<type>`:**
 
- feat (feature)
- hotfix
- fix (bug fix)
- docs (documentation)
- style (formatting, missing semicolons, …)
- refactor
- test (when adding missing tests)
- chore (maintain)

**Examples for `<section>`:**

- cancelation
- technologyChange
- vista
- onboarding
- ticketing
- customer
- tariffs
- terminals
- shared
- auth
- coverage
- consumtion
- appointments
- customer-scoring
- credit-scoring
- orders
- sales
- ...

## Merging a Pull Request

Once a PR has been reviewed by dev team and has e2e and has been tested by qa/product in a feature branch (must be synced with master), the PR can be merged.
 
Tips for merging a PR:

- Ensure that commits compile to these conventions, or at least one (in that case squash all commits except the one that compiles)
- Ensure you **remove the branch** after merging the PR