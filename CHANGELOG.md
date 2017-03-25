# Unreleased

# 0.2.0 (2017-03-25)
No changes since last rc.

# 0.2.0-rc.1 (2017-03-19)
## Compatibility Changes
- [SGP-35](https://jira.johnnei.org/browse/SGP-35): Update compliance to validate support against GitLab 8.15, 8.16 and 8.17.
- [SGP-36](https://jira.johnnei.org/browse/SGP-36): Update compliance to validate support against SonarQube 6.3

## New Features
- [SGP-17](https://jira.johnnei.org/browse/SGP-17): Accurately create inline comments when multiple new commits have been made since last analysis.
- [SGP-14](https://jira.johnnei.org/browse/SGP-14): Break the GitLab Pipeline when critical or worse issues are introduced.
- [SGP-6](https://jira.johnnei.org/browse/SGP-6): The .git folder is no longer mandatory to be available to create comments.

## Improvements
- [SGP-19](https://jira.johnnei.org/browse/SGP-19): Add severity icons to inline issue comments.

## Bug Fixes
- [SGP-31](https://jira.johnnei.org/browse/SGP-31): Issues on file instead of line no longer causes exceptions. File issues will be reported on an arbitrary section of diff containing the file they apply to.
- [SGP-32](https://jira.johnnei.org/browse/SGP-32): Diff parsing no longer fails on added files which are only a few lines.

## Miscellaneous
- [SGP-38](https://jira.johnnei.org/browse/SGP-38): Plugin key was changed from `gitlab` to `gitlabintegration`.

# 0.1.0 (2017-01-07)
No changes since last rc.

# 0.1.0-rc.2 (2017-01-01)
## Security Changes
- [SGP-27](https://jira.johnnei.org/browse/SGP-27): Prefer usage of GitLab access tokens.
- [SGP-28](https://jira.johnnei.org/browse/SGP-28): Mark auth token as password field and documentate security risks.

## Bug Fixes
- [SGP-29](https://jira.johnnei.org/browse/SGP-29): Only trigger post issue job when the commit hash is supplied.

# 0.1.0-rc.1 (2016-12-30)
## New Features
- [SGP-1](https://jira.johnnei.org/browse/SGP-1): Create comments in GitLab on commits.
- [SGP-2](https://jira.johnnei.org/browse/SGP-2): Don't duplicate comments on incremental analyses.
- [SGP-4](https://jira.johnnei.org/browse/SGP-4): Create summary comment in GitLab on commit.

## Compatibility Changes
- [SGP-3](https://jira.johnnei.org/browse/SGP-3): Ensure compatibility with SonarQube LTS through 6.2 and GitLab 8.12 through 8.14.
- [SGP-8](https://jira.johnnei.org/browse/SGP-4): Ensure compatibility with GitLab 8.15. Drops validated support for 8.12.
